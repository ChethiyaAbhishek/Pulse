package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
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
import com.ecads.pulse.android.databinding.ActivitySettingsSubBinding;
import com.ecads.pulse.android.fragment.ProfileFragment;
import com.ecads.pulse.android.fragment.ReportsFragment;
import com.ecads.pulse.android.utils.EmergencyActions;
import com.ecads.pulse.android.utils.Permissions;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsSubActivity extends AppCompatActivity {

    private ActivitySettingsSubBinding binding;
    private EmergencyActions emergencyActions;
    private ActivityResultLauncher<String[]> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsSubBinding.inflate(getLayoutInflater());
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

        int fragmentNo = getIntent().getIntExtra("fragment", 0);

        if (fragmentNo == 0) {
            finish();
            return;
        }

        if (fragmentNo == 1) {
            loadFragment(new ProfileFragment());
        }

        if (fragmentNo == 2) {
            loadFragment(new ReportsFragment());
        }

        if (fragmentNo == 3) {
            //TODO: LOAD PERMISSIONS
        }

        if (fragmentNo == 4) {
            //TODO: LOAD NOTIFICATIONS
        }

        binding.settingsSubBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

        binding.settingsSubBtnEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Permissions.permissionEnabled(SettingsSubActivity.this, Manifest.permission.CALL_PHONE) &&
                        Permissions.permissionEnabled(SettingsSubActivity.this, Manifest.permission.SEND_SMS)) {
                    emergencyActions.makeCall();
                    emergencyActions.sendSMS();
                } else {
                    launcher.launch(new String[]{
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.SEND_SMS
                    });
                }
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.settingsSubFragment, fragment);
        transaction.commit();
    }
}