package com.ecads.pulse.appointments.service;

import com.ecads.pulse.appointments.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentMode;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.authentication.service.UserAuthenticationService;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.payment.entity.AppointmentPayment;
import com.ecads.pulse.payment.enums.PaymentStatus;
import com.ecads.pulse.payment.repository.AppointmentPaymentsRepository;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserAuthenticationService authenticationService;
    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final AppointmentPaymentsRepository paymentsRepository;

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
            // channel name [agora]
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

            if (appointment.getAppointmentStatus() == AppointmentStatus.CONFIRMED
                    && appointment.getPaymentStatus() == PaymentStatus.PAID) {

                if (payment.getPaymentStatus() == PaymentStatus.PAID) {
                    return ResponseEntity.ok().build();
                } else {
                    log.debug("Appointment payment confirmation failed. Payment Status: {} [Expected: {}]",
                            payment.getPaymentStatus(), PaymentStatus.PAID);
                    throw new ShowErrorException("Appointment payment confirmation failed");
                }
            } else {
                if(payment.getPaymentStatus() == PaymentStatus.PAID){
                    if(appointment.getAppointmentStatus() != AppointmentStatus.CONFIRMED){
                        log.debug("Appointment confirming logic error. Appointment Status: {} [Expected: {}]",
                                appointment.getAppointmentStatus(), AppointmentStatus.CONFIRMED);
                    }else if(appointment.getPaymentStatus() != PaymentStatus.PAID){
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

}
