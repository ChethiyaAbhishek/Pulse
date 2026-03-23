package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.dto.ChannelNameAndTokenDto;
import com.ecads.pulse.android.dto.HospitalDto;
import com.ecads.pulse.android.dto.SearchResultDto;
import com.ecads.pulse.android.dto.SingleRequestDataDto;
import com.ecads.pulse.android.dto.TeleMedTokenReqDto;
import com.ecads.pulse.android.model.AppointmentDto;
import com.ecads.pulse.android.model.Consultant;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ChannelingApiService {

    @POST("channelling/upComingAppointments")
    Call<List<AppointmentDto>> retrieveAppointments(
            @Body SingleRequestDataDto requestData
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

    @POST("channelling/telemedChannelAndToken")
    Call<ChannelNameAndTokenDto> retrieveTeleMedChannelAndToken(
            @Body TeleMedTokenReqDto requestData
    );

    @POST("channelling/searchConsultantOrHospital")
    Call<SearchResultDto> retrieveSearchResults(
            @Body SingleRequestDataDto requestData
    );

    @POST("channelling/todayAppointments")
    Call<List<AppointmentDto>> retrieveTodayAppointments(
            @Body SingleRequestDataDto requestData
    );

    @POST("channelling/recentlyBookedConsultants")
    Call<List<AvailableConsultantDto>> recentlyBookedConsultants(
            @Body SingleRequestDataDto requestData
    );

}
