package com.ecads.pulse.android.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private String uid;
    private String strokeDate;
    private String cgiverFname;
    private String cgiverLname;
    private String cgiverContact;
    private String emerContact;
}
