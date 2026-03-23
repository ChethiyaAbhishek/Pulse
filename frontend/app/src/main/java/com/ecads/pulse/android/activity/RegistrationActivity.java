package com.ecads.pulse.android.activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.fragment.Reg01Fragment;

public class RegistrationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

//        SharedPreferences preferences = getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);
//        if (preferences.getBoolean("tempDataAvailable", false)) {
//            loadFragment(new Reg03Fragment());
//        } else {
            loadFragment(new Reg01Fragment());
//        }
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.regFragmentContainer, fragment);
        transaction.commit();
    }
}