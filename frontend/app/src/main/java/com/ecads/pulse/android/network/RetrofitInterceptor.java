package com.ecads.pulse.android.network;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;

public class RetrofitInterceptor implements Interceptor {

    private String firebaseToken;
    private static final String AUTHORIZATION = "Authorization";

    public RetrofitInterceptor(String firebaseToken) {
        this.firebaseToken = firebaseToken;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {

        Request request = chain.request();

        Request bearerRequest = request.newBuilder()
                .header(RetrofitInterceptor.AUTHORIZATION, "Bearer " + this.firebaseToken)
                .build();

        return chain.proceed(bearerRequest);
    }
}
