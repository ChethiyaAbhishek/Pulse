package com.ecads.pulse.consultant.controller;

import com.ecads.pulse.consultant.dto.AppointmentsRequestWebDto;
import com.ecads.pulse.consultant.dto.AppointmentsWebResponseDto;
import com.ecads.pulse.consultant.dto.AvailableDaysWebResponseDto;
import com.ecads.pulse.consultant.service.ConsultantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/consultant")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:8082",
        "http://localhost:8081"
})
public class ConsultantController {

    private final ConsultantService service;

    @PostMapping("/fetchUpComingAppointments")
    public ResponseEntity<List<AppointmentsWebResponseDto>> retrieveUpComingAppointments(
            @RequestBody AppointmentsRequestWebDto requestData) {
        return service.fetchUpComingAppointments(requestData);
    }

    @PostMapping("/fetchPreviousAppointments")
    public ResponseEntity<List<AppointmentsWebResponseDto>> retrievePreviousAppointments(
            @RequestBody AppointmentsRequestWebDto requestData) {
        return service.fetchPreviousAppointments(requestData);
    }

    @PostMapping("/fetchConsultantAvailableDays")
    public ResponseEntity<List<AvailableDaysWebResponseDto>> retrieveAvailableDays(
            @RequestBody AppointmentsRequestWebDto requestData) {
        return service.fetchAvailableDays(requestData);
    }

}
