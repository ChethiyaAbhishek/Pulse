package com.ecads.pulse.android.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.ecads.pulse.android.BuildConfig;
import com.ecads.pulse.android.R;
import com.ecads.pulse.android.adapter.AvailableSlotsAdapter;
import com.ecads.pulse.android.adapter.HospitalChannellingConsultantsAdapter;
import com.ecads.pulse.android.databinding.FragmentHospitalAppointmentBinding;
import com.ecads.pulse.android.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.android.dto.PayhereRequestDto;
import com.ecads.pulse.android.dto.PaymentDetailsDto;
import com.ecads.pulse.android.enums.ConsultantSpecialities;
import com.ecads.pulse.android.enums.PaymentStatus;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.model.Consultant;
import com.ecads.pulse.android.model.Hospital;
import com.ecads.pulse.android.model.User;
import com.ecads.pulse.android.network.AppointmentApiService;
import com.ecads.pulse.android.network.ChannelingApiService;
import com.ecads.pulse.android.network.PaymentApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class HospitalAppointmentFragment extends Fragment {

    private FragmentHospitalAppointmentBinding binding;
    private String placeId;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private Retrofit retrofit;
    private String authToken;
    private String[] options;
    private String[] days;
    private String publicUrl;
    private String hospitalName;

    private Consultant selectedConsultant;
    private String selectedAppointmentDate;
    private String selectedSlot;
    private String selectedAppointmentMode;
    private double totalAmountPayable;
    private String appointmentId;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            placeId = getArguments().getString("data");
            retrofit = RetrofitClient.getInstance(getArguments().getString("authToken"));
            authToken = getArguments().getString("authToken");
        }

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        publicUrl = BuildConfig.PUBLIC_URL;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHospitalAppointmentBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        retrieveToken(instantiated -> {
            if (instantiated) {
                completeView(view);
            }
        });

        options = new String[]{"Filter by consultant speciality", ConsultantSpecialities.NEUROLOGIST.toString(), ConsultantSpecialities.PHYSIOTHERAPIST.toString()};
        days = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        updateCalendarView(null);

        binding.hosAppointmentchipGroupMode.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup chipGroup, @NonNull List<Integer> list) {
                if (binding.hosAppointmentchipOnsite.isChecked()) {
                    selectedAppointmentMode = "Onsite";
                    if (binding.modeError.getVisibility() == View.VISIBLE) {
                        binding.modeError.setVisibility(View.GONE);
                    }
                }

                if (binding.hosAppointmentchipOnline.isChecked()) {
                    selectedAppointmentMode = "Online";
                    if (binding.modeError.getVisibility() == View.VISIBLE) {
                        binding.modeError.setVisibility(View.GONE);
                    }
                }
            }
        });

        updatePaymentSection(null, placeId);

        binding.hosAppointmentPaymentBtn.setOnClickListener(v -> {
            proceedPayment();
        });
    }

    private void completeView(View view) {
        fetchPlaceDetails(hospital -> {
            binding.hosAppointmentHospitalName.setText(hospital.getName());
            this.hospitalName = hospital.getName();
            binding.hosAppointmentAddress.setText(hospital.getAddress());

            Glide.with(this)
                    .load(hospital.getImageUrl())
                    .centerCrop()
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            binding.hosAppointmentImgContainer.setBackground(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                        }

                        @Override
                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                            super.onLoadFailed(errorDrawable);
                            binding.hosAppointmentImgContainer.setBackgroundResource(R.drawable.card_bg);
                        }
                    });

            AutoCompleteTextView searchFilter = binding.hosAppointmentSearchFilter;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(view.getContext(), android.R.layout.simple_dropdown_item_1line, options);
            searchFilter.setAdapter(adapter);
            searchFilter.setText(options[0], false);

            binding.hosAppointmentLocationBtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(hospital.getMapUrl()));
                v.getContext().startActivity(intent);
            });

            binding.hosAppointmentCallBtn.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + hospital.getContact()));
                v.getContext().startActivity(intent);
            });

            binding.hosAppointmentSearchBtn.setOnClickListener(v -> {
                fetchChannellingConsultants(hospital.getPlaceId(), true, results -> {
                    updateConsultants(results);
                });
            });

            fetchChannellingConsultants(hospital.getPlaceId(), false, results -> {
                updateConsultants(results);
            });
        });
    }

    private void proceedPayment() {
        if (this.selectedConsultant == null) {
            binding.consultantError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.selectedAppointmentDate == null) {
            binding.calendarError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.selectedSlot == null) {
            binding.slotError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.selectedAppointmentMode == null) {
            binding.modeError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.totalAmountPayable < 1) {
            binding.consultantError.setVisibility(View.VISIBLE);
            Toast.makeText(requireActivity(), "Unable to fetch the total amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseAuth.getUid() == null) {
            Toast.makeText(requireActivity(), "No user found. Please login", Toast.LENGTH_SHORT).show();
            return;
        }

        PaymentApiService apiService = retrofit.create(PaymentApiService.class);
        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", placeId);
        requestData.put("consultantUid", selectedConsultant.getUid());

        Call<PaymentDetailsDto> paymentDetails = apiService.channelingPaymentDetails(requestData);
        paymentDetails.enqueue(new Callback<PaymentDetailsDto>() {
            @Override
            public void onResponse(Call<PaymentDetailsDto> call, Response<PaymentDetailsDto> response) {
                if (response.isSuccessful()) {

                    if (response.body() == null) {
                        Toast.makeText(requireActivity(), "An error occurred while fetching data", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    createPendingAppointment(pendingAppointment -> {

                        if (pendingAppointment == null || pendingAppointment.isEmpty()) {
                            Toast.makeText(requireActivity(), "The slot has already booked", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        firebaseFirestore.collection("users").document(firebaseAuth.getUid()).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        if (documentSnapshot != null) {
                                            User user = documentSnapshot.toObject(User.class);

                                            if (user == null) {
                                                Toast.makeText(requireActivity(), "An error occurred while proceeding", Toast.LENGTH_SHORT).show();
                                                Log.e("HospitalAppointmentFragment", "FIREBASE FIRESTORE[proceedPayment]: No user");
                                                return;
                                            }

                                            appointmentId = pendingAppointment.get("appointmentId");

                                            PayhereRequestDto requestDto = new PayhereRequestDto(
                                                    response.body().getMerchantId(),
                                                    response.body().getTotal(),
                                                    pendingAppointment.get("appointmentId"),
                                                    pendingAppointment.get("description"),
                                                    user.getFname(),
                                                    user.getLname(),
                                                    user.getEmail(),
                                                    user.getMobile(),
                                                    "",
                                                    "",
                                                    "Sri Lanka"
                                            );

                                            proceedPayherePayment(requestDto);
                                        } else {
                                            Toast.makeText(requireActivity(), "An error occurred while proceeding", Toast.LENGTH_SHORT).show();
                                            Log.e("HospitalAppointmentFragment", "FIREBASE FIRESTORE[proceedPayment]: No user");
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(requireActivity(), "An error occurred while proceeding", Toast.LENGTH_SHORT).show();
                                        Log.e("HospitalAppointmentFragment", "FIREBASE FIRESTORE[proceedPayment]: " + e);
                                    }
                                });
                    });
                } else {
                    Toast.makeText(requireActivity(), "An error occurred while fetching data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaymentDetailsDto> call, Throwable throwable) {
                Toast.makeText(requireActivity(), "An error occurred while fetching data", Toast.LENGTH_SHORT).show();
                Log.e("HospitalAppointmentFragment", "SERVER[proceedPayment]: " + throwable);
            }
        });
    }

    private void proceedPayherePayment(PayhereRequestDto requestDto) {
        InitRequest req = new InitRequest();
        req.setSandBox(true);
        req.setMerchantId(requestDto.getMerchantId());

        req.setCurrency("LKR");
        req.setAmount(requestDto.getTotal());
        req.setOrderId(requestDto.getAppointmentId());
        req.setItemsDescription(requestDto.getDescription());

        req.getCustomer().setFirstName(requestDto.getCustomerFirstName());
        req.getCustomer().setLastName(requestDto.getCustomerLastName());
        req.getCustomer().setEmail(requestDto.getCustomerEmail());
        req.getCustomer().setPhone(requestDto.getCustomerPhone());

        req.getCustomer().getAddress().setAddress(requestDto.getCustomerAddress());
        req.getCustomer().getAddress().setCity(requestDto.getCustomerCity());
        req.getCustomer().getAddress().setCountry(requestDto.getCustomerCountry());

        //Optional Params
        req.setNotifyUrl(publicUrl + "/api/v1/payment/notifyAppointment");

        Intent intent = new Intent(requireContext(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

        payhereLauncher.launch(intent);
    }

    private void createPendingAppointment(CallBack<Map<String, String>> callBack) {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);
        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", placeId);
        requestData.put("consultantUid", selectedConsultant.getUid());
        requestData.put("appointmentDate", selectedAppointmentDate);
        requestData.put("appointmentSlot", selectedSlot);
        requestData.put("appointmentMode", selectedAppointmentMode);
        requestData.put("patientAuthToken", authToken);

        Call<Map<String, String>> pendingAppointment = apiService.pendingAppointment(requestData);
        pendingAppointment.enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                if (response.isSuccessful()) {
                    callBack.callBack(response.body());
                } else {
                    Toast.makeText(requireActivity(), "An error occurred while proceeding: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, String>> call, Throwable throwable) {
                Toast.makeText(requireActivity(), "An error occurred while proceeding", Toast.LENGTH_SHORT).show();
                Log.e("HospitalAppointmentFragment", "SERVER[createPendingAppointment]: {}", throwable);
            }
        });
    }

    private void updatePaymentSection(String consultantUid, String hospitalPlaceId) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        if (consultantUid == null || hospitalPlaceId == null) {
            binding.hosAppointmentConsultantFee.setText("LKR 0.00");
            binding.hosAppointmentHospitalFee.setText("LKR 0.00");
            binding.hosAppointmentPlatformFee.setText("LKR 0.00");
            binding.hosAppointmentTotalAmount.setText("LKR 0.00");
            return;
        }

        Map<String, String> requestData = new HashMap<>();
        requestData.put("consultantUid", consultantUid);
        requestData.put("hospitalPlaceId", hospitalPlaceId);

        Call<Map<String, Double>> results = apiService.pricingDetails(requestData);
        results.enqueue(new Callback<Map<String, Double>>() {
            @Override
            public void onResponse(Call<Map<String, Double>> call, Response<Map<String, Double>> response) {
                if (response.isSuccessful()) {
                    Map<String, Double> details = response.body();

                    StringBuilder consultantFee = new StringBuilder("LKR ");
                    StringBuilder hospitalFee = new StringBuilder("LKR ");
                    StringBuilder platformFee = new StringBuilder("LKR ");
                    StringBuilder totalAmount = new StringBuilder("LKR ");

                    if (details != null && !details.isEmpty()) {
                        double conFee = details.get("consultantFee");
                        double hosFee = details.get("hospitalFee");
                        double platFee = details.get("platformFee");

                        double total = conFee + hosFee + platFee;

                        consultantFee.append(conFee);
                        hospitalFee.append(hosFee);
                        platformFee.append(platFee);
                        totalAmount.append(total);
                        totalAmountPayable = total;

                    } else {
                        consultantFee.append("0.00");
                        hospitalFee.append("0.00");
                        platformFee.append("0.00");
                        totalAmount.append("0.00");
                    }

                    binding.hosAppointmentConsultantFee.setText(consultantFee);
                    binding.hosAppointmentHospitalFee.setText(hospitalFee);
                    binding.hosAppointmentPlatformFee.setText(platformFee);
                    binding.hosAppointmentTotalAmount.setText(totalAmount);
                } else {
                    Toast.makeText(requireActivity(), "Payment details fetching failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("HospitalAppointmentFragment", "Server Failure[updatePaymentSection]: " + response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable throwable) {
                Log.e("HospitalAppointmentFragment", "Server Failure[updatePaymentSection]: " + throwable);
            }
        });
    }

    private void updateCalendarView(String consultantUid) {
        CalendarView appointmentCalendar = binding.hosAppointmentCalendar;

        Calendar calendar = Calendar.getInstance();
        Calendar defaultSelected = calendar;
        long today = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        long nextMonthLastDate = calendar.getTimeInMillis();

        appointmentCalendar.setMinDate(today);
        appointmentCalendar.setMaxDate(nextMonthLastDate);

        if (consultantUid != null && !consultantUid.isEmpty()) {

            List<String> availableDays = new ArrayList<>();
            fetchAvailableDays(consultantUid, placeId, days -> {
                availableDays.addAll(days);

                StringBuilder labelText = new StringBuilder("Available Days: ");

                for (String day : availableDays) {
                    if (!day.equalsIgnoreCase(availableDays.get(0))) {
                        labelText.append(", ");
                    }
                    labelText.append(day.toUpperCase());
                }

                binding.hosAppointmentAvaiDays.setText(labelText.toString());
            });

            fetchChannellingDays(consultantUid, defaultSelected, slotList -> {
                updateAvailableSlots(slotList);
            });

            appointmentCalendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(@NonNull CalendarView calendarView, int year, int month, int date) {
//                    Toast.makeText(requireActivity(), date + "-" + (month + 1) + "-" + year, Toast.LENGTH_SHORT).show();

                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, date);
                    String dayName = days[(selectedDate.get(Calendar.DAY_OF_WEEK) - 1)];

                    boolean isAvailable = false;

                    if (!availableDays.isEmpty()) {
                        for (String day : availableDays) {
                            if (day.equalsIgnoreCase(dayName)) {
                                isAvailable = true;
                                break;
                            }
                        }
                    } else {
                        Toast.makeText(requireActivity(), "Consultant is currently unavailable", Toast.LENGTH_SHORT).show();
                    }

                    if (isAvailable) {
                        selectedAppointmentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate.getTime());
                        fetchChannellingDays(consultantUid, selectedDate, slotList -> {
                            updateAvailableSlots(slotList);
                        });
                        if (binding.calendarError.getVisibility() == View.VISIBLE) {
                            binding.calendarError.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(requireActivity(), "Consultant unavailable date", Toast.LENGTH_SHORT).show();
                        updateAvailableSlots(new ArrayList<String>());
                    }
                }
            });
        } else {
            updateAvailableSlots(new ArrayList<String>());
            binding.hosAppointmentAvaiDays.setText("Select a consultant");
        }
    }

    private void fetchAvailableDays(String consultantUid, String hospitalPlaceId,
                                    CallBack<List<String>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", hospitalPlaceId);
        requestData.put("consultantUid", consultantUid);

        Call<List<String>> results = apiService.consultantAvailableDays(requestData);
        results.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    callBack.callBack(response.body());
                } else {
                    Toast.makeText(requireActivity(), "Consultant available days fetching failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("HospitalAppointmentFragment", "Server Failure[fetchAvailableDays]: " + response);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {
                Log.e("HospitalAppointmentFragment", "Server Failure[fetchAvailableDays]: " + throwable);
            }
        });
    }

    private void updateAvailableSlots(List<String> availableSlotList) {
        RecyclerView recyclerView = binding.hosAppointmentSlotsRecycler;
        Chip slotsUnavailable = binding.slotsUnavailableChip;

        if (availableSlotList.isEmpty()) {
            // no available slots
            recyclerView.setVisibility(View.GONE);
            slotsUnavailable.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            slotsUnavailable.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            AvailableSlotsAdapter adapter = new AvailableSlotsAdapter(availableSlotList, slot -> {
//                Toast.makeText(requireActivity(), "Slot: " + slot, Toast.LENGTH_SHORT).show();
                this.selectedSlot = slot;
                if (binding.slotError.getVisibility() == View.VISIBLE) {
                    binding.slotError.setVisibility(View.GONE);
                }
            });

            recyclerView.setAdapter(adapter);
        }
    }

    private void fetchChannellingDays(String consultantUid, Calendar selectedDate,
                                      CallBack<List<String>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", placeId);
        requestData.put("consultantUid", consultantUid);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String formattedDate = sdf.format(selectedDate.getTime());

        requestData.put("selectedDate", formattedDate);

        Call<List<String>> results = apiService.channellingDates(requestData);
        results.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    callBack.callBack(response.body());
                } else {
                    Toast.makeText(requireActivity(), "Available slots fetching failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("HospitalAppointmentFragment", "Server Failure[fetchChannellingDays]: " + response);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {
                Log.e("HospitalAppointmentFragment", "Server Failure[fetchChannellingDays]: " + throwable);
            }
        });
    }

    private void updateConsultants(List<Consultant> results) {
        RecyclerView recyclerView = binding.hosAppointmentDocRecycler;
        Chip docNotFound = binding.chipDocNotFound;

        if (results.isEmpty()) {
            // consultants not found
            recyclerView.setVisibility(View.GONE);
            docNotFound.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            docNotFound.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            HospitalChannellingConsultantsAdapter adapter =
                    new HospitalChannellingConsultantsAdapter(results, consultant -> {
                        updateCalendarView(consultant.getUid());
                        updatePaymentSection(consultant.getUid(), placeId);
                        this.selectedConsultant = consultant;
                        if (binding.consultantError.getVisibility() == View.VISIBLE) {
                            binding.consultantError.setVisibility(View.GONE);
                        }
                    });

            recyclerView.setAdapter(adapter);
        }
    }

    private void fetchChannellingConsultants(String hospitalPlaceId, boolean isSearch,
                                             CallBack<List<Consultant>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);
        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", hospitalPlaceId);

        String searchFilter = binding.hosAppointmentSearchFilter.getText().toString();
        if (!searchFilter.isEmpty() && !searchFilter.equals(options[0])) {
            requestData.put("speciality", searchFilter);
        }

        if (isSearch) {
            String consultantSearch = binding.hosAppointmentSearchField.getText().toString();
            if (!consultantSearch.isEmpty()) {
                requestData.put("consultant", consultantSearch);
            }
        }

        Call<List<Consultant>> results = apiService.channellingConsultants(requestData);
        results.enqueue(new Callback<List<Consultant>>() {
            @Override
            public void onResponse(Call<List<Consultant>> call, Response<List<Consultant>> response) {
                if (response.isSuccessful()) {
                    callBack.callBack(response.body());
                } else {
                    Toast.makeText(requireActivity(), "Consultant details fetching failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("HospitalAppointmentFragment", "Server Failure[200]: " + response);
                }
            }

            @Override
            public void onFailure(Call<List<Consultant>> call, Throwable throwable) {
                Log.e("HospitalAppointmentFragment", "Server Failure[206]: " + throwable);
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
                            Log.i("Firebase Auth", "[retrieveToken] Firebase Token retrieved");
                            retrofit = RetrofitClient.getInstance(getTokenResult.getToken());
                            authToken = getTokenResult.getToken();
                            instantiated.callBack(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Firebase Auth", "[retrieveToken] Token retrieval failed: " + e.getMessage());
                            instantiated.callBack(false);
                        }
                    });
        } else {
            instantiated.callBack(false);
            Log.e("Firebase Auth", "[232] No firebase auth user");
            requireActivity().finish();
        }
    }

    private void fetchPlaceDetails(CallBack<Hospital> callBack) {
        firebaseFirestore.collection("hospitals").document(placeId)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot == null) {
                            fetchUsingPlacesApi(hospital -> {
                                callBack.callBack(hospital);
                            });
                            return;
                        }
                        Hospital hospital = documentSnapshot.toObject(Hospital.class);
                        callBack.callBack(hospital);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        fetchUsingPlacesApi(hospital -> {
                            callBack.callBack(hospital);
                        });
                    }
                });

    }

    private void fetchUsingPlacesApi(CallBack<Hospital> callBack) {
        //TODO: Fetch data from place api
        callBack.callBack(null);
    }

    private final ActivityResultLauncher<Intent> payhereLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), (result) -> {

                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        PHResponse<StatusResponse> response =
                                (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                        if (response != null && response.isSuccess()) {
                            StatusResponse statusResponse = response.getData();
                            Log.d("Payhere", "Response: " + statusResponse);
                            confirmAppointment();
                        } else {
                            //payment fails
                            cancelAppointment(PaymentStatus.FAILED);
                        }
                    }

                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    // payment canceled
                    cancelAppointment(PaymentStatus.CANCELLED);
                }
            });

    private void cancelAppointment(PaymentStatus paymentStatus) {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);

        AppointmentStatusNotifyDto cancelDto = new AppointmentStatusNotifyDto(appointmentId, paymentStatus);

        Call<Void> cancelled = apiService.cancelAppointment(cancelDto);
        cancelled.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("HospitalAppointmentFragment", "Payhere [APPOINTMENT_CANCEL: SUCCESS ]: " + response.code());
                } else {
                    Log.d("HospitalAppointmentFragment", "Payhere [APPOINTMENT_CANCEL: FAILED ]: " + response.code());
                    Toast.makeText(requireActivity(), "Appointment cancel failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.e("HospitalAppointmentFragment", "Payhere [APPOINTMENT_CANCEL: FAILED ]: " + throwable);
                Toast.makeText(requireActivity(), "Appointment cancel failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmAppointment() {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);

        AppointmentStatusNotifyDto notifyDto = new AppointmentStatusNotifyDto(appointmentId, PaymentStatus.PAID);

        Call<Void> confirmed = apiService.confirmAppointment(notifyDto);
        confirmed.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d("HospitalAppointmentFragment", "Payhere [APPOINTMENT_CONFIRMATION: SUCCESS ]: " + response.code());
                    Toast.makeText(requireActivity(), "Appointment confirmed", Toast.LENGTH_SHORT).show();

                    BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                    View view = getLayoutInflater()
                            .inflate(R.layout.appointment_confirmation_dialog, null);

                    TextView hospital = view.findViewById(R.id.appointmentConHospital);
                    hospital.setText(hospitalName);

                    TextView consultant = view.findViewById(R.id.appointmentConConsultantName);
                    consultant.setText(selectedConsultant.getTitle() + " " + selectedConsultant.getName());

                    TextView date = view.findViewById(R.id.appointmentConDate);
//                    date.setText(new SimpleDateFormat("MMM dd yyyy", Locale.US).format(selectedAppointmentDate));
                    date.setText(selectedAppointmentDate);

                    TextView time = view.findViewById(R.id.appointmentConTime);
                    time.setText(selectedSlot);

                    TextView mode = view.findViewById(R.id.appointmentConMode);
                    mode.setText(selectedAppointmentMode);

                    //TODO: map integration

                    dialog.setContentView(view);
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialogInterface) {
                            requireActivity().finish();
                        }
                    });
                    dialog.show();

                } else {
                    Log.d("HospitalAppointmentFragment", "Payhere [APPOINTMENT_CONFIRMATION: FAILED ]: " + response.code());
                    Toast.makeText(requireActivity(), "Appointment confirmation failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.e("HospitalAppointmentFragment", "Payhere [APPOINTMENT_CONFIRMATION: FAILED ]: " + throwable);
                Toast.makeText(requireActivity(), "Appointment confirmation failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}