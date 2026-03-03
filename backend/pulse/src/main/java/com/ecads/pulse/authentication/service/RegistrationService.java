package com.ecads.pulse.authentication.service;

import com.ecads.pulse.authentication.dto.JwtTokenDTO;
import com.ecads.pulse.authentication.dto.PatientRegRequestDTO;
import com.ecads.pulse.authentication.entity.User;
import com.ecads.pulse.authentication.repository.UserRepository;
import com.ecads.pulse.authentication.util.JwtUtil;
import com.ecads.pulse.common.enums.UserRole;
import com.ecads.pulse.patient.entity.Patient;
import com.ecads.pulse.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    @Transactional
    public ResponseEntity<JwtTokenDTO> registerPatient(@NonNull PatientRegRequestDTO requestDTO) {
//        log.info("Patient registration started for email: {}", requestDTO.getEmail());
//
//        User user = new User();
//        user.setRole(UserRole.PATIENT);
//
//        userRepository.save(user);
//        log.info("User created for email: {}", requestDTO.getEmail());
//
//        Patient patient = new Patient();
//        patient.setUser(user);
//        patient.setStrokeDate(requestDTO.getStrokeDate());
//
//        patientRepository.save(patient);
//        log.info("Patient registered with the email: {}", requestDTO.getEmail());
//
//        log.info("Auth token generation started.");
//        JwtTokenDTO tokenDTO = new JwtTokenDTO(
//                JwtUtil.generateToken(requestDTO.getEmail()),
//                JwtUtil.generateRefreshToken(requestDTO.getEmail())
//        );

        return null;
    }
}
