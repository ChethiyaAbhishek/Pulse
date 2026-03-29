package com.ecads.pulse.android.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.AppointmentActivity;
import com.ecads.pulse.android.activity.HeartRateMonitoringActivity;
import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.adapter.AvailableConsultantsAdapter;
import com.ecads.pulse.android.adapter.ExistingAppointmentsAdapter;
import com.ecads.pulse.android.adapter.PrescriptionsAdapter;
import com.ecads.pulse.android.databinding.FragmentHomeBinding;
import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.dto.PrescriptionDto;
import com.ecads.pulse.android.dto.SingleRequestDataDto;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.model.AppointmentDto;
import com.ecads.pulse.android.network.ChannelingApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.Helper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Retrofit retrofit;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences preferences;
    private String firebaseToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = requireActivity().getSharedPreferences(SplashActivity.APP_DATA, MODE_PRIVATE);
        retrieveToken(instantiated -> {
            if (instantiated) {
                loadBody(binding.homeBody);
            }
        });
        checkLocalStorageAndNotify();

        if (preferences.getString("fname", null) != null) {
            binding.userNameHome.setText("Hi " + preferences.getString("fname", "") + "!");
        }else{
            binding.userNameHome.setText("Hello!");
        }
    }

    private void loadBody(LinearLayout layout) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        attachTodayAppointments(inflater, layout, loaded -> {
            if (loaded) {
                attachActivePrescriptions(inflater, layout);
                attachConsultantsSection(inflater, layout);
            }
        });

        binding.btnCallCaregiver.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String careGiverContact = preferences.getString("cgiverContact", null);
                if (careGiverContact != null && !careGiverContact.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + careGiverContact));
                    view.getContext().startActivity(intent);
                } else {
                    Toast.makeText(requireActivity(), "Caregivers contact has not provided: " + careGiverContact, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.btnCheckHeartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(requireActivity(), HeartRateMonitoringActivity.class);
                requireActivity().startActivity(intent);
            }
        });

//        String heartRateBpm = preferences.getString("heartRateBpm", "--");
//        String heartRateStatus = preferences.getString("heartRateStatus", "");
//
//        binding.heartRateBpm.setText(heartRateBpm + " bpm");
//        binding.heartRateStatus.setText(heartRateStatus);
    }

    private void attachTodayAppointments(LayoutInflater inflater, LinearLayout parent, CallBack<Boolean> callBack) {
        View section = inflater.inflate(R.layout.card_section, parent, false);
        TextView sectionTitle = section.findViewById(R.id.card_section_title);
        RecyclerView container = section.findViewById(R.id.card_section_container);
        LinearLayout extraContainer = section.findViewById(R.id.extraContainer);

        container.setVisibility(View.VISIBLE);
        extraContainer.setVisibility(View.GONE);

        sectionTitle.setText("Today Appointments");
        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        fetchTodayAppointments(results -> {

            if (results != null) {
                ExistingAppointmentsAdapter adapter = new ExistingAppointmentsAdapter(results, appointmentDto -> {
                });
                container.setAdapter(adapter);
            } else {
                View noAppointmentView = inflater.inflate(R.layout.no_appointment_view, parent, false);
                TextView text = noAppointmentView.findViewById(R.id.noAppViewText);
                text.setText("No appointments today.\nReady to book one?");
                MaterialButton bookNowBtn = noAppointmentView.findViewById(R.id.btnBookNow);
                bookNowBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.fragmentContainer, new ChannelFragment());
                        transaction.commit();
                    }
                });
                extraContainer.addView(noAppointmentView);

                container.setVisibility(View.GONE);
                extraContainer.setVisibility(View.VISIBLE);
            }
            parent.addView(section);

            callBack.callBack(true);
        });
    }

    private void fetchTodayAppointments(CallBack<List<AppointmentDto>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);
        Call<List<AppointmentDto>> appointments = apiService.retrieveTodayAppointments(new SingleRequestDataDto(firebaseAuth.getUid()));
        appointments.enqueue(new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(Call<List<AppointmentDto>> call, Response<List<AppointmentDto>> response) {
                if (response.isSuccessful()) {
                    List<AppointmentDto> appointmentDtos = response.body();
                    if (appointmentDtos != null && !appointmentDtos.isEmpty()) {
                        callBack.callBack(appointmentDtos);
                    } else {
                        Log.d("ChannelFragment", "No appointments scheduled");
                        callBack.callBack(null);
                    }
                } else {
                    Log.d("HomeFragment", "onResponse: Appointments fetching failed [" + response.code() + "]");
                    Toast.makeText(requireActivity(), "An error occurred while fetching appointments", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AppointmentDto>> call, Throwable throwable) {
                Log.d("HomeFragment", "Appointments fetching failed: " + throwable);
                Toast.makeText(requireActivity(), "An error occurred while fetching appointments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachActivePrescriptions(LayoutInflater inflater, LinearLayout parent) {
        View section = inflater.inflate(R.layout.card_section, parent, false);
        TextView sectionTitle = section.findViewById(R.id.card_section_title);
        RecyclerView container = section.findViewById(R.id.card_section_container);
        LinearLayout extraContainer = section.findViewById(R.id.extraContainer);

        container.setVisibility(View.VISIBLE);
        extraContainer.setVisibility(View.GONE);

        sectionTitle.setText("Active Prescriptions");
        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        firestore.collection("prescriptions")
                .whereEqualTo("patientId", "PA_" + firebaseAuth.getUid())
                .whereEqualTo("status", "ACTIVE")
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        boolean showNoPrescription = false;
                        if (!qds.isEmpty()) {
                            List<PrescriptionDto> prescriptionList = qds.toObjects(PrescriptionDto.class);
                            if (!prescriptionList.isEmpty()) {
                                PrescriptionsAdapter adapter = new PrescriptionsAdapter(prescriptionList, prescription -> {
                                    FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.fragmentContainer, new PrescriptionFragment());
                                    transaction.commit();
                                });
                                container.setAdapter(adapter);
                            } else {
                                showNoPrescription = true;
                            }
                        } else {
                            showNoPrescription = true;
                        }

                        if (showNoPrescription) {
                            View noPrescriptionView = inflater.inflate(R.layout.no_appointment_view, parent, false);
                            TextView text = noPrescriptionView.findViewById(R.id.noAppViewText);
                            text.setText("No active prescriptions yet. Book a doctor to get started.");
                            MaterialButton bookNowBtn = noPrescriptionView.findViewById(R.id.btnBookNow);
                            bookNowBtn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                                    transaction.replace(R.id.fragmentContainer, new ChannelFragment());
                                    transaction.commit();
                                }
                            });
                            extraContainer.addView(noPrescriptionView);

                            container.setVisibility(View.GONE);
                            extraContainer.setVisibility(View.VISIBLE);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireActivity(), "Prescriptions loading failed", Toast.LENGTH_SHORT).show();
                        Log.e("PrescriptionFragment", "Prescriptions loading failed: " + e);
                    }
                });
        parent.addView(section);
    }

    private void attachConsultantsSection(LayoutInflater inflater, LinearLayout parent) {
        View section = inflater.inflate(R.layout.card_section, parent, false);
        TextView sectionTitle = section.findViewById(R.id.card_section_title);
        RecyclerView container = section.findViewById(R.id.card_section_container);
        Chip notFound = section.findViewById(R.id.cardSectionResultNotFound);

        container.setVisibility(View.VISIBLE);
        notFound.setVisibility(View.GONE);

        sectionTitle.setText("Suggested Consultants");
        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        fetchConsultants(results -> {
            if (results != null && !results.isEmpty()) {
                AvailableConsultantsAdapter adapter = new AvailableConsultantsAdapter(results, availableConsultantDto -> {
                    Intent intent = new Intent(requireActivity(), AppointmentActivity.class);
                    intent.putExtra("type", Helper.CONSULTANT);
                    intent.putExtra("data", availableConsultantDto.getConsultant().getUid());
                    if (firebaseToken != null) {
                        intent.putExtra("authToken", firebaseToken);
                    }
                    startActivity(intent);
                });

                container.setAdapter(adapter);
            } else {
                // no consultant card
            }
            parent.addView(section);
        });
    }

    private void fetchConsultants(CallBack<List<AvailableConsultantDto>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);
        Call<List<AvailableConsultantDto>> recentList = apiService.recentlyBookedConsultants(new SingleRequestDataDto(firebaseAuth.getUid()));
        recentList.enqueue(new Callback<List<AvailableConsultantDto>>() {
            @Override
            public void onResponse(Call<List<AvailableConsultantDto>> call, Response<List<AvailableConsultantDto>> response) {
                if (response.isSuccessful()) {
                    List<AvailableConsultantDto> consultantDtoList = response.body();

                    if (consultantDtoList != null && !consultantDtoList.isEmpty()) {
                        callBack.callBack(consultantDtoList);
                    }

                } else {
                    Log.d("HomeFragment", "onResponse: recently booked consultants fetching failed [" + response.code() + "]");
                    Toast.makeText(requireActivity(), "An error occurred while fetching recently booked consultants", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AvailableConsultantDto>> call, Throwable throwable) {
                Log.d("HomeFragment", "Recently booked consultants fetching failed: " + throwable);
                Toast.makeText(requireActivity(), "An error occurred while fetching recently booked consultants", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void retrieveToken(CallBack<Boolean> instantiated) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Log.i("Firebase Auth", "Firebase Token retrieved");
                            retrofit = RetrofitClient.getInstance(getTokenResult.getToken());
                            instantiated.callBack(true);
                            firebaseToken = getTokenResult.getToken();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Firebase Auth", "Token retrieval failed: " + e.getMessage());
                            instantiated.callBack(false);
                        }
                    });
        } else {
            // exception
            Log.e("Firebase Auth", "No firebase auth user");
        }
    }

    private void checkLocalStorageAndNotify() {
        String emergencyContact = preferences.getString("emergencyContact", null);
        String careGiverContact = preferences.getString("cgiverContact", null);
        if (emergencyContact == null || emergencyContact.isEmpty()
                || careGiverContact == null || careGiverContact.isEmpty()) {
            binding.profileIncompleteView.setVisibility(View.VISIBLE);
        } else {
            binding.profileIncompleteView.setVisibility(View.GONE);
        }
    }
}