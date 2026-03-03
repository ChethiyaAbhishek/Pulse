package com.ecads.pulse.android.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Hospital {
    private String placeId;
    private String imageUrl;
    private String name;
    private String address;
    private String contact;
    private String email;
    private String[] location;
    private String geohash;
    private String mapUrl;
}
