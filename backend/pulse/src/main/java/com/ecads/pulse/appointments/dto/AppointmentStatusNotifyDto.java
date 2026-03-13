package com.ecads.pulse.appointments.dto;

import com.ecads.pulse.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AppointmentStatusNotifyDto {
    private String appointmentId;
    private PaymentStatus status;
}
