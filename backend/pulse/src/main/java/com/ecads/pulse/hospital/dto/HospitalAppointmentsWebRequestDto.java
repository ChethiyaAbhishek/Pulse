package com.ecads.pulse.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalAppointmentsWebRequestDto {
    private String hospitalId;
    private int startAt;
}
