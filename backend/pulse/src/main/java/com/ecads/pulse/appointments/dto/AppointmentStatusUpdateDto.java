package com.ecads.pulse.appointments.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AppointmentStatusUpdateDto {
    private String appointmentId;
    private int statusId;
}
