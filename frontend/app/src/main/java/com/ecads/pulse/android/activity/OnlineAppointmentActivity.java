package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.ActivityOnlineAppointmentBinding;
import com.ecads.pulse.android.dto.ChannelNameAndTokenDto;
import com.ecads.pulse.android.dto.TeleMedTokenReqDto;
import com.ecads.pulse.android.fragment.VideoCallFragment;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.network.ChannelingApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.EmergencyActions;
import com.ecads.pulse.android.utils.Permissions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class OnlineAppointmentActivity extends AppCompatActivity {

    private ActivityOnlineAppointmentBinding binding;
    private ActivityResultLauncher<String[]> launcher;
    private List<String> permissionsList;
    private List<String> deniedPermissionList;
    private String appointmentId;
    private Retrofit retrofit;
    private FirebaseAuth firebaseAuth;
    private EmergencyActions emergencyActions;

    private String consultantName;
    private String session;
    private String hospital;
    private String date;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOnlineAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        emergencyActions = new EmergencyActions(this);

        appointmentId = getIntent().getStringExtra("appointmentId");
        consultantName = getIntent().getStringExtra("name");
        session = getIntent().getStringExtra("session");
        hospital = getIntent().getStringExtra("hospital");
        date = getIntent().getStringExtra("date");

        if (appointmentId == null || appointmentId.isEmpty()) {
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        permissionsList = new ArrayList<>();
        deniedPermissionList = new ArrayList<>();

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (result != null) {
                        for (String permission : permissionsList) {
                            boolean granted = result.getOrDefault(permission, false);
                            if (!granted) {
                                deniedPermissionList.add(permission);
                            }
                        }
                        if (!deniedPermissionList.isEmpty()) {
                            new AlertDialog.Builder(this)
                                    .setTitle("Unable to proceed with the video consultation")
                                    .setMessage("Please enable the required permissions to enable video consultations.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            checkAndRequestPermissions();
                                        }
                                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            OnlineAppointmentActivity.this.finish();
                                        }
                                    })
                                    .show();
                        } else {
                            retrieveToken(instantiated -> {
                                if (instantiated) {
                                    retrieveChannelAndToken();
                                }
                            });
                        }
                    }
                });

        binding.upComingSessionPreview.setVisibility(View.GONE);
        binding.onlineAppointmentContainer.setVisibility(View.VISIBLE);

        checkAndRequestPermissions();

        binding.onlineAppointmentBtnEmergency.setOnClickListener(view -> {
            if (Permissions.permissionEnabled(this, Manifest.permission.CALL_PHONE) &&
                    Permissions.permissionEnabled(this, Manifest.permission.SEND_SMS)) {
                emergencyActions.makeCall();
                emergencyActions.sendSMS();
            } else {
                Toast.makeText(this, "Permissions are denied", Toast.LENGTH_SHORT).show();
            }
        });

        binding.onlineAppointmentBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void checkAndRequestPermissions() {
        permissionsList.clear();
        deniedPermissionList.clear();

        permissionsList.add(Manifest.permission.CAMERA);
        permissionsList.add(Manifest.permission.RECORD_AUDIO);
        permissionsList.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        permissionsList.add(Manifest.permission.ACCESS_WIFI_STATE);
        permissionsList.add(Manifest.permission.ACCESS_NETWORK_STATE);
        permissionsList.add(Manifest.permission.BLUETOOTH);
        permissionsList.add(Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            permissionsList.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissionsList.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        String[] permissionsArray = permissionsList.toArray(new String[permissionsList.size()]);
        launcher.launch(permissionsArray);
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.onlineAppointmentContainer, fragment);
        transaction.commit();
    }

    private void retrieveChannelAndToken() {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        TeleMedTokenReqDto tokenReqDto = new TeleMedTokenReqDto(firebaseAuth.getUid(), appointmentId);
        Call<ChannelNameAndTokenDto> channelAndToken = apiService.retrieveTeleMedChannelAndToken(tokenReqDto);
        channelAndToken.enqueue(new Callback<ChannelNameAndTokenDto>() {
            @Override
            public void onResponse(Call<ChannelNameAndTokenDto> call, Response<ChannelNameAndTokenDto> response) {
                if (response.isSuccessful()) {
                    ChannelNameAndTokenDto tokenDto = response.body();
                    if (tokenDto != null && tokenDto.getChannelName() != null
                            && tokenDto.getChannelToken() != null) {

                        String substring = session.substring(0, 6);

                        Bundle bundle = new Bundle();
                        bundle.putString("channel", tokenDto.getChannelName());
                        bundle.putString("token", tokenDto.getChannelToken());
                        bundle.putString("consultantName", consultantName);
                        bundle.putString("appointmentId", appointmentId);
                        bundle.putString("slot", substring);

                        VideoCallFragment fragment = new VideoCallFragment();
                        fragment.setArguments(bundle);

                        loadFragment(fragment);
                    }else{
                        // not available yet
                        binding.upComingSessionPreview.setVisibility(View.VISIBLE);
                        binding.onlineAppointmentContainer.setVisibility(View.GONE);

                        binding.onlineAppointmentConsultantName.setText(consultantName);
                        binding.onlineAppointmentSlot.setText(session.toUpperCase());
                        binding.onlineAppointmentDate.setText(date);
                        binding.onlineAppointmentHospital.setText(hospital);
                    }
                } else {
                    Toast.makeText(OnlineAppointmentActivity.this, "An error occurred while fetching online appointment data", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }

            @Override
            public void onFailure(Call<ChannelNameAndTokenDto> call, Throwable throwable) {
                Toast.makeText(OnlineAppointmentActivity.this, "Appointment data cannot be fetched", Toast.LENGTH_SHORT).show();
                finish();
                return;
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
}