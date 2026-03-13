package com.ecads.pulse.android.dto;

import com.ecads.pulse.android.model.Consultant;
import com.ecads.pulse.android.model.Hospital;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvailableConsultantDto {
    private Consultant consultant;
    private Hospital hospital;
    private List<String> availableSlots;
}
