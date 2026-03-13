package com.ecads.pulse.hospital.dto;

import com.google.cloud.firestore.GeoPoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
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
    private GeoPoint location;
    private String geohash;
    private String mapUrl;
}
