package com.ecads.pulse.android.fragment;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ecads.pulse.android.databinding.FragmentPermissionBinding;
import com.ecads.pulse.android.utils.Permissions;

public class PermissionFragment extends Fragment {

    private FragmentPermissionBinding binding;
    private ActivityResultLauncher<String[]> launcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                        boolean calling = result.getOrDefault(Manifest.permission.CALL_PHONE, false);
                        boolean phoneState = result.getOrDefault(Manifest.permission.READ_PHONE_STATE, false);
                        boolean messages = result.getOrDefault(Manifest.permission.SEND_SMS, false);
                        boolean location = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                        boolean coarseLocation = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                        boolean camera = result.getOrDefault(Manifest.permission.CAMERA, false);
                        boolean audio = result.getOrDefault(Manifest.permission.RECORD_AUDIO, false);
                        boolean notifications = result.getOrDefault(Manifest.permission.POST_NOTIFICATIONS, false);
                        boolean externalStorage = result.getOrDefault(Manifest.permission.READ_EXTERNAL_STORAGE, false);
                        boolean bluetoothConnect = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
                        boolean bluetoothScan = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);

                        updateUi();

                        if (!calling) {
                            binding.permissionCall.setChecked(false);
                        }

                        if (!phoneState) {
                            binding.permissionCallState.setChecked(false);
                        }

                        if (!messages) {
                            binding.permissionSms.setChecked(false);
                        }

                        if (!location || !coarseLocation) {
                            binding.permissionLocation.setChecked(false);
                        }

                        if (!camera) {
                            binding.permissionCamera.setChecked(false);
                        }

                        if (!audio) {
                            binding.permissionAudio.setChecked(false);
                        }

                        if (!notifications) {
                            binding.permissionNotification.setChecked(false);
                        }

                        if (!externalStorage) {
                            binding.permissionStorage.setChecked(false);
                        }

                        if (!bluetoothConnect && !bluetoothScan) {
                            binding.permissionBluetooth.setChecked(false);
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPermissionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateUi();

        binding.permissionCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.CALL_PHONE)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CALL_PHONE)) {
                        launcher.launch(new String[]{Manifest.permission.CALL_PHONE});
                    } else {
                        openAppSettings();
                    }
                }
            }
        });

        binding.permissionCallState.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.READ_PHONE_STATE)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.READ_PHONE_STATE)) {
                        launcher.launch(new String[]{Manifest.permission.READ_PHONE_STATE});
                    } else {
                        openAppSettings();
                    }
                }
            }
        });

        binding.permissionSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.SEND_SMS)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.SEND_SMS)) {
                        launcher.launch(new String[]{Manifest.permission.SEND_SMS});
                    } else {
                        openAppSettings();
                    }
                }
            }
        });

        binding.permissionLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                        || !Permissions.permissionEnabled(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                        launcher.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                    } else {
                        openAppSettings();
                    }
                }
            }
        });

        binding.permissionCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.CAMERA)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CAMERA)) {
                        launcher.launch(new String[]{Manifest.permission.CAMERA});
                    } else {
                        openAppSettings();
                    }
                }
            }
        });

        binding.permissionAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
                        launcher.launch(new String[]{Manifest.permission.RECORD_AUDIO});
                    } else {
                        openAppSettings();
                    }
                }
            }
        });

        binding.permissionBluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
                            || !Permissions.permissionEnabled(requireActivity(), Manifest.permission.BLUETOOTH_SCAN)) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)) {
                            launcher.launch(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN});
                        } else {
                            openAppSettings();
                        }
                    }
                }
            }
        });

        binding.permissionStorage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CALL_PHONE)) {
                            launcher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
                        } else {
                            openAppSettings();
                        }
                    }
                }
            }
        });

        binding.permissionNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (!Permissions.permissionEnabled(requireActivity(), Manifest.permission.POST_NOTIFICATIONS)) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), Manifest.permission.CALL_PHONE)) {
                            launcher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                        } else {
                            openAppSettings();
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUi();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void updateUi() {
        if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.CALL_PHONE)) {
            binding.permissionCall.setEnabled(false);
            binding.permissionCall.setChecked(true);
        } else {
            binding.permissionCall.setEnabled(true);
        }

        if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.READ_PHONE_STATE)) {
            binding.permissionCallState.setEnabled(false);
            binding.permissionCallState.setChecked(true);
        } else {
            binding.permissionCallState.setEnabled(true);
        }

        if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.SEND_SMS)) {
            binding.permissionSms.setEnabled(false);
            binding.permissionSms.setChecked(true);
        } else {
            binding.permissionSms.setEnabled(true);
        }

        if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                && Permissions.permissionEnabled(requireActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)) {
            binding.permissionLocation.setEnabled(false);
            binding.permissionLocation.setChecked(true);
        } else {
            binding.permissionLocation.setEnabled(true);
        }

        if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.CAMERA)) {
            binding.permissionCamera.setEnabled(false);
            binding.permissionCamera.setChecked(true);
        } else {
            binding.permissionCamera.setEnabled(true);
        }

        if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.RECORD_AUDIO)) {
            binding.permissionAudio.setEnabled(false);
            binding.permissionAudio.setChecked(true);
        } else {
            binding.permissionAudio.setEnabled(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.POST_NOTIFICATIONS)) {
                binding.permissionNotification.setEnabled(false);
                binding.permissionNotification.setChecked(true);
            } else {
                binding.permissionNotification.setEnabled(true);
            }
        } else {
            binding.permissionNotification.setEnabled(false);
            binding.permissionNotification.setChecked(true);

            if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                binding.permissionStorage.setEnabled(false);
                binding.permissionStorage.setChecked(true);
            } else {
                binding.permissionStorage.setEnabled(true);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (Permissions.permissionEnabled(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
                    && Permissions.permissionEnabled(requireActivity(), Manifest.permission.BLUETOOTH_SCAN)) {
                binding.permissionBluetooth.setEnabled(false);
                binding.permissionBluetooth.setChecked(true);
            } else {
                binding.permissionBluetooth.setEnabled(true);
            }
        } else {
            binding.permissionBluetooth.setEnabled(false);
        }
    }
}