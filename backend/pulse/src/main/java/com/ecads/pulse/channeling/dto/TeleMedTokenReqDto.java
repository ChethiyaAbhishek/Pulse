package com.ecads.pulse.channeling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeleMedTokenReqDto {
    private String patientUid;
    private String appointmentId;
}
