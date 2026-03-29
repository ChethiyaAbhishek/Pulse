package com.ecads.pulse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private String uid;
    private String dob;
    private String email;
    private String fname;
    private String gender;
    private String lname;
    private String mobile;
}
