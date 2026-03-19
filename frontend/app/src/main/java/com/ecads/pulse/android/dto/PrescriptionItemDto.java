package com.ecads.pulse.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrescriptionItemDto {
    private String dosage;
    private String duration;
    private String frequency;
    private String instructions;
    private String medicineName;
    private String prescriptionId;
}
