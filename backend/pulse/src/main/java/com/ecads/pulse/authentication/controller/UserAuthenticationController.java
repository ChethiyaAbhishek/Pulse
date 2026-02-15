package com.ecads.pulse.authentication.controller;

import com.ecads.pulse.authentication.dto.JwtTokenDTO;
import com.ecads.pulse.authentication.dto.LoginRequestDTO;
import com.ecads.pulse.authentication.dto.RoleBasedDTO;
import com.ecads.pulse.authentication.service.UserAuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class UserAuthenticationController {

    private final UserAuthenticationService service;

    @PostMapping("/login/patient")
    public ResponseEntity<JwtTokenDTO> authPatient(@RequestBody LoginRequestDTO requestDTO) {
        return service.authenticateUser(requestDTO);
    }

    @PostMapping("/login/userRole")
    public ResponseEntity<RoleBasedDTO> authUserRole(@RequestBody LoginRequestDTO requestDTO) {
        return service.authenticateUserRole(requestDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDTO> reNewAccessToken(@RequestBody JwtTokenDTO tokenDTO) {
        return service.newAccessToken(tokenDTO);
    }

}
