package com.ecads.pulse.telemedicine.controller;

import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.telemedicine.dto.PrescriptionSaveDto;
import com.ecads.pulse.telemedicine.service.TelemedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/telemed")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:8081")
public class TelemedicineController {

    private final TelemedicineService service;

    @PostMapping("/sendPushNotificationSessionStarted")
    public ResponseEntity<Void> sendPushNotificationSessionStarted(
            @RequestBody SingleRequestDataDto requestData) {
        return service.notifyPatientSessionStarted(requestData);
    }

    @PostMapping("/savePrescription")
    public ResponseEntity<Void> savePrescription(
            @RequestBody PrescriptionSaveDto requestData) {
        return service.savePrescription(requestData);
    }

}
