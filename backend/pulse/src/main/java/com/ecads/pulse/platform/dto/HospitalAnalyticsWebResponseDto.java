package com.ecads.pulse.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalAnalyticsWebResponseDto {
    private String totalHospitalCount;
    private String totalConsultants;
    private String totalAppointments;
    private List<HospitalAnalyticsHospitalDto> hospitals;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HospitalAnalyticsHospitalDto{
        private String hospitalPlaceId;
        private String name;
        private String address;
        private String consultantCount;
        private String appointmentsCount;
        private String totalRevenue;
    }
}
