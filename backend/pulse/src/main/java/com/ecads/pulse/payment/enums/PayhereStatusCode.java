package com.ecads.pulse.payment.enums;

import lombok.Getter;

@Getter
public enum PayhereStatusCode {
    SUCCESS(2),
    PENDING(0),
    CANCELLED(-1),
    FAILED(-2),
    CHARGED_BACK(-3);

    private final int code;

    PayhereStatusCode(int code) {
        this.code = code;
    }
}
