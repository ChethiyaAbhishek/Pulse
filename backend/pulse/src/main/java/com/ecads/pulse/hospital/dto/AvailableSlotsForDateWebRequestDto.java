package com.ecads.pulse.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailableSlotsForDateWebRequestDto {
    private String consultantUid;
    private String hospitalId;
    private String date;
}
