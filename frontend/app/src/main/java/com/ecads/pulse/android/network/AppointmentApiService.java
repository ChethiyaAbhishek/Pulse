package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.AppointmentStatusNotifyDto;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AppointmentApiService {

    @POST("appointment/createPending")
    Call<Map<String, String>> pendingAppointment(
            @Body Map<String, String> requestData
    );

    @POST("appointment/cancelAppointment")
    Call<Void> cancelAppointment(
            @Body AppointmentStatusNotifyDto notifyDto
    );

    @POST("appointment/confirmAppointment")
    Call<Void> confirmAppointment(
            @Body AppointmentStatusNotifyDto notifyDto
    );

}
