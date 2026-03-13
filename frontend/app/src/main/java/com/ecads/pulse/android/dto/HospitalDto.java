package com.ecads.pulse.android.dto;

import com.google.firebase.firestore.GeoPoint;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
