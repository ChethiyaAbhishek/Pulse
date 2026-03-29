package com.ecads.pulse.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalAppointmentsWebResponseDto {
    private String appointmentId;
    private String patientUid;
    private String patientName;
    private String consultantUid;
    private String consultantName;
    private String date;
    private String time;
    private String fee;
    private String appointmentStatus;
    private String paymentStatus;
}
