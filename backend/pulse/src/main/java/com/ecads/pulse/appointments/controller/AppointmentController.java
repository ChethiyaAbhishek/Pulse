package com.ecads.pulse.appointments.controller;

import com.ecads.pulse.appointments.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.appointments.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/appointment")
@Validated
@RequiredArgsConstructor
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
}
