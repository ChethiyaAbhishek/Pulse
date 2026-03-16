package com.ecads.pulse.android.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentDto {
    private String appointmentId;
    private String therapistName;
    private String scheduledTime;
    private String hospital;
    private String date;
    private String locationLink;
    private String videoLink;
}
