package com.ecads.pulse.hospital.dto;

import com.ecads.pulse.channeling.dto.ConsultantDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalConsultantsWebDto {
    private ConsultantDTO consultantDTO;
    private String fee;
    private List<String> availableDays;
}
