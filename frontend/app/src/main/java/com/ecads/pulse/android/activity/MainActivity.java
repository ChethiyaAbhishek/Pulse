package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.ActivityMainBinding;
import com.ecads.pulse.android.databinding.SideNavigationHeaderBinding;
import com.ecads.pulse.android.fragment.ChannelFragment;
import com.ecads.pulse.android.fragment.HomeFragment;
import com.ecads.pulse.android.fragment.ProfileFragment;
import com.ecads.pulse.android.model.Hospital;
import com.ecads.pulse.android.model.Patient;
import com.ecads.pulse.android.network.AuthApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.EmergencyActions;
import com.ecads.pulse.android.utils.Permissions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        NavigationBarView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private BottomNavigationView bottomNavigationView;
    private NavigationView sideNavigationView;
    private SideNavigationHeaderBinding sideHeaderBinding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar sideNavTopBar;

    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<String[]> launcher;
    private EmergencyActions emergencyActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
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

        bottomNavigationView = binding.bottomNavigationView;
        sideNavigationView = binding.sideNavigationView;
        sideHeaderBinding = SideNavigationHeaderBinding.bind(sideNavigationView.getHeaderView(0));
        drawerLayout = binding.drawerLayout;
        sideNavTopBar = binding.toolbar;

        setSupportActionBar(sideNavTopBar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                sideNavTopBar,
                R.string.drawer_open,
                R.string.drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toggle.setDrawerIndicatorEnabled(false);

        sideNavTopBar.setNavigationIcon(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setTitle(null);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });

        binding.sideMenu.setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        sideNavigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            bottomNavigationView.getMenu().findItem(R.id.bn_home).setChecked(true);
        }

        binding.emergencyBtnMain.setOnClickListener(view -> {
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

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        int itemId = menuItem.getItemId();

        Menu sideMenu = sideNavigationView.getMenu();
        Menu bottomMenu = bottomNavigationView.getMenu();

        for (int index = 0; index < sideMenu.size(); index++) {
            sideMenu.getItem(index).setChecked(false);
        }

        for (int index = 0; index < bottomMenu.size(); index++) {
            bottomMenu.getItem(index).setChecked(false);
        }

        if (itemId == R.id.bn_home) {
            loadFragment(new HomeFragment());
            bottomMenu.findItem(R.id.bn_home).setChecked(true);

        } else if (itemId == R.id.bn_exercise) {
            //TODO: Load exercise fragment
//            loadFragment(new ChannelFragment());
            bottomMenu.findItem(R.id.bn_exercise).setChecked(true);

        } else if (itemId == R.id.bn_channel) {
            loadFragment(new ChannelFragment());
            bottomMenu.findItem(R.id.bn_channel).setChecked(true);

        } else if (itemId == R.id.bn_progress) {
            //TODO: Load progress fragment
//            loadFragment(new ChannelFragment());
            bottomMenu.findItem(R.id.bn_progress).setChecked(true);

        } else if (itemId == R.id.bn_profile) {
            loadFragment(new ProfileFragment());
            bottomMenu.findItem(R.id.bn_profile).setChecked(true);

        } else if (itemId == R.id.sn_channel_history) {
            Intent intent = new Intent(MainActivity.this, PreviousAppointmentsActivity.class);
            startActivity(intent);
//            sideMenu.findItem(R.id.sn_channel_history).setChecked(true);

        } else if (itemId == R.id.sn_prescriptions) {
            //TODO: Load prescription fragment
//            loadFragment(new ChannelFragment());
            sideMenu.findItem(R.id.sn_prescriptions).setChecked(true);

        } else if (itemId == R.id.sn_settings) {
            //TODO: Load settings fragment
//            loadFragment(new ChannelFragment());
            sideMenu.findItem(R.id.sn_settings).setChecked(true);

        } else if (itemId == R.id.sn_logout) {
            sideMenu.findItem(R.id.sn_logout).setChecked(true);
            notifyServer();
        }
        return true;
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    private void notifyServer() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            currentUser.getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Log.i("Firebase Auth", "Firebase Token retrieved");

                            Retrofit retrofit = RetrofitClient.getInstance(getTokenResult.getToken());
                            AuthApiService apiService = retrofit.create(AuthApiService.class);

                            Call<Void> userAuth = apiService.userLogout();
                            userAuth.enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    Log.i("Firebase Auth", "Server: Response Received");

                                    if (response.isSuccessful()) {
                                        Log.i("Firebase Auth", "Server: Success");
                                        firebaseAuth.signOut();
                                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                                        finish();
                                    } else {
                                        Log.e("Firebase Auth", "Server: Failed " + response.code());
                                        Toast.makeText(MainActivity.this, "An error occurred. Please try again shortly.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(Call<Void> call, Throwable throwable) {
                                    Log.e("Firebase Auth", "Server: Failed " + throwable.getMessage());
                                }
                            });

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