package com.ecads.pulse.android.dto;

import com.ecads.pulse.android.enums.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentStatusNotifyDto {
    private String appointmentId;
    private PaymentStatus status;
}
