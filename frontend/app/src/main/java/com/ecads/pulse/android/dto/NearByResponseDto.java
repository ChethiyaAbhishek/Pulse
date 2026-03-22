package com.ecads.pulse.android.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NearByResponseDto {
    private List<Result> results;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result {
        private String place_id;
        private String name;
        private List<Photo> photos;
        private Geometry geometry;
        private String vicinity;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Photo {
        private String photo_reference;
        private int height;
        private int width;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Geometry {
        private Location location;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Location {
        private double lat;
        private double lng;
    }
}
