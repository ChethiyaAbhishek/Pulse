package com.ecads.pulse.patient.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientDto {
    private String uid;
    private String cgiverContact;
    private String cgiverFname;
    private String cgiverLname;
    private String emerContact;
    private String strokeDate;
}
