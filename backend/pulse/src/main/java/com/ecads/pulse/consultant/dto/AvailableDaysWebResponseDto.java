package com.ecads.pulse.consultant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailableDaysWebResponseDto {
    private String day;
    private boolean morning;
    private boolean afternoon;
}
