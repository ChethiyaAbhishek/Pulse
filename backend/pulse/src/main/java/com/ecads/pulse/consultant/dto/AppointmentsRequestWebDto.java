package com.ecads.pulse.consultant.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AppointmentsRequestWebDto {
    private String consultantUid;
    private String hospitalPlaceId;
}
