package com.ecads.pulse.android.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ecads.pulse.android.databinding.ActivityLoginBinding;
import com.ecads.pulse.android.network.AuthApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.EmergencyActions;
import com.ecads.pulse.android.utils.Helper;
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
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<String[]> launcher;
    private EmergencyActions emergencyActions;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i("LoginActivity", "onCreate: View created.");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseAuth = FirebaseAuth.getInstance();
        emergencyActions = new EmergencyActions(this);
        preferences = getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);

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

        binding.loginRegText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("LoginActivity", "onCreate: redirects to reg.");
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
                finish();
            }
        });

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

                                                    if (preferences.getString("fcmToken", null) != null) {
                                                        Helper.updateFcmToken(firebaseAuth.getUid(), getTokenResult.getToken(), preferences.getString("fcmToken", ""));
                                                    } else {
                                                        FirebaseMessaging.getInstance().getToken()
                                                                .addOnSuccessListener(new OnSuccessListener<String>() {
                                                                    @Override
                                                                    public void onSuccess(String token) {
                                                                        Helper.updateFcmToken(
                                                                                firebaseAuth.getUid(),
                                                                                getTokenResult.getToken(),
                                                                                token);
                                                                    }
                                                                }).addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Log.e(LoginActivity.class.getSimpleName(), "onFailure: FCM token retrieving failed: " + e.getMessage() + " ");
                                                                    }
                                                                });
                                                    }
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

        binding.emergencyBtnLogin.setOnClickListener(view -> {
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


        binding.loginForgotPassword.setOnClickListener(view -> {

            String email = binding.loginEmailField.getText().toString();
            TextView emailError = binding.loginEmailError;

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

            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                Log.d("LoginActivity", "password reset email link sent");
                                Toast.makeText(LoginActivity.this, "Password reset link sent to email", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
        });

    }

    private void notifyServer(String firebaseToken) {
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

    private void redirectToMain() {
        startActivity(new Intent(LoginActivity.this, MainActivity.class));
        finish();
    }
}