package com.ecads.pulse.hospital.service;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentMode;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.channeling.dto.ConsultantDTO;
import com.ecads.pulse.channeling.entity.AvailableChannelingDays;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.channeling.enums.SlotDurations;
import com.ecads.pulse.channeling.repository.AvailableChannelingDaysRepository;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.common.dto.UserDto;
import com.ecads.pulse.common.exception.FirebaseFirestoreException;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.common.util.MessagingService;
import com.ecads.pulse.hospital.dto.*;
import com.ecads.pulse.payment.entity.AppointmentPayment;
import com.ecads.pulse.payment.enums.PaymentStatus;
import com.ecads.pulse.payment.repository.AppointmentPaymentsRepository;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class HospitalService {

    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final AvailableChannelingDaysRepository daysRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentPaymentsRepository paymentsRepository;
    private final MessagingService messagingService;
    private final Firestore firestore;

    public ResponseEntity<List<HospitalConsultantsWebDto>> fetchChannelingConsultants(
            SingleRequestDataDto requestData) {
        if (requestData.getData() == null || requestData.getData().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.getData();
        List<HospitalConsultantsWebDto> channellingConsultants = new ArrayList<>();

        List<HospitalConsultantsWebDto> consultantResultList = filterConsultantsByHospitalPlaceId(hospitalPlaceId);
        if (consultantResultList != null && !consultantResultList.isEmpty()) {
            channellingConsultants = consultantResultList;
        }

        return ResponseEntity.ok(channellingConsultants);
    }

    public ResponseEntity<List<HospitalAppointmentsWebResponseDto>> fetchAppointments(
            HospitalAppointmentsWebRequestDto requestData) {
        if (requestData == null || requestData.getHospitalId() == null || requestData.getStartAt() < 0) {
            return ResponseEntity.badRequest().build();
        }

        int pageSize = 15;
        String hospitalPlaceId = requestData.getHospitalId();

        List<Appointment> appointmentList = appointmentRepository
                .getAllByConsultantChannelingHospital_HospitalPlaceId(hospitalPlaceId,
                        PageRequest.of(requestData.getStartAt(), pageSize));

        List<HospitalAppointmentsWebResponseDto> responseDtos = new ArrayList<>();

        if (appointmentList != null && !appointmentList.isEmpty()) {

            Set<String> patientUids = appointmentList.stream()
                    .flatMap(a -> Stream.of(
                            a.getPatientUid(),
                            a.getConsultantChannelingHospital().getConsultantUid()))
                    .collect(Collectors.toSet());

            Set<String> consultantUids = appointmentList.stream()
                    .flatMap(a -> Stream.of(
                            a.getConsultantChannelingHospital().getConsultantUid()))
                    .collect(Collectors.toSet());

            List<UserDto> patients = fetchUsersByUids(patientUids);

            Map<String, UserDto> patientMap = patients.stream()
                    .collect(Collectors.toMap(UserDto::getUid, Function.identity()));

            List<ConsultantDTO> consultants = getConsultantsByUids(consultantUids.stream().toList());

            Map<String, ConsultantDTO> consultantMap = consultants.stream()
                    .collect(Collectors.toMap(ConsultantDTO::getUid, Function.identity()));

            for (Appointment appointment : appointmentList) {

                UserDto patient = patientMap.get(appointment.getPatientUid());
                ConsultantDTO consultant = consultantMap.get(appointment.getConsultantChannelingHospital().getConsultantUid());

                if (patient != null && consultant != null) {

                    HospitalAppointmentsWebResponseDto dto = new HospitalAppointmentsWebResponseDto();
                    dto.setAppointmentId(String.valueOf(appointment.getId()));
                    dto.setPatientUid(appointment.getPatientUid());
                    dto.setPatientName(patient.getFname() + " " + patient.getLname());
                    dto.setConsultantUid(appointment.getConsultantChannelingHospital().getConsultantUid());
                    dto.setConsultantName(consultant.getTitle() + " " + consultant.getName());
                    dto.setDate(appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    dto.setTime(appointment.getAppointmentStartsAt().format(DateTimeFormatter.ofPattern("hh:mm a")));
                    dto.setFee(String.valueOf(appointment.getConsultantChannelingHospital().getConsultationFee()));
                    dto.setAppointmentStatus(appointment.getAppointmentStatus().name());
                    dto.setPaymentStatus(appointment.getPaymentStatus().name());

                    responseDtos.add(dto);
                }
            }
        }

        return ResponseEntity.ok(responseDtos);
    }

    @Transactional
    public ResponseEntity<Void> appointmentCancelAndRefund(SingleRequestDataDto requestData) {
        if (requestData == null || requestData.getData() == null) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.getAppointmentById(
                Long.valueOf(requestData.getData()));

        if (appointment != null) {
            if (appointment.getPaymentStatus() == PaymentStatus.PAID) {

                AppointmentPayment payment = paymentsRepository.getAppointmentPaymentByAppointmentId(
                        appointment.getId());

                if (payment != null) {

                    appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                    //PAYHERE REFUND
                    appointment.setPaymentStatus(PaymentStatus.REFUNDED);
                    payment.setPaymentStatus(PaymentStatus.REFUNDED);

                    List<String> conList = new ArrayList<>();
                    conList.add(appointment.getConsultantChannelingHospital().getConsultantUid());
                    List<ConsultantDTO> consultants = getConsultantsByUids(conList);

                    if (consultants != null && !consultants.isEmpty()) {
                        String consultantName = consultants.get(0).getTitle() + " " + consultants.get(0).getName();
                        String message = "Appointment for "
                                + consultantName +
                                " on "
                                + appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                + " was cancelled. Your payment has been refunded. See details in Pulse.";

                        Boolean pushNotification = messagingService.sendPushNotification(
                                appointment.getPatientUid(),
                                "Appointment Cancelled!",
                                message
                        );

                        if (!pushNotification) {
                            log.error("Push notification sending failed");
                        }
                    } else {
                        log.error("Consultant details fetching failed. Push notification sending failed.");
                    }

                    return ResponseEntity.ok().build();
                } else {
                    throw new ShowErrorException("Payment not found");
                }
            } else {
                throw new ShowErrorException("Invalid payment status");
            }
        } else {
            throw new ShowErrorException("No appointment found");
        }
    }

    @Transactional
    public ResponseEntity<Void> appointmentReschedule(AppointmentRescheduleRequestDto requestData) {
        if (requestData == null || requestData.getAppointmentId() == null || requestData.getAppointmentId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        LocalDate appointmentDate = LocalDate.parse(requestData.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        LocalTime slot = LocalTime.parse(requestData.getSlot(), DateTimeFormatter.ofPattern("hh:mm a"));
        AppointmentMode appointmentMode = AppointmentMode.ONSITE;

        if (requestData.getMode().equalsIgnoreCase(AppointmentMode.ONLINE.name())) {
            appointmentMode = AppointmentMode.ONLINE;
        }

        if (LocalDate.now().isEqual(appointmentDate)) {
            if (LocalTime.now().isAfter(slot)) {
                return ResponseEntity.badRequest().build();
            }
        } else if (LocalDate.now().isAfter(appointmentDate)) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.getAppointmentById(
                Long.valueOf(requestData.getAppointmentId()));

        if (appointment != null) {
            SlotDurations duration = appointment.getConsultantChannelingHospital().getDurationPerPatient();

            if (appointment.getAppointmentStatus() == AppointmentStatus.CONFIRMED
                    || (appointment.getAppointmentStatus() == AppointmentStatus.CANCELLED
                    && appointment.getPaymentStatus() == PaymentStatus.PAID)) {

                appointment.setAppointmentDate(appointmentDate);
                appointment.setAppointmentStartsAt(slot);
                appointment.setAppointmentMode(appointmentMode);
                appointment.setAppointmentEndsAt(slot.plusMinutes(duration.getMinutes()));
                appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);

                List<String> conList = new ArrayList<>();
                conList.add(appointment.getConsultantChannelingHospital().getConsultantUid());
                List<ConsultantDTO> consultants = getConsultantsByUids(conList);

                if (consultants != null && !consultants.isEmpty()) {
                    String consultantName = consultants.get(0).getTitle() + " " + consultants.get(0).getName();
                    String message = "Your appointment with "
                            + consultantName +
                            " has been rescheduled to "
                            + appointmentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            + " at " + requestData.getSlot() + ".";

                    Boolean pushNotification = messagingService.sendPushNotification(
                            appointment.getPatientUid(),
                            "Appointment Rescheduled!",
                            message
                    );

                    if (!pushNotification) {
                        log.error("Push notification sending failed for appointment rescheduling");
                    }
                } else {
                    log.error("Consultant details fetching failed. Push notification sending failed for appointment rescheduling");
                }

                return ResponseEntity.ok().build();

            } else {
                return ResponseEntity.badRequest().build();
            }

        } else {
            throw new ShowErrorException("No appointment found");
        }
    }

    public ResponseEntity<List<String>> fetchAvailableDates(ConsultantAvailableDatesWebRequestDto requestData) {
        if (requestData == null || requestData.getConsultantUid() == null || requestData.getConsultantUid().isEmpty()
                || requestData.getHospitalId() == null || requestData.getHospitalId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.getHospitalId();
        String consultantUid = requestData.getConsultantUid();

        ConsultantChannelingHospital channellingRecord = channellingHospitalRepository.getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        if (channellingRecord != null && channellingRecord.getIsActive()) {

            List<AvailableChannelingDays> channelingDays =
                    daysRepository.getAllByChannellingHospital_IdAndIsActive(channellingRecord.getId(), true);

            if (channelingDays != null && !channelingDays.isEmpty()) {

                List<String> availableDays = new ArrayList<>();

                for (AvailableChannelingDays day : channelingDays) {
                    if (day.getIsActive()) {
                        availableDays.add(day.getAvailableDay().name());
                    }
                }

                return ResponseEntity.ok(availableDays);

            } else {
                throw new ShowErrorException("This consultant currently not available for channeling");
            }
        } else {
            throw new ShowErrorException("This consultant currently not available for channeling");
        }
    }

    public ResponseEntity<List<String>> fetchAvailableSlots(AvailableSlotsForDateWebRequestDto requestData) {
        if (requestData == null || requestData.getConsultantUid() == null || requestData.getConsultantUid().isEmpty()
                || requestData.getDate() == null || requestData.getDate().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String consultantUid = requestData.getConsultantUid();
        String hospitalPlaceId = requestData.getHospitalId();

        ConsultantChannelingHospital channellingRecord = channellingHospitalRepository
                .getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        if (channellingRecord != null && channellingRecord.getIsActive()) {

            List<AvailableChannelingDays> channelingDays =
                    daysRepository.getAllByChannellingHospital_IdAndIsActive(channellingRecord.getId(), true);

            LocalDate selectedDate = LocalDate.parse(requestData.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            List<Appointment> existingAppointments = appointmentRepository
                    .getAppointmentByConsultantChannelingHospitalAndAppointmentDate(
                            channellingRecord, selectedDate);

            List<LocalTime> bookedSlots = new ArrayList<>();

            for (Appointment appointment : existingAppointments) {
                if (appointment.getAppointmentStatus() != AppointmentStatus.CANCELLED) {
                    if (!bookedSlots.contains(appointment.getAppointmentStartsAt())) {
                        bookedSlots.add(appointment.getAppointmentStartsAt());
                    }
                }
            }

            if (channelingDays != null && !channelingDays.isEmpty()) {
                DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();

                List<String> slotList = new ArrayList<>();

                for (AvailableChannelingDays session : channelingDays) {
                    if (session.getAvailableDay().name().equalsIgnoreCase(dayOfWeek.toString())) {

                        List<LocalTime> availableSlots = new ArrayList<>();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

                        int duration = session.getChannellingHospital().getDurationPerPatient().getMinutes();
                        LocalTime startsAt = session.getChannellingHospital().getSessionStartsAt();
                        LocalTime endsAt = session.getChannellingHospital().getSessionsEndsAt();

                        LocalTime slot = startsAt;

                        while (!slot.plusMinutes(duration).isAfter(endsAt)) {
                            if (!bookedSlots.contains(slot)) {
                                availableSlots.add(slot);
                            }
                            slot = slot.plusMinutes(duration);
                        }

                        for (LocalTime avSlot : availableSlots) {
                            slotList.add(formatter.format(avSlot));
                        }
                        break;
                    }
                }

                return ResponseEntity.ok(slotList);
            } else {
                throw new ShowErrorException("This consultant currently not available for channelling");
            }
        } else {
            throw new ShowErrorException("This consultant currently not available for channeling");
        }
    }


    private List<HospitalConsultantsWebDto> filterConsultantsByHospitalPlaceId(String placeId) {
        if (placeId != null && !placeId.isEmpty()) {
            List<ConsultantChannelingHospital> hospitalConsultants = channellingHospitalRepository.getAllByHospitalPlaceId(placeId);
            if (hospitalConsultants != null) {
                List<HospitalConsultantsWebDto> resultList = new ArrayList<>();

                List<String> uidList = new ArrayList<>();
                Map<String, HospitalConsultantsWebDto> responseObjects = new HashMap<>();

                for (ConsultantChannelingHospital consultant : hospitalConsultants) {
                    if (consultant.getIsActive()) {
                        uidList.add(consultant.getConsultantUid());

                        List<String> availableDays = getConsultantAvailableDays(consultant);

                        HospitalConsultantsWebDto consultantsWebDto = new HospitalConsultantsWebDto();
                        consultantsWebDto.setFee(String.valueOf(consultant.getConsultationFee()));
                        consultantsWebDto.setAvailableDays(availableDays);

                        responseObjects.put(consultant.getConsultantUid(), consultantsWebDto);
                    }
                }

                List<ConsultantDTO> consultantsByUids = getConsultantsByUids(uidList);
                if (consultantsByUids != null) {
                    for (ConsultantDTO dto : consultantsByUids) {
                        HospitalConsultantsWebDto webDto = responseObjects.get(dto.getUid());
                        webDto.setConsultantDTO(dto);
                        resultList.add(webDto);
                    }
                }

                return resultList;
            } else {
                // no records related to the placeId
                return null;
            }
        } else {
            throw new RuntimeException("No hospitalPlaceId found");
        }
    }

    private List<ConsultantDTO> getConsultantsByUids(List<String> uidList) {
        if (uidList != null && !uidList.isEmpty()) {
            try {
                QuerySnapshot querySnapshot = firestore.collection("consultants").whereIn("uid", uidList)
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    return querySnapshot.toObjects(ConsultantDTO.class);
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("Firestore consultantUid retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore consultantUid retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            return null;
        }
    }

    private List<String> getConsultantAvailableDays(ConsultantChannelingHospital channellingRecord) {
        if (channellingRecord != null && channellingRecord.getIsActive()) {

            List<AvailableChannelingDays> channelingDays =
                    daysRepository.getAllByChannellingHospital_IdAndIsActive(channellingRecord.getId(), true);

            if (channelingDays != null && !channelingDays.isEmpty()) {

                List<String> availableDays = new ArrayList<>();

                for (AvailableChannelingDays day : channelingDays) {
                    if (day.getIsActive()) {
                        availableDays.add(day.getAvailableDay().name());
                    }
                }

                return availableDays;
            } else {
                throw new ShowErrorException("This consultant currently not available for channeling");
            }
        } else {
            throw new ShowErrorException("This consultant currently not available for channeling");
        }
    }

    private List<UserDto> fetchUsersByUids(Set<String> uids) {
        if (uids == null || uids.isEmpty()) {
            return Collections.emptyList();
        }

        try {

            DocumentReference[] docRefs = uids.stream()
                    .map(uid -> firestore.collection("users").document(uid))
                    .toArray(DocumentReference[]::new);

            ApiFuture<List<DocumentSnapshot>> future = firestore.getAll(docRefs);
            List<DocumentSnapshot> snapshots = future.get();

            return snapshots.stream()
                    .filter(DocumentSnapshot::exists)
                    .map(snapshot -> snapshot.toObject(UserDto.class))
                    .collect(Collectors.toList());

        } catch (InterruptedException e) {
            log.error("Firestore user retrieval interrupted: {}", e.getMessage());
            throw new FirebaseFirestoreException("An error occurred while fetching users");
        } catch (ExecutionException e) {
            log.error("Firestore user retrieval failed: {}", e.getMessage());
            throw new FirebaseFirestoreException("An error occurred while fetching users");
        }
    }
}
