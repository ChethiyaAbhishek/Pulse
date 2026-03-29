package com.ecads.pulse.consultant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentsWebResponseDto {
    private String appointmentId;
    private String patientUid;
    private String patientName;
    private String date;
    private String slot;
    private String mode;
    private String status;
    private String channelName;
}
