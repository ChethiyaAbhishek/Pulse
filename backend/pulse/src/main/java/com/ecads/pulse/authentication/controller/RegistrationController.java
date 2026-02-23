package com.ecads.pulse.authentication.controller;

import com.ecads.pulse.authentication.dto.JwtTokenDTO;
import com.ecads.pulse.authentication.dto.PatientRegRequestDTO;
import com.ecads.pulse.authentication.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reg")
@Validated
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService service;

    @PostMapping("/patient")
    public ResponseEntity<JwtTokenDTO> registerPatient(@RequestBody @Valid PatientRegRequestDTO requestDTO) {
        return service.registerPatient(requestDTO);
    }

//    public ResponseEntity<JwtTokenDTO> registerTherapist(@RequestBody){}

}
