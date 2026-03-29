package com.ecads.pulse.telemedicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FirebasePrescriptionDto {
    private String appointmentId;
    private String consultantName;
    private String consultantUid;
    private String issuedDate;
    private String notes;
    private String patientId;
    private String prescriptionId;
    private String status;
}
