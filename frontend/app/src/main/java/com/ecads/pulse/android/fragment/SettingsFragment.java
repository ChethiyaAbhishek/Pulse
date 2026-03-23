package com.ecads.pulse.android.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ecads.pulse.android.activity.SettingsSubActivity;
import com.ecads.pulse.android.databinding.FragmentSettingsBinding;


public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.settingsProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToFragmentLoader(1);
            }
        });

        binding.settingsReports.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToFragmentLoader(2);
            }
        });

        binding.settingsPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToFragmentLoader(3);
            }
        });

        binding.settingsNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToFragmentLoader(4);
            }
        });
    }

    private void navigateToFragmentLoader(int fragmentNo) {
        Intent intent = new Intent(requireActivity(), SettingsSubActivity.class);
        intent.putExtra("fragment", fragmentNo);
        startActivity(intent);
    }
}