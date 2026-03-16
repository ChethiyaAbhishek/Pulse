package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.adapter.PreviousAppointmentsAdapter;
import com.ecads.pulse.android.databinding.ActivityPreviousAppointmentsBinding;
import com.ecads.pulse.android.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.android.dto.PatientUidRequestDto;
import com.ecads.pulse.android.dto.PreviousAppointmentDto;
import com.ecads.pulse.android.dto.SingleRequestDataDto;
import com.ecads.pulse.android.enums.PaymentStatus;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.network.AppointmentApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.EmergencyActions;
import com.ecads.pulse.android.utils.Permissions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class PreviousAppointmentsActivity extends AppCompatActivity {

    private ActivityPreviousAppointmentsBinding binding;
    private ActivityResultLauncher<String[]> launcher;
    private EmergencyActions emergencyActions;
    private FirebaseAuth firebaseAuth;
    private Retrofit retrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPreviousAppointmentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        checkAndRequestPermission();
        binding.preAppointmentsRecycler.setVisibility(View.GONE);
        binding.preAppointmentLoading.setVisibility(View.VISIBLE);

        emergencyActions = new EmergencyActions(this);

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                    }
                });


        binding.preAppointmentBtnEmergency.setOnClickListener(view -> {
            if (Permissions.permissionEnabled(this, Manifest.permission.CALL_PHONE) &&
                    Permissions.permissionEnabled(this, Manifest.permission.SEND_SMS)) {
                emergencyActions.makeCall();
                emergencyActions.sendSMS();
            } else {
                checkAndRequestPermission();
            }
        });

        binding.preAppointmentBtnBack.setOnClickListener(v -> {
            finish();
        });

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show();
            Log.e(PreviousAppointmentsActivity.class.getSimpleName(), "No user found");
            finish();
        }

        retrieveToken(bool -> {
            if (bool) {
                loadPreviousAppointments();
            }
        });

    }

    private void retrieveToken(CallBack<Boolean> instantiated) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Log.i("Firebase Auth", "Firebase Token retrieved");
                            retrofit = RetrofitClient.getInstance(getTokenResult.getToken());
                            instantiated.callBack(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Firebase Auth", "Token retrieval failed: " + e.getMessage());
                        }
                    });
        } else {
            // exception
            Log.e("Firebase Auth", "No firebase auth user");
        }
    }

    private void loadPreviousAppointments() {
        RecyclerView recyclerView = binding.preAppointmentsRecycler;
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));

        binding.noPreAppointmentsCard.setVisibility(View.GONE);
        binding.preAppointmentsRecycler.setVisibility(View.VISIBLE);
        binding.preAppointmentLoading.setVisibility(View.GONE);

        fetchAppointmentsForUser(firebaseAuth.getUid(), previousAppointmentList -> {
            if (!previousAppointmentList.isEmpty()) {

                PreviousAppointmentsAdapter adapter = new PreviousAppointmentsAdapter(previousAppointmentList,
                        onInvoiceClick -> {

                        }, onCancelClick -> {
                    new AlertDialog.Builder(this)
                            .setTitle("Cancel Appointment")
                            .setMessage("\n Do you want to cancel the appointment? \n\n Consultant: " + onCancelClick.getConsultantName() + " \n Session: " + onCancelClick.getSlot() + " - " + onCancelClick.getEndsAt() + " \n Date: " + onCancelClick.getDate())
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    cancelConfirmedAppointment(onCancelClick.getAppointmentId());
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                });
                recyclerView.setAdapter(adapter);
            } else {
                // no appointments
                recyclerView.setVisibility(View.GONE);
                binding.noPreAppointmentsCard.setVisibility(View.VISIBLE);
            }
        });

    }

    private void fetchAppointmentsForUser(
            String patientUid, CallBack<List<PreviousAppointmentDto>> callBack) {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);

        Call<List<PreviousAppointmentDto>> appointmentList = apiService.retrieveAppointmentList(new PatientUidRequestDto(patientUid));
        appointmentList.enqueue(new Callback<List<PreviousAppointmentDto>>() {
            @Override
            public void onResponse(Call<List<PreviousAppointmentDto>> call, Response<List<PreviousAppointmentDto>> response) {
                if (response.isSuccessful()) {
                    Log.i("check", "list size: " + response.body().size());
                    callBack.callBack(response.body());
                } else {
                    Log.d("PreviousAppointmentsActivity", "Appointment list retrieving failed: " + response.message());
                    Toast.makeText(PreviousAppointmentsActivity.this, "Appointment list retrieving failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<PreviousAppointmentDto>> call, Throwable throwable) {
                Log.e("PreviousAppointmentsActivity", "Appointment list retrieving failed: " + throwable.getMessage());
                Toast.makeText(PreviousAppointmentsActivity.this, "Appointment list retrieving failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelConfirmedAppointment(String appointmentId) {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);

        Call<Void> cancelled = apiService.cancelConfirmedAppointment(new SingleRequestDataDto(appointmentId));
        cancelled.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(PreviousAppointmentsActivity.class.getSimpleName(), "[APPOINTMENT_CANCEL: SUCCESS ]: " + response.code());

                    // NOTIFICATION
                    checkAndRequestPermission();
                    sendNotification("Appointment Cancelled", "Your appointment has been cancelled.");

                } else {
                    Log.d(PreviousAppointmentsActivity.class.getSimpleName(), "[APPOINTMENT_CANCEL: FAILED ]: " + response.code());

                    // NOTIFICATION
                    checkAndRequestPermission();
                    sendNotification("Appointment Cancellation Failed", "Your appointment cancellation has been failed.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.e(PreviousAppointmentsActivity.class.getSimpleName(), "[APPOINTMENT_CANCEL: FAILED ]: " + throwable);

                // NOTIFICATION
                checkAndRequestPermission();
                sendNotification("Appointment Cancellation Failed", "Your appointment cancellation has been failed.");
            }
        });
    }

    private void sendNotification(String title, String msg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, SplashActivity.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
            int notificationId = 1;
            notificationManager.notify(notificationId, builder.build());
        } else {
            checkAndRequestPermission();
        }
    }

    private void checkAndRequestPermission() {
        if (!Permissions.permissionEnabled(this, Manifest.permission.POST_NOTIFICATIONS)
                || !Permissions.permissionEnabled(this, Manifest.permission.CALL_PHONE)
                || !Permissions.permissionEnabled(this, Manifest.permission.SEND_SMS)) {

            launcher.launch(new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        }
    }
}