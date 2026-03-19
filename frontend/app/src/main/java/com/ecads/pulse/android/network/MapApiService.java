package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.NearByResponseDto;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface MapApiService {
    @GET("maps/api/place/nearbysearch/json")
    Call<NearByResponseDto> getNearbyHospitals(
            @Query("location") String location,
            @Query("radius") int radius,
            @Query("type") String type,
            @Query("keyword") String keyword,
            @Query("key") String apiKey
    );
}
