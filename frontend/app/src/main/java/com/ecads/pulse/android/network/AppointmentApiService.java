package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.android.dto.AppointmentStatusUpdateDto;
import com.ecads.pulse.android.dto.PatientUidRequestDto;
import com.ecads.pulse.android.dto.PreviousAppointmentDto;
import com.ecads.pulse.android.dto.SingleRequestDataDto;

import java.util.List;
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

    @POST("appointment/cancelConfirmedAppointment")
    Call<Void> cancelConfirmedAppointment(
            @Body SingleRequestDataDto requestData
    );

    @POST("appointment/retrieveAppointmentList")
    Call<List<PreviousAppointmentDto>> retrieveAppointmentList(
            @Body PatientUidRequestDto requestDto
    );

    @POST("appointment/updateOnlineAppointmentStatus")
    Call<Void> updateOnlineAppointmentStatus(
            @Body AppointmentStatusUpdateDto requestDto
    );



}
