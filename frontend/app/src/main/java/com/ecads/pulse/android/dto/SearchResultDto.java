package com.ecads.pulse.android.dto;

import com.ecads.pulse.android.model.Hospital;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDto {
    private AvailableConsultantDto consultantDto;
    private HospitalDto hospital;
}
