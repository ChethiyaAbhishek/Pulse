package com.ecads.pulse.authentication.controller;

import com.ecads.pulse.authentication.dto.FcmTokenDto;
import com.ecads.pulse.authentication.dto.LoginRequestDTO;
import com.ecads.pulse.authentication.dto.RoleBasedDTO;
import com.ecads.pulse.authentication.service.UserAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
@RequiredArgsConstructor
public class UserAuthenticationController {

    private final UserAuthenticationService service;

    private static final String AUTHORIZATION = "Authorization";

    @PostMapping("/patient")
    public ResponseEntity<Void> authPatient(@RequestHeader(AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return service.authenticatePatient(token);
    }

    @PostMapping("/patient/logout")
    public ResponseEntity<Void> patientLogout(@RequestHeader(AUTHORIZATION) String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return service.patientLogout(token);
    }

    @PostMapping("/patient/updateFcmToken")
    public ResponseEntity<Void> updateFcmToken(
            @RequestBody FcmTokenDto requestData) {
        return service.updatePatientFcmToken(requestData);
    }

    @PostMapping("/login/userRole")
    public ResponseEntity<RoleBasedDTO> authUserRole(@RequestBody LoginRequestDTO requestDTO) {
//        return service.authenticateUserRole(requestDTO);
        return null;
    }
}
