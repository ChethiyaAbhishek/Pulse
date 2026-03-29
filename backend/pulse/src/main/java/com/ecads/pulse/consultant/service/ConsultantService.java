package com.ecads.pulse.consultant.service;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.channeling.entity.AvailableChannelingDays;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.channeling.enums.Days;
import com.ecads.pulse.channeling.repository.AvailableChannelingDaysRepository;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.dto.UserDto;
import com.ecads.pulse.common.exception.FirebaseFirestoreException;
import com.ecads.pulse.consultant.dto.AppointmentsRequestWebDto;
import com.ecads.pulse.consultant.dto.AppointmentsWebResponseDto;
import com.ecads.pulse.consultant.dto.AvailableDaysWebResponseDto;
import com.ecads.pulse.hospital.dto.HospitalConsultantsWebDto;
import com.ecads.pulse.payment.enums.PaymentStatus;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConsultantService {

    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final AppointmentRepository appointmentRepository;
    private final AvailableChannelingDaysRepository daysRepository;
    private final Firestore firestore;

    public ResponseEntity<List<AppointmentsWebResponseDto>> fetchUpComingAppointments(
            AppointmentsRequestWebDto requestData) {
        if (requestData == null || requestData.getConsultantUid() == null
                || requestData.getConsultantUid().isEmpty()
                || requestData.getHospitalPlaceId() == null
                || requestData.getHospitalPlaceId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String consultantUid = requestData.getConsultantUid();
        String hospitalPlaceId = requestData.getHospitalPlaceId();
        List<HospitalConsultantsWebDto> channellingConsultants = new ArrayList<>();

        ConsultantChannelingHospital channelingHospital = channellingHospitalRepository
                .getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        List<AppointmentsWebResponseDto> responseList = new ArrayList();
        if (channelingHospital != null) {
            List<Appointment> appointments = appointmentRepository
                    .getAllByConsultantChannelingHospitalAndAppointmentStatus(
                            channelingHospital, AppointmentStatus.CONFIRMED);

            if (appointments != null && !appointments.isEmpty()) {
                LocalDate today = LocalDate.now();
                for (Appointment appointment : appointments) {
                    if (today.equals(appointment.getAppointmentDate())
                            || today.isBefore(appointment.getAppointmentDate())) {
                        responseList.add(createAppointmentWebResponseObject(appointment));
                    }
                }
            }
        }

        return ResponseEntity.ok(responseList);
    }

    public ResponseEntity<List<AppointmentsWebResponseDto>> fetchPreviousAppointments(
            AppointmentsRequestWebDto requestData) {
        if (requestData == null || requestData.getConsultantUid() == null
                || requestData.getConsultantUid().isEmpty()
                || requestData.getHospitalPlaceId() == null
                || requestData.getHospitalPlaceId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String consultantUid = requestData.getConsultantUid();
        String hospitalPlaceId = requestData.getHospitalPlaceId();

        ConsultantChannelingHospital channelingHospital = channellingHospitalRepository
                .getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        List<AppointmentsWebResponseDto> responseList = new ArrayList();
        if (channelingHospital != null) {

            List<Appointment> appointments = appointmentRepository
                    .getAllByConsultantChannelingHospitalAndAppointmentStatusIsNot(
                            channelingHospital, AppointmentStatus.CONFIRMED);

            if (appointments != null && !appointments.isEmpty()) {
                LocalDate today = LocalDate.now();
                for (Appointment appointment : appointments) {

                    if (today.isAfter(appointment.getAppointmentDate())
                            || today.isEqual(appointment.getAppointmentDate())) {
                        if (appointment.getAppointmentStatus() != AppointmentStatus.PENDING) {
                            if (appointment.getPaymentStatus() != PaymentStatus.CANCELLED &&
                                    appointment.getPaymentStatus() != PaymentStatus.PENDING) {

                                responseList.add(createAppointmentWebResponseObject(appointment));
                            }
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(responseList);
    }

    public ResponseEntity<List<AvailableDaysWebResponseDto>> fetchAvailableDays(
            AppointmentsRequestWebDto requestData) {
        if (requestData == null || requestData.getConsultantUid() == null
                || requestData.getConsultantUid().isEmpty()
                || requestData.getHospitalPlaceId() == null
                || requestData.getHospitalPlaceId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String consultantUid = requestData.getConsultantUid();
        String hospitalPlaceId = requestData.getHospitalPlaceId();

        ConsultantChannelingHospital channelingHospital = channellingHospitalRepository
                .getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        List<AvailableDaysWebResponseDto> responseList = new ArrayList<>();
        Map<String, AvailableDaysWebResponseDto> objectMap = new HashMap<>();
        for (Days day : Days.values()) {
            AvailableDaysWebResponseDto dto = new AvailableDaysWebResponseDto();
            dto.setDay(day.name());
            dto.setMorning(false);
            dto.setAfternoon(false);

            objectMap.put(day.name().toLowerCase(), dto);
        }

        if (channelingHospital != null) {
            List<AvailableChannelingDays> channelingDays = daysRepository
                    .getAllByChannellingHospital_IdAndIsActive(
                            channelingHospital.getId(), true);

            if (channelingDays != null && !channelingDays.isEmpty()) {
                for (AvailableChannelingDays availableDay : channelingDays) {

                    AvailableDaysWebResponseDto day = objectMap.get(
                            availableDay.getAvailableDay().name().toLowerCase());

                    if(availableDay.getChannellingHospital().getSessionStartsAt()
                            .isBefore(LocalTime.NOON)){
                        day.setMorning(true);
                    }else{
                        day.setAfternoon(true);
                    }
                }
            }
        }

        for(Days day : Days.values()){
            responseList.add(objectMap.get(day.name().toLowerCase()));
        }

        return ResponseEntity.ok(responseList);
    }


    private AppointmentsWebResponseDto createAppointmentWebResponseObject(Appointment appointment) {
        UserDto patient = fetchPatientByUid(appointment.getPatientUid());

        AppointmentsWebResponseDto responseDto = new AppointmentsWebResponseDto();
        responseDto.setAppointmentId(String.valueOf(appointment.getId()));
        responseDto.setPatientUid(appointment.getPatientUid());
        responseDto.setPatientName(patient.getFname() + " " + patient.getLname());
        responseDto.setDate(appointment.getAppointmentDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        responseDto.setSlot(appointment.getAppointmentStartsAt().format(DateTimeFormatter.ofPattern("hh:mm a")));
        responseDto.setMode(appointment.getAppointmentMode().name());
        responseDto.setStatus(appointment.getAppointmentStatus().name());
        responseDto.setChannelName(appointment.getMeetingLink());

        return responseDto;
    }

    private UserDto fetchPatientByUid(String uid) {
        if (uid != null && !uid.isEmpty()) {
            try {
                ApiFuture<DocumentSnapshot> user = firestore.collection("users").document(uid).get();
                DocumentSnapshot snapshot = user.get();

                if (snapshot != null) {
                    return snapshot.toObject(UserDto.class);
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("Firestore consultant retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore consultant retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            return null;
        }
    }

}
