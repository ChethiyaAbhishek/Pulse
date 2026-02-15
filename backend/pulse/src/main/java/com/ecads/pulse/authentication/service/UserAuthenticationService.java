package com.ecads.pulse.authentication.service;

import com.ecads.pulse.authentication.dto.JwtTokenDTO;
import com.ecads.pulse.authentication.dto.LoginRequestDTO;
import com.ecads.pulse.authentication.dto.RoleBasedDTO;
import com.ecads.pulse.authentication.entity.User;
import com.ecads.pulse.authentication.exception.InvalidCredentialsException;
import com.ecads.pulse.authentication.exception.UserNotFoundException;
import com.ecads.pulse.authentication.repository.UserRepository;
import com.ecads.pulse.authentication.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserAuthenticationService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    private @NonNull User getUser(String email) {
        User user = repository.findUserByEmail(email);
        if (user == null) {
            log.warn("No user exists from the provided email address");
            throw new UserNotFoundException("No user exists from the provided email address");
        }
        log.info("A user exists from the provided email address");
        return user;
    }

    private boolean validatePassword(String rawPassword, String hashPassword) {
        log.info("Password validation started");
        return passwordEncoder.matches(rawPassword, hashPassword);
    }

    @Transactional
    public ResponseEntity<JwtTokenDTO> authenticateUser(@NonNull LoginRequestDTO requestDTO) {

        User user = getUser(requestDTO.getEmail());
        boolean matches = validatePassword(requestDTO.getPassword(), user.getPasswordHash());

        if (matches) {
            user.markActive();

            //JWT token
            try {
                JwtTokenDTO tokenDTO = new JwtTokenDTO(
                        JwtUtil.generateToken(requestDTO.getEmail()),
                        JwtUtil.generateRefreshToken(requestDTO.getEmail())
                );

                return ResponseEntity.ok(tokenDTO);
            } catch (JwtException e) {
                log.warn("Jwt Token generation failed ", e);
                throw new InvalidCredentialsException("Authentication Failed");
            }
        } else {
            log.warn("Invalid Password");
            throw new InvalidCredentialsException("Invalid Password");
        }
    }

    @Transactional
    public ResponseEntity<RoleBasedDTO> authenticateUserRole(@NonNull LoginRequestDTO requestDTO) {

        User user = getUser(requestDTO.getEmail());
        boolean matches = validatePassword(requestDTO.getPassword(), user.getPasswordHash());

        if (matches) {
            user.markActive();

            try {
                //JWT token
                JwtTokenDTO tokenDTO = new JwtTokenDTO(
                        JwtUtil.generateToken(requestDTO.getEmail()),
                        JwtUtil.generateRefreshToken(requestDTO.getEmail())
                );

                RoleBasedDTO roleBasedDTO = new RoleBasedDTO(
                        tokenDTO,
                        user.getRole()
                );

                return ResponseEntity.ok(roleBasedDTO);
            } catch (JwtException e) {
                log.warn("Jwt Token generation failed ", e);
                throw new InvalidCredentialsException("Authentication Failed");
            }
        } else {
            log.warn("Invalid Password ");
            throw new InvalidCredentialsException("Invalid Password");
        }
    }

    public ResponseEntity<JwtTokenDTO> newAccessToken(@NonNull JwtTokenDTO tokenDTO) {
        try {

            String email = JwtUtil.validateToken(tokenDTO.getRefreshToken());
            String accessToken = JwtUtil.generateToken(email);

            return ResponseEntity.ok(new JwtTokenDTO(
                    accessToken,
                    tokenDTO.getRefreshToken()
            ));

        } catch (JwtException e) {
            log.warn("Invalid Jwt Token", e);
            throw new InvalidCredentialsException("Authentication Failed");
        }
    }
}
