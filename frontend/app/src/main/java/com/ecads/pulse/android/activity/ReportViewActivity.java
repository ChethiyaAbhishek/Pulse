package com.ecads.pulse.android.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.ecads.pulse.android.databinding.ActivityReportViewBinding;

public class ReportViewActivity extends AppCompatActivity {

    private ActivityReportViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityReportViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String uri = getIntent().getStringExtra("uri");

        if (uri == null) {
            Toast.makeText(this, "Image loading failed", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Glide.with(this)
                .load(uri)
                .into(binding.reportViewImage);

        binding.reportViewClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
                return;
            }
        });

    }
}