package com.ecads.pulse.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ExceptionResponseDTO {
    private int status;
    private String message;
    private String webRequestDescription;
}
