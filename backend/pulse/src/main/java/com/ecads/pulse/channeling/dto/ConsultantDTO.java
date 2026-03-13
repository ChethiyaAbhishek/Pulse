package com.ecads.pulse.channeling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsultantDTO {
    String uid;
    String title;
    String name;
    String lowerName;
    String speciality;
    String baseHospital;
}
