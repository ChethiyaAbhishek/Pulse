package com.ecads.pulse.android.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Consultant {

    private String uid;
    private String title;
    private String name;
    private String lowerName;
    private String speciality;
    private String baseHospital;

}
