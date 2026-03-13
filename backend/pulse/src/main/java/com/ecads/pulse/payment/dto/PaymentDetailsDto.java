package com.ecads.pulse.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PaymentDetailsDto {
    private double total;
    private String merchantId;
}
