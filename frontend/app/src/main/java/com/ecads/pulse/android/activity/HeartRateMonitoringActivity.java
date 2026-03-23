package com.ecads.pulse.android.activity;

import android.Manifest;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.annotation.NonNull;

import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.ecads.pulse.android.databinding.ActivityHeartRateMonitoringBinding;
import com.ecads.pulse.android.dto.PpgDto;
import com.ecads.pulse.android.utils.Permissions;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;

public class HeartRateMonitoringActivity extends AppCompatActivity {

    private ActivityHeartRateMonitoringBinding binding;
    private ActivityResultLauncher<String[]> launcher;
    private ProcessCameraProvider cameraProvider;
    private boolean isCameraOn;
    private List<PpgDto> greenBuffer;
    private long lastPeakTime = 0;
    private int peakCount = 0;
    private double currentBPM = 0;
    private ExecutorService cameraExecutor;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHeartRateMonitoringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        greenBuffer = new ArrayList<>();
        cameraExecutor = Executors.newSingleThreadExecutor();
        isCameraOn = false;
        preferences = getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                        boolean callingPermissionGranted = result.getOrDefault(Manifest.permission.CALL_PHONE, false);
                        boolean messagingPermissionGranted = result.getOrDefault(Manifest.permission.SEND_SMS, false);
                        boolean cameraPermissionGranted = result.getOrDefault(Manifest.permission.CAMERA, false);
                    }
                });

        checkAndRequestPermission();
        binding.hrmProgress.setProgress(0);

        binding.hrmMeasurementBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!isCameraOn) {
                    if (Permissions.permissionEnabled(HeartRateMonitoringActivity.this, Manifest.permission.CAMERA)) {
                        startCamera();
                    } else {
                        checkAndRequestPermission();
                    }
                } else {
                    binding.hrmMeasurementBtn.setEnabled(false);
                    binding.hrmMeasurementBtn.setAlpha(0.5f);
                }
            }
        });

    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        binding.hrmMeasurementBtn.setEnabled(false);
        binding.hrmMeasurementBtn.setAlpha(0.5f);
        binding.hrmMeasurementBtn.setText("Measuring...");

        binding.hrmProgress.setProgress(0);

        isCameraOn = true;

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e(HeartRateMonitoringActivity.class.getSimpleName(), "startCamera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        runOnUiThread(() -> binding.hrmProgress.setProgress(20));

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new android.util.Size(640, 480))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::processFrame);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            var camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
            camera.getCameraControl().enableTorch(true);
        } catch (Exception e) {
            Log.e(HeartRateMonitoringActivity.class.getSimpleName(), "bindCameraUseCases: ", e);
        }
    }

    private void processFrame(@NonNull ImageProxy image) {
        runOnUiThread(() -> binding.hrmProgress.setProgress(40));

        double greenIntensity = extractGreenIntensity(image);
        long timestamp = System.currentTimeMillis();

        greenBuffer.add(new PpgDto(timestamp, greenIntensity));

        if (greenBuffer.size() > 300) {
            greenBuffer.remove(0);
        }

        detectPeaksAndCalculateBPM();

        image.close();
    }

    private double extractGreenIntensity(@NonNull ImageProxy image) {
        runOnUiThread(() -> binding.hrmProgress.setProgress(60));

        androidx.camera.core.ImageProxy.PlaneProxy[] planes = image.getPlanes();

        java.nio.ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int width = image.getWidth();
        int height = image.getHeight();

        long sum = 0;
        int count = 0;

        for (int y = 0; y < height; y += 2) {
            for (int x = 0; x < width; x += 2) {
                int offset = y * rowStride + x * pixelStride;
                if (offset + 1 < buffer.capacity()) {
                    int green = buffer.get(offset + 1) & 0xFF;
                    sum += green;
                    count++;
                }
            }
        }

        return count > 0 ? (double) sum / count : 0.0;
    }

    private void detectPeaksAndCalculateBPM() {
        if (greenBuffer.size() < 60) return;

        double threshold = calculateDynamicThreshold();
        long currentTime = System.currentTimeMillis();

        for (int i = 1; i < greenBuffer.size() - 1; i++) {
            PpgDto prev = greenBuffer.get(i - 1);
            PpgDto curr = greenBuffer.get(i);
            PpgDto next = greenBuffer.get(i + 1);

            if (curr.getIntensity() > threshold &&
                    curr.getIntensity() > prev.getIntensity() &&
                    curr.getIntensity() > next.getIntensity()) {

                if (currentTime - lastPeakTime > 300) {
                    peakCount++;
                    lastPeakTime = currentTime;
                }
            }
        }

        if (peakCount >= 15) {
            long timeSpan = greenBuffer.get(greenBuffer.size() - 1).getTimestamp()
                    - greenBuffer.get(0).getTimestamp();

            if (timeSpan > 0) {
                currentBPM = (peakCount * 60000.0) / timeSpan;
                currentBPM = Math.max(40, Math.min(160, currentBPM));
                stopCamera();
            }
        }

        String status = currentBPM > 100 ? "Elevated" : "Normal";

        this.runOnUiThread(() -> {
            runOnUiThread(() -> binding.hrmProgress.setProgress(100));
            binding.hrmBpmValue.setText(String.format("%.0f", currentBPM));

            if (currentBPM > 100) {
                binding.hrmStatus.setText("Elevated");
            } else if (currentBPM > 80) {
                binding.hrmStatus.setText("Normal");
            } else {
                binding.hrmStatus.setText("Normal");
            }
        });
    }

    private double calculateDynamicThreshold() {
        runOnUiThread(() -> binding.hrmProgress.setProgress(80));

        double sum = 0;
        for (PpgDto p : greenBuffer) {
            sum += p.getIntensity();
        }
        return sum / greenBuffer.size() * 1.12;   // 12% above mean
    }

    private void stopCamera() {
        isCameraOn = false;

        this.runOnUiThread(() -> {
            try {
                if (cameraProvider != null) {
                    cameraProvider.unbindAll();
                }

                binding.hrmMeasurementBtn.setEnabled(true);
                binding.hrmMeasurementBtn.setAlpha(1.0f);
                binding.hrmMeasurementBtn.setText("Start Measurement");
                binding.previewView.setVisibility(View.GONE);

                greenBuffer.clear();
                peakCount = 0;
                lastPeakTime = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }
    }

    private void checkAndRequestPermission() {
        if (!Permissions.permissionEnabled(this, Manifest.permission.CALL_PHONE) ||
                !Permissions.permissionEnabled(this, Manifest.permission.SEND_SMS) ||
                !Permissions.permissionEnabled(this, Manifest.permission.CAMERA)) {
            launcher.launch(new String[]{
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CAMERA
            });
        }
    }
}