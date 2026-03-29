package com.ecads.pulse.telemedicine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FirebasePrescriptionItemDto {
    private String dosage;
    private String duration;
    private String frequency;
    private String instructions;
    private String medicineName;
    private String prescriptionId;
}
