package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.FcmTokenDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("auth/patient")
    Call<Void> userAuth();

    @POST("auth/patient/logout")
    Call<Void> userLogout();

    @POST("auth/patient/updateFcmToken")
    Call<Void> updateFcmToken(
            @Body FcmTokenDto requestDto
    );
}
