package com.ecads.pulse.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreviousAppointmentDto {

    private String appointmentId;
    private String channelingHospital;
    private String consultantName;
    private String consultantSpeciality;
    private String baseHospital;
    private String hospitalMapUrl;
    private String onlineChannelLink;
    private String slot;
    private String date;
}
