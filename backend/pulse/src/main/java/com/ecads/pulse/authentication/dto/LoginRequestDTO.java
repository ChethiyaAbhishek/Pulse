package com.ecads.pulse.authentication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class LoginRequestDTO {

    @NotBlank(message = "Email address is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "password is required.")
    private String password;
}
