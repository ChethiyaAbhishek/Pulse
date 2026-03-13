package com.ecads.pulse.channeling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalBasedConsultantsResponseDTO {
    List<ConsultantDTO> consultantDTOList;
    Map<String, List<String>> channellingSlotsMap;
}
