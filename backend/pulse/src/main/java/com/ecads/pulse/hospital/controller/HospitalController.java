package com.ecads.pulse.hospital.controller;

import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.hospital.dto.*;
import com.ecads.pulse.hospital.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital")
@Validated
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:8082",
        "http://localhost:8081"
})
public class HospitalController {

    private final HospitalService service;

    @PostMapping("/fetchConsultants")
    public ResponseEntity<List<HospitalConsultantsWebDto>> retrieveChannelingConsultants(
            @RequestBody SingleRequestDataDto requestData) {
        return service.fetchChannelingConsultants(requestData);
    }

    @PostMapping("/fetchAppointments")
    public ResponseEntity<List<HospitalAppointmentsWebResponseDto>> retrieveAppointments(
            @RequestBody HospitalAppointmentsWebRequestDto requestData) {
        return service.fetchAppointments(requestData);
    }

    @PostMapping("/appointmentCancelAndRefund")
    public ResponseEntity<Void> appointmentCancelAndRefund(
            @RequestBody SingleRequestDataDto requestData) {
        return service.appointmentCancelAndRefund(requestData);
    }

    @PostMapping("/appointmentReschedule")
    public ResponseEntity<Void> appointmentReschedule(
            @RequestBody AppointmentRescheduleRequestDto requestData) {
        return service.appointmentReschedule(requestData);
    }

    @PostMapping("/getAvailableDates")
    public ResponseEntity<List<String>> retrieveAvailableDates(
            @RequestBody ConsultantAvailableDatesWebRequestDto requestData) {
        return service.fetchAvailableDates(requestData);
    }

    @PostMapping("/getAvailableSlots")
    public ResponseEntity<List<String>> retrieveAvailableSlots(
            @RequestBody AvailableSlotsForDateWebRequestDto requestData) {
        return service.fetchAvailableSlots(requestData);
    }

}
