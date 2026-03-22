package com.ecads.pulse.android.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlacesApiDto {
    private String placeId;
    private String imageUrl;
    private String name;
    private String lowerName;
    private String address;
    private LocationDto location;
    private String geohash;
    private String mapUrl;
}
