package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
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
import com.ecads.pulse.android.databinding.ActivityOnlineAppointmentBinding;
import com.ecads.pulse.android.fragment.VideoCallFragment;
import com.ecads.pulse.android.utils.Permissions;

import java.util.ArrayList;
import java.util.List;

public class OnlineAppointmentActivity extends AppCompatActivity {

    private ActivityOnlineAppointmentBinding binding;
    private ActivityResultLauncher<String[]> launcher;
    private List<String> permissionsList;
    private List<String> deniedPermissionList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOnlineAppointmentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
                            loadFragment(new VideoCallFragment());
                        }
                    }
                });

        checkAndRequestPermissions();
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
}