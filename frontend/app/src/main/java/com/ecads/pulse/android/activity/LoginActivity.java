package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.ActivityLoginBinding;
import com.ecads.pulse.android.fragment.Reg03Fragment;
import com.ecads.pulse.android.network.AuthApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.Permissions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<String[]> launcher;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("LoginActivity", "onCreate: View created.");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();

        preferences = getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                        boolean callingPermissionGranted = result.getOrDefault(Manifest.permission.CALL_PHONE, false);
                        boolean messagingPermissionGranted = result.getOrDefault(Manifest.permission.SEND_SMS, false);

                        if (callingPermissionGranted) {
                            makeEmergencyCall();
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("Emergency call forwarding failed!")
                                    .setMessage("Please enable permissions for calling to use this feature.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        }

                        if (messagingPermissionGranted) {
                            sendSMS();
                        } else {
                            Toast.makeText(this, "Emergency text message sending failed. Please enable the permissions.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // redirects to reg
        binding.loginRegText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("LoginActivity", "onCreate: redirects to reg.");
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
                finish();
            }
        });

        // login process
        binding.signInBtn.setOnClickListener(view -> {
            Log.i("LoginActivity", "onCreate: Login process started.");

            String email = binding.loginEmailField.getText().toString();
            TextView emailError = binding.loginEmailError;
            String password = binding.loginPasswordField.getText().toString();
            TextView passwordError = binding.loginPasswordError;

            if (firebaseAuth.getCurrentUser() != null) {
                redirectToMain();
                return;
            }

            if (email.isEmpty()) {
                emailError.setVisibility(View.VISIBLE);
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError.setVisibility(View.VISIBLE);
                emailError.setText("* Enter a valid email address.");
                return;
            }

            if (password.isEmpty()) {
                passwordError.setVisibility(View.VISIBLE);
                return;
            }

            binding.signInBtn.setEnabled(false);
            binding.signInBtn.setAlpha(0.5f);

            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success
                                Log.i("Firebase Auth", "User logged in");

                                FirebaseUser currentUser = firebaseAuth.getCurrentUser();

                                if (currentUser != null) {
                                    currentUser.getIdToken(true)
                                            .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                                                @Override
                                                public void onSuccess(GetTokenResult getTokenResult) {
                                                    Log.i("Firebase Auth", "Firebase Token retrieved");
                                                    notifyServer(getTokenResult.getToken());
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e("Firebase Auth", "Token retrieval failed: " + e.getMessage());
                                                    binding.signInBtn.setEnabled(true);
                                                    binding.signInBtn.setAlpha(1f);
                                                }
                                            });
                                } else {
                                    // exception
                                    Log.e("Firebase Auth", "No firebase auth user");
                                    binding.signInBtn.setEnabled(true);
                                    binding.signInBtn.setAlpha(1f);
                                }

                            } else {
                                Log.w("Firebase Auth", "User login failed: " + task.getException().getMessage());

                                binding.signInBtn.setEnabled(true);
                                binding.signInBtn.setAlpha(1f);

                                String message = "An error occurred. Please try again later.";

                                if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                    message = "User login credentials does not match.";
                                }

                                if (task.getException() instanceof FirebaseAuthInvalidUserException) {
                                    message = "The user does not exists, please create an account";
                                }

                                if (task.getException() instanceof FirebaseNetworkException) {
                                    message = "Login failed due to a connection failure. Please try again.";
                                }

                                new AlertDialog.Builder(LoginActivity.this)
                                        .setTitle("Login Failed!")
                                        .setMessage(message)
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        }
                    });
        });

        // 1990 emergency call & send SMS
        binding.emergencyBtnLogin.setOnClickListener(view -> {
            if (Permissions.permissionEnabled(this, Manifest.permission.CALL_PHONE) &&
                    Permissions.permissionEnabled(this, Manifest.permission.SEND_SMS)) {
                // make call & SMS
                makeEmergencyCall();
                sendSMS();
            } else {
                // request and continue
                launcher.launch(new String[]{
                        Manifest.permission.CALL_PHONE,
                        Manifest.permission.SEND_SMS
                });
            }
        });

        // TODO: forgot password process
        binding.loginForgotPassword.setOnClickListener(view -> {
        });

    }

    private void notifyServer(String firebaseToken){
        Retrofit retrofit = RetrofitClient.getInstance(firebaseToken);
        AuthApiService apiService = retrofit.create(AuthApiService.class);

        Call<Void> userAuth = apiService.userAuth();
        userAuth.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.i("Firebase Auth", "Server: Response Received");

                if (response.isSuccessful()) {
                    Log.i("Firebase Auth", "Server: Success");
                    redirectToMain();
                } else {
                    Log.e("Firebase Auth", "Server: Failed " + response.code());
                    Toast.makeText(LoginActivity.this, "An error occurred. Please try again shortly.", Toast.LENGTH_SHORT).show();
                    binding.signInBtn.setEnabled(true);
                    binding.signInBtn.setAlpha(1f);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.e("Firebase Auth", "Server: Failed " + throwable.getMessage());
                binding.signInBtn.setEnabled(true);
                binding.signInBtn.setAlpha(1f);
            }
        });
    }

    private void makeEmergencyCall() {
        Intent callingIntent = new Intent(Intent.ACTION_CALL);
        callingIntent.setData(Uri.parse("tel:0740971467"));
        startActivity(callingIntent);
    }

    private void sendSMS() {
        // TODO: get current location and attach it
        String message = "Pulse: EMERGENCY! \n " + preferences.getString("fname", "") + " needs your help. \n Currect Location: {location}";
        List<String> contactList = Arrays.asList(preferences.getString("cgiverContact", null),
                preferences.getString("emergencyContact", null));

        SmsManager manager = SmsManager.getDefault();

        for (String contact : contactList) {
            if (contact != null) {
                manager.sendTextMessage(contact, null, message, null, null);
            }
        }
    }

    private void redirectToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}