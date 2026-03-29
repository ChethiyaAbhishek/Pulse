package com.ecads.pulse.android.fragment;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.databinding.FragmentProfileBinding;
import com.ecads.pulse.android.model.Patient;
import com.ecads.pulse.android.model.User;
import com.ecads.pulse.android.utils.CalendarView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private String mobileValidationRegex;
    private SharedPreferences preferences;
    private String name;

    private Map<String, String> existingData;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        mobileValidationRegex = "^(?:\\+94|0)?7[0124578]\\d{7}$";
        existingData = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferences = requireActivity().getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);

        //calendar view for stroke date
        EditText dateOfStrokeField = binding.profileDosField;
        dateOfStrokeField.setOnClickListener(v -> {
            CalendarView.setCalendarView(v.getContext(), dateOfStrokeField, false);
        });

        setUserData();

        binding.profileChangesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateData();
            }
        });

        binding.profileDosField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.profileDosError.setVisibility(GONE);
            }
        });

        binding.profileCfnField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.profileCfnError.setVisibility(GONE);
            }
        });

        binding.profileClnField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.profileClnError.setVisibility(GONE);
            }
        });

        binding.profileCconField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.profileCconError.setVisibility(GONE);
            }
        });

        binding.profileEcField.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable editable) {

            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                binding.profileEcError.setVisibility(GONE);
            }
        });

    }

    private void setUserData() {

        String uid = firebaseAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("users").document(uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        User user = documentSnapshot.toObject(User.class);

                        if (user != null) {
                            binding.profileHeaderName.setText(user.getFname() + " " + user.getLname());
                            binding.profileHeaderEmail.setText(user.getEmail());
                            name = user.getFname();
                        }else{
                            name = "";
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showErrorAlert();
                    }
                });


        firebaseFirestore.collection("patients").document("PA_" + uid).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {

                        Patient patient = documentSnapshot.toObject(Patient.class);

                        if (patient != null) {
                            binding.profileDosField.setText(patient.getStrokeDate());
                            existingData.put("dos", patient.getStrokeDate());
                            binding.profileCfnField.setText(patient.getCgiverFname());
                            existingData.put("cfn", patient.getCgiverFname());
                            binding.profileClnField.setText(patient.getCgiverLname());
                            existingData.put("cln", patient.getCgiverLname());
                            binding.profileCconField.setText(patient.getCgiverContact());
                            existingData.put("ccon", patient.getCgiverContact());
                            binding.profileEcField.setText(patient.getEmerContact());
                            existingData.put("econ", patient.getEmerContact());
                        } else {
                            binding.profileDosError.setVisibility(View.VISIBLE);
                            binding.profileCfnError.setVisibility(View.VISIBLE);
                            binding.profileClnError.setVisibility(View.VISIBLE);
                            binding.profileCconError.setVisibility(View.VISIBLE);
                            binding.profileEcError.setVisibility(View.VISIBLE);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showErrorAlert();
                    }
                });
    }

    private void updateData() {

        String dateOfStoke = binding.profileDosField.getText().toString();
        TextView dosError = binding.profileDosError;

        String cFname = binding.profileCfnField.getText().toString();
        TextView cfnError = binding.profileCfnError;

        String cLname = binding.profileClnField.getText().toString();
        TextView clnError = binding.profileClnError;

        String cContact = binding.profileCconField.getText().toString();
        TextView cconError = binding.profileCconError;

        String emerContact = binding.profileEcField.getText().toString();
        TextView emerConError = binding.profileEcError;

        List<TextView> errorList = Arrays.asList(dosError, cfnError, clnError, cconError, emerConError);

        for (TextView error : errorList) {
            error.setVisibility(GONE);
        }

        if (dateOfStoke.isEmpty()) {
            dosError.setVisibility(VISIBLE);
            return;
        }

        if (cFname.isEmpty()) {
            cfnError.setVisibility(VISIBLE);
            return;
        }

        if (cLname.isEmpty()) {
            clnError.setVisibility(VISIBLE);
            return;
        }

        if (cContact.isEmpty()) {
            cconError.setVisibility(VISIBLE);
            return;
        }

        if (!Patterns.PHONE.matcher(cContact).matches() ||
                !cContact.matches(mobileValidationRegex)) {
            cconError.setVisibility(VISIBLE);
            cconError.setText("Enter a valid contact number.");
            return;
        }

        if (emerContact.isEmpty()) {
            emerConError.setVisibility(VISIBLE);
            return;
        }

        if (!Patterns.PHONE.matcher(emerContact).matches() ||
                !emerContact.matches(mobileValidationRegex)) {
            emerConError.setVisibility(VISIBLE);
            emerConError.setText("Enter a valid contact number.");
            return;
        }

        if (existingData.isEmpty()) {
            Patient patient = new Patient();
            patient.setStrokeDate(dateOfStoke);
            patient.setCgiverFname(cFname);
            patient.setCgiverLname(cLname);
            patient.setCgiverContact(cContact);
            patient.setEmerContact(emerContact);

            firebaseFirestore.collection("patients")
                    .document("PA_" + firebaseAuth.getUid())
                    .set(patient).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(requireActivity(), "Profile details saved", Toast.LENGTH_SHORT).show();
                            loadFragment(new ProfileFragment());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireActivity(), "Profile details saving failed", Toast.LENGTH_SHORT).show();
                            Log.e("ProfileFragment", "Profile details saving failed: " + e.getMessage());
                        }
                    });
        } else {
            Map<String, Object> dataToUpdate = new HashMap<>();

            if (!dateOfStoke.equals(existingData.get("dos"))) {
                dataToUpdate.put("strokeDate", dateOfStoke);
            }

            if (!cFname.equals(existingData.get("cfn"))) {
                dataToUpdate.put("cgiverFname", cFname);
            }

            if (!cLname.equals(existingData.get("cln"))) {
                dataToUpdate.put("cgiverLname", cLname);
            }

            if (!cContact.equals(existingData.get("ccon"))) {
                dataToUpdate.put("cgiverContact", cContact);
            }

            if (!emerContact.equals(existingData.get("econ"))) {
                dataToUpdate.put("emerContact", emerContact);
            }

            firebaseFirestore.collection("patients")
                    .document("PA_" + firebaseAuth.getUid())
                    .update(dataToUpdate).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(requireActivity(), "Profile details updated", Toast.LENGTH_SHORT).show();
                            loadFragment(new ProfileFragment());
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(requireActivity(), "Profile details updating failed", Toast.LENGTH_SHORT).show();
                            Log.e("ProfileFragment", "Profile details updating failed: " + e.getMessage());
                        }
                    });
        }

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("cgiverContact", cContact);
        editor.putString("emergencyContact", emerContact);
        editor.putString("fname", name);
        editor.apply();
    }

    private void showErrorAlert() {
        new AlertDialog.Builder(requireActivity())
                .setTitle("Details fetching failed!")
                .setMessage("An error occurred while fetching user data.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragmentContainer, new HomeFragment())
                                .commit();
                    }
                })
                .show();
    }

    private void loadFragment(Fragment fragment) {
        if(requireActivity().findViewById(R.id.fragmentContainer) != null){
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
        }
    }
}