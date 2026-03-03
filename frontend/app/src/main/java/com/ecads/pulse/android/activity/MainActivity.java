package com.ecads.pulse.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.ActivityMainBinding;
import com.ecads.pulse.android.databinding.SideNavigationHeaderBinding;
import com.ecads.pulse.android.fragment.ChannelFragment;
import com.ecads.pulse.android.fragment.ProfileFragment;
import com.ecads.pulse.android.network.AuthApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

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
            //TODO: Load home fragment
//            loadFragment(new HomeFragment());
            bottomNavigationView.getMenu().findItem(R.id.bn_home).setChecked(true);
        }

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
            //TODO: Load home fragment
//            loadFragment(new ChannelFragment());
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
            // TODO: Load channel history fragment
//            loadFragment(new ChannelFragment());
            sideMenu.findItem(R.id.sn_channel_history).setChecked(true);

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