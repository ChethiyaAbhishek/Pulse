package com.ecads.pulse.android.utils;

import android.util.Log;

import com.ecads.pulse.android.dto.FcmTokenDto;
import com.ecads.pulse.android.network.AuthApiService;
import com.ecads.pulse.android.network.RetrofitClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class Helper {

    public static final String HOSPITAL = "hospital";
    public static final String CONSULTANT = "consultant";

    private static DateTimeFormatter inputFormat =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static DateTimeFormatter outputFormat =
            DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);

    public static String formatDate(String date) {
        LocalDate formatedInput = LocalDate.parse(date, inputFormat);
        if (LocalDate.now().equals(formatedInput)) {
            return "Today";
        }
        return formatedInput.format(outputFormat);
    }

    public static void updateFcmToken(String uid, String firebaseToken, String fcmToken) {
        Retrofit retrofit = RetrofitClient.getInstance(firebaseToken);
        AuthApiService apiService = retrofit.create(AuthApiService.class);

        FcmTokenDto dto = new FcmTokenDto(uid, fcmToken);

        Call<Void> fcmTokenUpdate = apiService.updateFcmToken(dto);
        fcmTokenUpdate.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.i("FcmToken", "Fcm token updated");
                } else {
                    Log.d("FcmToken", "Fcm token updating failed: [" + response.code() + "]");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.d("FcmToken", "Fcm token updating failed: " + throwable);
            }
        });

    }
}
