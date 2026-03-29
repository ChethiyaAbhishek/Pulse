package com.ecads.pulse.authentication.service;

import com.ecads.pulse.authentication.dto.FcmTokenDto;
import com.ecads.pulse.authentication.dto.LoginRequestDTO;
import com.ecads.pulse.authentication.dto.RoleBasedDTO;
import com.ecads.pulse.authentication.entity.User;
import com.ecads.pulse.authentication.exception.FirebaseAuthFailedException;
import com.ecads.pulse.authentication.repository.UserRepository;
import com.ecads.pulse.common.enums.UserRole;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.common.util.FirebaseTokenVerification;
import com.google.firebase.auth.FirebaseToken;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final UserRepository repository;
    private final FirebaseTokenVerification tokenVerification;

    public FirebaseToken authUser(String token) {
        try {
            log.info("User authentication started");
            return tokenVerification.verify(token);
        } catch (Exception e) {
            log.error("User authentication failed: {}", e.getMessage());
            throw new FirebaseAuthFailedException(e.getMessage());
        }
    }

    @Transactional
    public ResponseEntity<Void> authenticatePatient(String token) {
        log.info("Patient authentication started");
        FirebaseToken authUser = authUser(token);
        String uid = authUser.getUid();

        Optional<User> userRecord = repository.findByUid(uid);
        if (userRecord.isPresent()) {
            // login
            log.info("Patient login");
            User user = userRecord.get();
            user.markActive();
        } else {
            // registration
            log.info("Patient registration");
            User user = new User();
            user.setUid(uid);
            user.setRole(UserRole.PATIENT);
            user.setIsActive(true);
            repository.save(user);
        }
        return ResponseEntity.ok(null);
    }

    @Transactional
    public ResponseEntity<Void> patientLogout(String token) {
        log.info("Patient logout process");
        FirebaseToken authUser = authUser(token);
        String uid = authUser.getUid();

        Optional<User> userRecord = repository.findByUid(uid);

        if (userRecord.isPresent()) {
            log.info("Marked inactive");
            User user = userRecord.get();
            user.markInactive();
            return ResponseEntity.ok(null);
        } else {
            log.error("No user record found");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Transactional
    public ResponseEntity<Void> updatePatientFcmToken(FcmTokenDto requestData) {
        if (requestData == null || requestData.getPatientUid().isEmpty()
                || requestData.getFcmToken().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String patientUid = requestData.getPatientUid();
        String fcmToken = requestData.getFcmToken();

        User user = repository.getByUid(patientUid);

        if (user != null) {

            user.setFcmToken(fcmToken);

            return ResponseEntity.ok(null);
        } else {
            throw new ShowErrorException("No user found");
        }
    }

    @Transactional
    public ResponseEntity<RoleBasedDTO> authenticateUserRole(@NonNull LoginRequestDTO requestDTO) {
//
//        User user = getUser(requestDTO.getEmail());
//        boolean matches = validatePassword(requestDTO.getPassword(), user.getPasswordHash());
//
//        if (matches) {
//            user.markActive();
//
//            try {
//                //JWT token
//                JwtTokenDTO tokenDTO = new JwtTokenDTO(
//                        JwtUtil.generateToken(requestDTO.getEmail()),
//                        JwtUtil.generateRefreshToken(requestDTO.getEmail())
//                );
//
//                RoleBasedDTO roleBasedDTO = new RoleBasedDTO(
//                        tokenDTO,
//                        user.getRole()
//                );
//
//                return ResponseEntity.ok(roleBasedDTO);
//            } catch (JwtException e) {
//                log.warn("Jwt Token generation failed ", e);
//                throw new InvalidCredentialsException("Authentication Failed");
//            }
//        } else {
//            log.warn("Invalid Password ");
//            throw new InvalidCredentialsException("Invalid Password");
//        }
        return null;
    }

}
