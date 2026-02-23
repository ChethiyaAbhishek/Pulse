package com.ecads.pulse.android.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String uid;
    private String fname;
    private String lname;
    private String mobile;
    private String dob;
    private String gender;
    private String email;
}
