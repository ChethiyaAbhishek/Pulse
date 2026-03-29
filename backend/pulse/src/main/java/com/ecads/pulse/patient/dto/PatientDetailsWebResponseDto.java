package com.ecads.pulse.patient.dto;

import com.ecads.pulse.patient.entity.Diagnosis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PatientDetailsWebResponseDto {
    private String name;
    private String age;
    private String gender;
    private String phone;
    private String email;
    private String address;
    private String emergencyContact;
    private String careGiverName;
    private String careGiverPhone;
    private List<Diagnosis> diagnosesList;
}
