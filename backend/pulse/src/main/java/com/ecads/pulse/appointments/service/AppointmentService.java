package com.ecads.pulse.appointments.service;

import com.ecads.pulse.appointments.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.appointments.dto.AppointmentStatusUpdateDto;
import com.ecads.pulse.appointments.dto.PreviousAppointmentsDto;
import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentMode;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.authentication.service.UserAuthenticationService;
import com.ecads.pulse.channeling.dto.ConsultantDTO;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.common.exception.FirebaseFirestoreException;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.hospital.dto.HospitalApiDto;
import com.ecads.pulse.hospital.dto.HospitalDto;
import com.ecads.pulse.hospital.dto.LocationDto;
import com.ecads.pulse.payment.entity.AppointmentPayment;
import com.ecads.pulse.payment.enums.PaymentStatus;
import com.ecads.pulse.payment.repository.AppointmentPaymentsRepository;
import com.ecads.pulse.telemedicine.util.TeleMedicineHelper;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserAuthenticationService authenticationService;
    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final AppointmentPaymentsRepository paymentsRepository;
    private final Firestore firestore;
    private final TeleMedicineHelper teleMedicineHelper;

    @Transactional
    public ResponseEntity<Map<String, String>> createPendingAppointment(
            Map<String, String> requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.get("hospitalPlaceId");
        String consultantUid = requestData.get("consultantUid");
        LocalDate appointmentDate = LocalDate.parse(requestData.get("appointmentDate"));
        LocalTime appointmentSlot = LocalTime.parse(
                requestData.get("appointmentSlot"),
                DateTimeFormatter.ofPattern("hh:mm a"));
        String patientAuthToken = requestData.get("patientAuthToken");

        AppointmentMode appointmentMode = AppointmentMode.ONSITE;
        if (requestData.get("appointmentMode").equalsIgnoreCase(AppointmentMode.ONLINE.name())) {
            appointmentMode = AppointmentMode.ONLINE;
        }

        ConsultantChannelingHospital channelingHospital = channellingHospitalRepository
                .getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        if (channelingHospital == null || !channelingHospital.getIsActive()) {
            throw new ShowErrorException("The consultant is currently unavailable");
        }

        List<Appointment> currentAppointments = appointmentRepository
                .getAppointmentByConsultantChannelingHospitalAndAppointmentDate(
                        channelingHospital, appointmentDate);

        Map<String, String> resultsMap = new HashMap<>();

        if (currentAppointments != null && !currentAppointments.isEmpty()) {
            for (Appointment appointment : currentAppointments) {
                if (appointment.getAppointmentStartsAt().equals(appointmentSlot)) {
                    if (!appointment.getAppointmentStatus().equals(AppointmentStatus.CANCELLED)) {
                        return ResponseEntity.ok(resultsMap);
                    }
                }
            }
        }

        FirebaseToken authUser = authenticationService.authUser(patientAuthToken);
        String patientUid = authUser.getUid();

        Appointment appointment = new Appointment();
        appointment.setPatientUid(patientUid);
        appointment.setConsultantChannelingHospital(channelingHospital);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setAppointmentStartsAt(appointmentSlot);
        appointment.setAppointmentEndsAt(appointmentSlot.plusMinutes(
                channelingHospital.getDurationPerPatient().getMinutes()));
        appointment.setAppointmentStatus(AppointmentStatus.PENDING);
        appointment.setAppointmentMode(appointmentMode);
        appointment.setPaymentStatus(PaymentStatus.PENDING);

        if (appointmentMode.equals(AppointmentMode.ONLINE)) {
            //temp
            appointment.setMeetingLink("channelName");
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

        resultsMap.put("appointmentId", String.valueOf(savedAppointment.getId()));
        resultsMap.put("description", " ");
//        resultsMap.put("description", "Appointment with"+ +" Dr Isuka Premalal on 02 Apr 2026 at 03:45 PM");

        return ResponseEntity.ok(resultsMap);
    }

    @Transactional
    public ResponseEntity<Void> cancelAppointment(
            AppointmentStatusNotifyDto cancelDto) {
        if (cancelDto == null) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.
                getAppointmentById(Long.valueOf(cancelDto.getAppointmentId()));

        if (appointment != null) {

            appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
            appointment.setPaymentStatus(cancelDto.getStatus());

            return ResponseEntity.ok().build();

        } else {
            throw new ShowErrorException("No appointment found");
        }
    }

    @Transactional
    public ResponseEntity<Void> confirmAppointment(
            AppointmentStatusNotifyDto notifyDto) {
        if (notifyDto == null) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.
                getAppointmentById(Long.valueOf(notifyDto.getAppointmentId()));

        if (appointment != null) {
            AppointmentPayment payment = paymentsRepository
                    .getAppointmentPaymentByAppointmentId(appointment.getId());

            if (payment == null) {
                throw new ShowErrorException("Payment record not found");
            }

            if (appointment.getAppointmentStatus() == AppointmentStatus.CONFIRMED
                    && appointment.getPaymentStatus() == PaymentStatus.PAID) {

                if (payment.getPaymentStatus() == PaymentStatus.PAID) {

                    if (appointment.getAppointmentMode() == AppointmentMode.ONLINE) {
                        appointment.setMeetingLink(teleMedicineHelper.generateAgoraChannelName(String.valueOf(appointment.getId())));
                    }

                    return ResponseEntity.ok().build();
                } else {
                    log.debug("Appointment payment confirmation failed. Payment Status: {} [Expected: {}]",
                            payment.getPaymentStatus(), PaymentStatus.PAID);
                    throw new ShowErrorException("Appointment payment confirmation failed");
                }
            } else {
                if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                    if (appointment.getAppointmentStatus() != AppointmentStatus.CONFIRMED) {
                        log.debug("Appointment confirming logic error. Appointment Status: {} [Expected: {}]",
                                appointment.getAppointmentStatus(), AppointmentStatus.CONFIRMED);
                    } else if (appointment.getPaymentStatus() != PaymentStatus.PAID) {
                        log.debug("Appointment confirming logic error. Payment Status: {} [Expected: {}]",
                                appointment.getPaymentStatus(), PaymentStatus.PAID);
                    }
                }
                appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                appointment.setPaymentStatus(PaymentStatus.CANCELLED);
                throw new ShowErrorException("Appointment confirmation failed");
            }

        } else {
            throw new ShowErrorException("No appointment found");
        }
    }

    @Transactional
    public ResponseEntity<Void> cancelConfirmedAppointment(
            SingleRequestDataDto cancelDto) {
        if (cancelDto == null) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.
                getAppointmentById(Long.valueOf(cancelDto.getData()));

        if (appointment != null) {

            AppointmentPayment payment = paymentsRepository
                    .getAppointmentPaymentByAppointmentId(appointment.getId());

            if (payment != null) {

                appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                appointment.setPaymentStatus(PaymentStatus.REFUNDED);

                // TODO: Payhere refund

                payment.setPaymentStatus(PaymentStatus.REFUNDED);

                return ResponseEntity.ok().build();

            } else {
                throw new ShowErrorException("No payments found for the appointment");
            }

        } else {
            throw new ShowErrorException("No appointment found");
        }
    }

    public ResponseEntity<List<PreviousAppointmentsDto>> fetchAppointmentList(
            String patientUid) {

        if (patientUid == null || patientUid.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        List<Appointment> appointmentList = appointmentRepository.getAllByPatientUidAndPaymentStatusIsNot(patientUid.trim(), PaymentStatus.CANCELLED, Sort.by(Sort.Direction.DESC, "appointmentDate"));
        Map<String, ConsultantDTO> consultantMap = new HashMap<>();
        Map<String, HospitalApiDto> hospitalMap = new HashMap<>();

        Set<String> consultantUids = new HashSet<>();
        Set<String> hospitalPlaceIds = new HashSet<>();

        List<PreviousAppointmentsDto> resultsList = new ArrayList<>();

        if (appointmentList != null && !appointmentList.isEmpty()) {
            for (Appointment appointment : appointmentList) {
                consultantUids.add(appointment.getConsultantChannelingHospital().getConsultantUid());
                hospitalPlaceIds.add(appointment.getConsultantChannelingHospital().getHospitalPlaceId());
            }

            try {
                QuerySnapshot querySnapshot = firestore.collection("consultants")
                        .whereIn("uid", consultantUids.stream().toList()).get().get();

                if (!querySnapshot.isEmpty()) {
                    List<ConsultantDTO> consultantDTOS = querySnapshot.toObjects(ConsultantDTO.class);
                    for (ConsultantDTO dto : consultantDTOS) {
                        consultantMap.put(dto.getUid(), dto);
                    }
                } else {
                    log.warn("Received consultant querySnapshot is empty");
                    throw new ShowErrorException("Firestore error: Received consultant querySnapshot is empty");
                }

                QuerySnapshot hospitalQuerySnapshot = firestore.collection("hospitals")
                        .whereIn("placeId", hospitalPlaceIds.stream().toList()).get().get();

                if (!hospitalQuerySnapshot.isEmpty()) {
                    List<HospitalDto> dtoList = hospitalQuerySnapshot.toObjects(HospitalDto.class);

                    for (HospitalDto hospitalDto : dtoList) {
                        HospitalApiDto apiDto = new HospitalApiDto(
                                hospitalDto.getPlaceId(),
                                hospitalDto.getImageUrl(),
                                hospitalDto.getName(),
                                hospitalDto.getLowerName(),
                                hospitalDto.getAddress(),
                                hospitalDto.getContact(),
                                hospitalDto.getEmail(),
                                new LocationDto(hospitalDto.getLocation().getLatitude(), hospitalDto.getLocation().getLongitude()),
                                hospitalDto.getGeohash(),
                                hospitalDto.getMapUrl()
                        );
                        hospitalMap.put(hospitalDto.getPlaceId(), apiDto);
                    }
                } else {
                    log.warn("Received hospital querySnapshot is empty");
                    throw new ShowErrorException("Firestore error: Received hospital querySnapshot is empty");
                }

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

                for (Appointment appointment : appointmentList) {
                    HospitalApiDto hospital = hospitalMap.get(appointment.getConsultantChannelingHospital().getHospitalPlaceId());
                    ConsultantDTO consultant = consultantMap.get(appointment.getConsultantChannelingHospital().getConsultantUid());

                    PreviousAppointmentsDto appointmentsDto = new PreviousAppointmentsDto(
                            appointment.getId().toString(),
                            hospital.getName(),
                            consultant.getTitle() + " " + consultant.getName(),
                            consultant.getSpeciality(),
                            consultant.getBaseHospital(),
                            hospital.getMapUrl(),
                            appointment.getMeetingLink(),
                            appointment.getAppointmentStartsAt().format(timeFormatter),
                            appointment.getAppointmentDate().toString(),
                            appointment.getPaymentStatus(),
                            appointment.getAppointmentEndsAt().format(timeFormatter)
                    );
                    resultsList.add(appointmentsDto);
                }


            } catch (InterruptedException e) {
                log.error("Firestore data retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore data retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        }
        return ResponseEntity.ok(resultsList);
    }

    @Transactional
    public ResponseEntity<Void> updateOnlineAppointmentStatus(
            AppointmentStatusUpdateDto updateDto) {

        if (updateDto == null) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository
                .getAppointmentById(Long.valueOf(updateDto.getAppointmentId()));

        if (appointment == null) {
            throw new ShowErrorException("Appointment not found");
        }

        if (updateDto.getStatusId() == 0) {
            appointment.setAppointmentStatus(AppointmentStatus.COMPLETED);
        } else if (updateDto.getStatusId() == 1) {
            appointment.setAppointmentStatus(AppointmentStatus.DISCONNECTED);
        }

        return ResponseEntity.ok().build();
    }
}
