package com.ecads.pulse.appointments.controller;

import com.ecads.pulse.appointments.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.appointments.dto.AppointmentStatusUpdateDto;
import com.ecads.pulse.appointments.dto.PatientUidRequestDto;
import com.ecads.pulse.appointments.dto.PreviousAppointmentsDto;
import com.ecads.pulse.appointments.service.AppointmentService;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointment")
@Validated
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:8082",
        "http://localhost:8081"
})
public class AppointmentController {

    private final AppointmentService service;

    @PostMapping("/createPending")
    public ResponseEntity<Map<String, String>> createPendingAppointment(
            @RequestBody Map<String, String> requestData) {
        return service.createPendingAppointment(requestData);
    }

    @PostMapping("/cancelAppointment")
    public ResponseEntity<Void> cancelAppointment(
            @RequestBody AppointmentStatusNotifyDto requestData) {
        return service.cancelAppointment(requestData);
    }

    @PostMapping("/confirmAppointment")
    public ResponseEntity<Void> confirmAppointment(
            @RequestBody AppointmentStatusNotifyDto requestData) {
        return service.confirmAppointment(requestData);
    }

    @PostMapping("/cancelConfirmedAppointment")
    public ResponseEntity<Void> cancelConfirmedAppointment(
            @RequestBody SingleRequestDataDto requestData) {
        return service.cancelConfirmedAppointment(requestData);
    }

    @PostMapping("/retrieveAppointmentList")
    public ResponseEntity<List<PreviousAppointmentsDto>> retrieveAppointmentList(
            @RequestBody PatientUidRequestDto requestDto) {
        return service.fetchAppointmentList(requestDto.getPatientUid());
    }

    @PostMapping("/updateOnlineAppointmentStatus")
    public ResponseEntity<Void> updateOnlineAppointmentStatus(
            @RequestBody AppointmentStatusUpdateDto requestData) {
        return service.updateOnlineAppointmentStatus(requestData);
    }

    @PostMapping("/updateNoShowStatus")
    public ResponseEntity<Void> updateNoShowAppointmentStatus(
            @RequestBody SingleRequestDataDto requestData) {
        return service.updateNoShowAppointmentStatus(requestData);
    }
}
