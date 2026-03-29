package com.ecads.pulse.patient.controller;

import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.patient.dto.PatientDetailsWebResponseDto;
import com.ecads.pulse.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patient")
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:8082",
        "http://localhost:8081"
})
public class PatientController {

    private final PatientService service;

    @PostMapping("/fetchPatientDetails")
    public ResponseEntity<PatientDetailsWebResponseDto> retrievePatientDetails(
            @RequestBody SingleRequestDataDto requestData) {
        return service.fetchPatientDetails(requestData);
    }

}
