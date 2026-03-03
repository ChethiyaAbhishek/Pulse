package com.ecads.pulse.android.fragment;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.LoginActivity;
import com.ecads.pulse.android.activity.MainActivity;
import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.databinding.FragmentReg03Binding;
import com.ecads.pulse.android.model.Patient;
import com.ecads.pulse.android.model.User;
import com.ecads.pulse.android.utils.CalendarView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Reg03Fragment extends Fragment {

    private FragmentReg03Binding binding;
    private FirebaseFirestore firebaseFirestore;
    private SharedPreferences preferences;
    private String uid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseFirestore = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            uid = getArguments().getString("uid");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReg03Binding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = requireActivity().getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        String mobileValidationRegex = "^(?:\\+94|0)?7[0124578]\\d{7}$";

        // back btn
        binding.reg3BtnBack.setEnabled(false);
        binding.reg3BtnBack.setAlpha(0.5f);

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });

        //calendar view for stroke date
        EditText dateOfStrokeField = binding.dateOfStrokeField;
        dateOfStrokeField.setOnClickListener(v -> {
            CalendarView.setCalendarView(view.getContext(), dateOfStrokeField, false);
        });

        binding.regFinalizeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String strokeDate = dateOfStrokeField.getText().toString();
                TextView strokeDateError = binding.dosError;
                String cgiverFname = binding.cFnameField.getText().toString();
                TextView cgiverFnameError = binding.cfnError;
                String cgiverLname = binding.cLnameField.getText().toString();
                TextView cgiverLnameError = binding.clnError;
                String cgiverContact = binding.cContactField.getText().toString();
                TextView cgiverContactError = binding.cconError;
                String emContact = binding.eContactField.getText().toString();
                TextView emContactError = binding.econError;

                List<TextView> errorList = Arrays.asList(strokeDateError, cgiverFnameError, cgiverLnameError, cgiverContactError, emContactError);

                for (TextView error : errorList) {
                    error.setVisibility(GONE);
                }

                if (strokeDate.isEmpty()) {
                    strokeDateError.setVisibility(VISIBLE);
                    return;
                }

                if (cgiverFname.isEmpty()) {
                    cgiverFnameError.setVisibility(VISIBLE);
                    return;
                }

                if (cgiverLname.isEmpty()) {
                    cgiverLnameError.setVisibility(VISIBLE);
                    return;
                }

                if (cgiverContact.isEmpty()) {
                    cgiverContactError.setVisibility(VISIBLE);
                    return;
                }

                if (!Patterns.PHONE.matcher(cgiverContact).matches() ||
                        !cgiverContact.matches(mobileValidationRegex)) {
                    cgiverContactError.setVisibility(VISIBLE);
                    cgiverContactError.setText("Enter a valid mobile number.");
                    return;
                }

                if (emContact.isEmpty()) {
                    emContactError.setVisibility(VISIBLE);
                    return;
                }

                if (!Patterns.PHONE.matcher(emContact).matches() ||
                        !emContact.matches(mobileValidationRegex)) {
                    emContactError.setVisibility(VISIBLE);
                    emContactError.setText("Enter a valid mobile number.");
                    return;
                }

                checkAndSaveTempData(editor);

                Patient patient = Patient.builder()
                        .uid(uid)
                        .strokeDate(strokeDate)
                        .cgiverFname(cgiverFname)
                        .cgiverLname(cgiverLname)
                        .cgiverContact(cgiverContact)
                        .emerContact(emContact)
                        .build();

                firebaseFirestore.collection("patients")
                        .document("PA_" + uid)
                        .set(patient).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.i("Firebase Firestore", "Patient data stored");
                                editor.putBoolean("profileDetailsAvailable", true);

                                //for emergency purposes
                                editor.putString("cgiverContact", cgiverContact);
                                editor.putString("emergencyContact", emContact);

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("Firebase Firestore", "Data saving failed :" + e.getMessage());
                                new AlertDialog.Builder(requireActivity())
                                        .setTitle("Details saving failed!")
                                        .setMessage("An error occurred while saving.")
                                        .setPositiveButton("OK", null)
                                        .show();
                            }
                        });
                editor.apply();

                // TODO: backend api calling: store stroke date

                startActivity(new Intent(requireActivity(), MainActivity.class));
                requireActivity().finish();
            }
        });
    }

    private void checkAndSaveTempData(SharedPreferences.Editor editor) {
        if (preferences.getBoolean("tempDataAvailable", false)) {

            String uid = preferences.getString("uid", null);

            User user = User.builder()
                    .uid(uid)
                    .fname(preferences.getString("fname", null))
                    .lname(preferences.getString("lname", null))
                    .mobile(preferences.getString("mobile", null))
                    .dob(preferences.getString("dob", null))
                    .gender(preferences.getString("gender", null))
                    .email(preferences.getString("email", null))
                    .build();

            if (uid != null) {
                firebaseFirestore.collection("users")
                        .document(uid)
                        .set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.i("Firebase Firestore", "Data stored");

                                editor.remove("tempDataAvailable");
                                editor.remove("lname");
                                editor.remove("mobile");
                                editor.remove("dob");
                                editor.remove("gender");
                                editor.remove("email");
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("Firebase Firestore", "Data saving failed :" + e.getMessage());
                            }
                        });
            }
        }
    }
}