package com.ecads.pulse.appointments.dto;

import com.ecads.pulse.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PreviousAppointmentsDto {
    private String appointmentId;
    private String channelingHospital;
    private String consultantName;
    private String consultantSpeciality;
    private String baseHospital;
    private String hospitalMapUrl;
    private String onlineChannelLink;
    private String slot;
    private String date;
    private PaymentStatus paymentStatus;
    private String endsAt;
}
