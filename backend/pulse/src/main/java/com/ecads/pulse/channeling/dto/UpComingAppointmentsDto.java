package com.ecads.pulse.channeling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpComingAppointmentsDto {
    private String appointmentId;
    private String therapistName;
    private String scheduledTime;
    private String hospital;
    private String date;
    private String locationLink;
    private String videoLink;
}
