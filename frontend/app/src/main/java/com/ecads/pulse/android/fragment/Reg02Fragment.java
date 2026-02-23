package com.ecads.pulse.android.fragment;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.LoginActivity;
import com.ecads.pulse.android.activity.RegistrationActivity;
import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.databinding.FragmentReg02Binding;
import com.ecads.pulse.android.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class Reg02Fragment extends Fragment {

    private FragmentReg02Binding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private Bundle dataBundle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            dataBundle = getArguments();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReg02Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.reg2BtnBack.setOnClickListener(v -> {
            goBack();
        });

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        });

        binding.createAccountBtn.setOnClickListener(v -> {
            String email = binding.emailField.getText().toString();
            TextView emailError = binding.emailError;
            String password = binding.passwordField.getText().toString();
            TextView passError = binding.passwordError;
            String conPassword = binding.confirmPasswordField.getText().toString();
            TextView conPassError = binding.conPasswordError;

            List<TextView> errorList = Arrays.asList(emailError, passError, conPassError);

            for (TextView error : errorList) {
                error.setVisibility(GONE);
            }

            if (email.isEmpty()) {
                emailError.setVisibility(VISIBLE);
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailError.setVisibility(VISIBLE);
                emailError.setText("* Enter a valid email address.");
                return;
            }

            if (password.isEmpty()) {
                passError.setVisibility(VISIBLE);
                return;
            }

            if (password.length() < 8) {
                passError.setVisibility(VISIBLE);
                passError.setText("* Password must contain minimum of 8 characters.");
                return;
            }

            if (conPassword.isEmpty()) {
                conPassError.setVisibility(VISIBLE);
                return;
            }

            if (!conPassword.equals(password)) {
                conPassError.setVisibility(VISIBLE);
                conPassError.setText("* Passwords must match.");
                return;
            }

            binding.createAccountBtn.setEnabled(false);
            registerUser(email, password);

        });

    }

    private void goBack() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void registerUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.i("Firebase Auth", "Reg successful");

                            // db save
                            String uid = task.getResult().getUser().getUid();

                            User user = User.builder()
                                    .uid(uid)
                                    .fname(dataBundle.getString("fname"))
                                    .lname(dataBundle.getString("lname"))
                                    .mobile(dataBundle.getString("mobile"))
                                    .dob(dataBundle.getString("dob"))
                                    .gender(dataBundle.getString("gender"))
                                    .email(email).build();

                            SharedPreferences preferences = requireActivity().getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean("registered", true);
                            editor.putString("uid", uid);
                            editor.putString("fname", dataBundle.getString("fname"));

                            firebaseFirestore.collection("users")
                                    .document(uid)
                                    .set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unused) {
                                            Log.i("Firebase Firestore", "Data stored");
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Log.w("Firebase Firestore", "Data saving failed");

                                            editor.putBoolean("tempDataAvailable", true);
                                            editor.putString("lname", dataBundle.getString("lname"));
                                            editor.putString("mobile", dataBundle.getString("mobile"));
                                            editor.putString("dob", dataBundle.getString("dob"));
                                            editor.putString("gender", dataBundle.getString("gender"));
                                            editor.putString("email", email);
                                        }
                                    });

                            editor.apply();

                            Bundle transferBundle = new Bundle();
                            transferBundle.putString("uid", uid);

                            Reg03Fragment fragment = new Reg03Fragment();
                            fragment.setArguments(transferBundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.regFragmentContainer, fragment)
                                    .commit();

                        } else {
                            // reg failure
                            Log.w("Firebase Auth", "Reg failure: " + task.getException());

                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {

                                // user exists
                                Log.w("Firebase Auth", "Reg failure: User exists");

                                new AlertDialog.Builder(requireActivity())
                                        .setTitle("Registration Failed!")
                                        .setMessage("A user with the same email address already exists. Please login.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            }
                                        })
                                        .show();
                                return;
                            }

                            if (task.getException() instanceof FirebaseNetworkException) {

                                // network issue
                                Log.w("Firebase Auth", "Reg failure: Network issue");

                                new AlertDialog.Builder(requireActivity())
                                        .setTitle("Registration Failed!")
                                        .setMessage("Registration failed due to a network issue. Please check your connection and try again.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                binding.createAccountBtn.setEnabled(true);
                                            }
                                        })
                                        .show();
                                return;
                            }

                            new AlertDialog.Builder(requireActivity())
                                    .setTitle("Registration Failed!")
                                    .setMessage("An error occurred while registering.")
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            binding.createAccountBtn.setEnabled(true);
                                        }
                                    })
                                    .show();
                        }
                    }
                });
    }
}