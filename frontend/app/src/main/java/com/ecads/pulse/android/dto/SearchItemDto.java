package com.ecads.pulse.android.dto;

import lombok.Data;

@Data
public class SearchItemDto {

    public static final int TYPE_CONSULTANT = 1;
    public static final int TYPE_HOSPITAL = 2;

    private int type;
    private AvailableConsultantDto consultantDto;
    private HospitalDto hospitalDto;

    public SearchItemDto(AvailableConsultantDto consultantDto){
        this.setConsultantDto(consultantDto);
        this.setType(TYPE_CONSULTANT);
    }

    public SearchItemDto(HospitalDto hospitalDto){
        this.setHospitalDto(hospitalDto);
        this.setType(TYPE_HOSPITAL);
    }

}
