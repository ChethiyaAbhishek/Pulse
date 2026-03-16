package com.ecads.pulse.appointments.dto;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.channeling.dto.ConsultantDTO;
import com.ecads.pulse.hospital.dto.HospitalApiDto;
import com.ecads.pulse.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

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
