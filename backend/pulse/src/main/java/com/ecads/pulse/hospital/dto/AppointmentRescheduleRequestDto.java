package com.ecads.pulse.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentRescheduleRequestDto {
    private String appointmentId;
    private String date;
    private String slot;
    private String mode;
}
