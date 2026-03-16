package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.ActivityAppointmentBinding;
import com.ecads.pulse.android.fragment.ConsultantAppointmentFragment;
import com.ecads.pulse.android.fragment.HospitalAppointmentFragment;
import com.ecads.pulse.android.utils.EmergencyActions;
import com.ecads.pulse.android.utils.Helper;
import com.ecads.pulse.android.utils.Permissions;

import java.util.Arrays;
import java.util.List;

public class AppointmentActivity extends AppCompatActivity {

    private ActivityAppointmentBinding binding;
    private ActivityResultLauncher<String[]> launcher;
    private EmergencyActions emergencyActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        emergencyActions = new EmergencyActions(this);

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                        boolean callingPermissionGranted = result.getOrDefault(Manifest.permission.CALL_PHONE, false);
                        boolean messagingPermissionGranted = result.getOrDefault(Manifest.permission.SEND_SMS, false);

                        if (callingPermissionGranted) {
                            emergencyActions.makeCall();
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("Emergency call forwarding failed!")
                                    .setMessage("Please enable permissions for calling to use this feature.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }

                        if (messagingPermissionGranted) {
                            emergencyActions.sendSMS();
                        } else {
                            Toast.makeText(this, "Emergency text message sending failed. Please enable the permissions.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        binding.appointmentBtnEmergency.setOnClickListener(view -> {
            if (Permissions.permissionEnabled(this, Manifest.permission.CALL_PHONE) &&
                    Permissions.permissionEnabled(this, Manifest.permission.SEND_SMS)) {
                emergencyActions.makeCall();
                emergencyActions.sendSMS();
            } else {
                launcher.launch(new String[]{
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS
                });
            }
        });

        binding.appointmentBtnBack.setOnClickListener(v -> {
            finish();
        });

        Intent intent = getIntent();

        if (intent == null) {
            finish();
            return;
        }

        String type = intent.getStringExtra("type");
        String data = intent.getStringExtra("data");
        String authToken = null;

        if(intent.hasExtra("authToken")){
            authToken = intent.getStringExtra("authToken");
        }

        if (type == null || data == null) {
            finish();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString("data", data);
        bundle.putString("authToken", authToken);

        if (type.equals(Helper.HOSPITAL)) {
            HospitalAppointmentFragment fragment = new HospitalAppointmentFragment();
            fragment.setArguments(bundle);
            loadFragment(fragment);
        }

        if (type.equals(Helper.CONSULTANT)) {
            ConsultantAppointmentFragment fragment = new ConsultantAppointmentFragment();
            fragment.setArguments(bundle);
            loadFragment(fragment);
        }

    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.appointmentContainer, fragment);
        transaction.commit();
    }

}