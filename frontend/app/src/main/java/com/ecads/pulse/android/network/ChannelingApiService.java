package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.dto.HospitalDto;
import com.ecads.pulse.android.model.AppointmentDto;
import com.ecads.pulse.android.model.Consultant;
import com.ecads.pulse.android.model.Hospital;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChannelingApiService {

    @POST("channelling/upComingAppointments")
    Call<List<AppointmentDto>> retrieveAppointments(
            @Body String patientUid
    );

    @POST("channelling/consultantsForHospitalList")
    Call<List<AvailableConsultantDto>> consultantsForHospitalList(
            @Body List<HospitalDto> hospitalList
    );

    @POST("channelling/hospitalBasedConsultants")
    Call<List<Consultant>> channellingConsultants(
            @Body Map<String, String> requestData
    );

    @POST("channelling/consultantBasedHospitals")
    Call<List<HospitalDto>> channellingHospitals(
            @Body Map<String, String> requestData
    );

    @POST("channelling/hospitalBasedConsultantChannelingSlots")
    Call<List<String>> channellingDates(
            @Body Map<String, String> requestData
    );

    @POST("channelling/hospitalBasedConsultantChannelingDays")
    Call<List<String>> consultantAvailableDays(
            @Body Map<String, String> requestData
    );

    @POST("channelling/pricingDetails")
    Call<Map<String, Double>> pricingDetails(
            @Body Map<String, String> requestData
    );



}
