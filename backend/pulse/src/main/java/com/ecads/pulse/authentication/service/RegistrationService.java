package com.ecads.pulse.authentication.service;

import com.ecads.pulse.authentication.dto.JwtTokenDTO;
import com.ecads.pulse.authentication.dto.PatientRegRequestDTO;
import com.ecads.pulse.authentication.entity.User;
import com.ecads.pulse.authentication.exception.UserAlreadyExistsException;
import com.ecads.pulse.authentication.repository.UserRepository;
import com.ecads.pulse.authentication.util.JwtUtil;
import com.ecads.pulse.common.enums.UserRole;
import com.ecads.pulse.patient.entity.Patient;
import com.ecads.pulse.patient.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    private void checkUserExistence(String email) {
        User user = userRepository.findUserByEmail(email);
        if (user != null) {
            // user exist
            log.warn("A user already exists with the email: {}", email);
            throw new UserAlreadyExistsException("An account with this email already exists. Please log in instead.");
        }
        log.info("No user found from the email. Registration continues.");
    }

    @Transactional
    public ResponseEntity<JwtTokenDTO> registerPatient(@NonNull PatientRegRequestDTO requestDTO) {
        log.info("Patient registration started for email: {}", requestDTO.getEmail());
        checkUserExistence(requestDTO.getEmail());

        User user = new User();
        user.setRole(UserRole.PATIENT);
        user.setEmail(requestDTO.getEmail());
        user.setPhone(requestDTO.getMobile());
        user.setPasswordHash(passwordEncoder.encode(requestDTO.getPassword()));
        user.setFirstName(requestDTO.getFirstName());
        user.setLastName(requestDTO.getLastName());
        user.setDateOfBirth(requestDTO.getDateOfBirth());
        user.setGender(requestDTO.getGender());

        userRepository.save(user);
        log.info("User created for email: {}", requestDTO.getEmail());

        Patient patient = new Patient();
        patient.setUser(user);
        patient.setStrokeDate(requestDTO.getStrokeDate());
        patient.setCaregiverFirstName(requestDTO.getCaregiverFirstName());
        patient.setCaregiverLastName(requestDTO.getCaregiverLastName());
        patient.setCaregiverMobile(requestDTO.getCaregiverMobile());
        patient.setEmergencyContact(requestDTO.getEmergencyContact());

        patientRepository.save(patient);
        log.info("Patient registered with the email: {}", requestDTO.getEmail());

        log.info("Auth token generation started.");
        JwtTokenDTO tokenDTO = new JwtTokenDTO(
                JwtUtil.generateToken(requestDTO.getEmail()),
                JwtUtil.generateRefreshToken(requestDTO.getEmail())
        );

        return ResponseEntity.ok(tokenDTO);
    }
}
