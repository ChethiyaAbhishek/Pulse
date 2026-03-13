package com.ecads.pulse.android.network;

import com.ecads.pulse.android.dto.PaymentDetailsDto;
import com.ecads.pulse.android.enums.PaymentStatus;
import com.ecads.pulse.android.model.Consultant;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PaymentApiService {

    @POST("payment/channeling")
    Call<PaymentDetailsDto> channelingPaymentDetails(
            @Body Map<String, String> requestData
    );

}
