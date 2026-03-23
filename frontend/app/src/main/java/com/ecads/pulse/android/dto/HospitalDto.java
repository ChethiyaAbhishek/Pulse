package com.ecads.pulse.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HospitalDto {
    private String placeId;
    private String imageUrl;
    private String name;
    private String lowerName;
    private String address;
    private String contact;
    private String email;
    private LocationDto location;
    private String geohash;
    private String mapUrl;
}
