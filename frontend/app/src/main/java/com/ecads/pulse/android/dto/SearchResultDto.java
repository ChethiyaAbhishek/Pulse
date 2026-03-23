package com.ecads.pulse.android.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDto {
    private List<AvailableConsultantDto> consultantList;
    private List<HospitalDto> hospitalList;
}
