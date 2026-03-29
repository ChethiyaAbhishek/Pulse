package com.ecads.pulse.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlatformFinancialSummaryWebResponseDto {
    private String totalPlatformRevenue;
    private String totalCommission;
    private String totalRefundedAmount;
    private String netPlatformRevenue;
    private String avgAppointmentFee;
    private String hospitalCount;
}
