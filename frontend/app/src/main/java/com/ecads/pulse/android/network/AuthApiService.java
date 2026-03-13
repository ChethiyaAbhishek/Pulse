package com.ecads.pulse.android.network;

import retrofit2.Call;
import retrofit2.http.POST;

public interface AuthApiService {
    @POST("auth/patient")
    Call<Void> userAuth();

    @POST("auth/patient/logout")
    Call<Void> userLogout();
}
