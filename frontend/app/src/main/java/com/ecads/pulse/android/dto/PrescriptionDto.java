package com.ecads.pulse.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionDto {
    private String prescriptionId;
    private String appointmentId;
    private String consultantName;
    private String consultantUid;
    private String issuedDate;
    private String notes;
    private String patientId;
    private String status;
}
