package com.ecads.pulse.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PayhereNotifyDto {
    private String merchantId;
    private String appointmentId;
    private String paymentId;
    private String amount;
    private String currency;
    private String statusCode;
    private String md5sig;
}
