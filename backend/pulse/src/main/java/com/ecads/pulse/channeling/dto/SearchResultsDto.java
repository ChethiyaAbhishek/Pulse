package com.ecads.pulse.channeling.dto;

import com.ecads.pulse.hospital.dto.HospitalApiDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultsDto {
    private List<AvailableConsultantDto> consultantList;
    private List<HospitalApiDto> hospitalList;
}
