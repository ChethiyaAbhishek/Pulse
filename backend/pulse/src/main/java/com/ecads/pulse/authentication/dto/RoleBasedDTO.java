package com.ecads.pulse.authentication.dto;

import com.ecads.pulse.common.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class RoleBasedDTO {
    private JwtTokenDTO tokenDTO;
    private UserRole role;
}
