package com.ecads.pulse.android.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import com.ecads.pulse.android.BuildConfig;
import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.SplashActivity;
import com.ecads.pulse.android.adapter.AvailableSlotsAdapter;
import com.ecads.pulse.android.adapter.ConsultantBasedHospitalsAdapter;
import com.ecads.pulse.android.databinding.FragmentConsultantAppointmentBinding;
import com.ecads.pulse.android.dto.AppointmentStatusNotifyDto;
import com.ecads.pulse.android.dto.HospitalDto;
import com.ecads.pulse.android.dto.PayhereRequestDto;
import com.ecads.pulse.android.dto.PaymentDetailsDto;
import com.ecads.pulse.android.enums.PaymentStatus;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.model.Consultant;
import com.ecads.pulse.android.model.User;
import com.ecads.pulse.android.network.AppointmentApiService;
import com.ecads.pulse.android.network.ChannelingApiService;
import com.ecads.pulse.android.network.PaymentApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.Permissions;
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

public class ConsultantAppointmentFragment extends Fragment {

    private FragmentConsultantAppointmentBinding binding;
    private String consultantUid;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private Retrofit retrofit;
    private String authToken;
    private String publicUrl;
    private String[] days;
    private ActivityResultLauncher<String> launcher;
    private String selectedConsultantName;

    private HospitalDto selectedHospital;
    private String selectedAppointmentDate;
    private String selectedSlot;
    private String selectedAppointmentMode;
    private double totalAmountPayable;
    private String appointmentId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            consultantUid = getArguments().getString("data");
            retrofit = RetrofitClient.getInstance(getArguments().getString("authToken"));
            authToken = getArguments().getString("authToken");
        }

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        publicUrl = BuildConfig.PUBLIC_URL;

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (!isGranted) {
                        Toast.makeText(requireActivity(), "Permissions for notifications are not granted", Toast.LENGTH_SHORT).show();
                    }
                });

        checkAndRequestPermission();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentConsultantAppointmentBinding.inflate(inflater, container, false);
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

        days = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};

        updateCalendarView(null);

        binding.conAppointmentchipGroupMode.setOnCheckedStateChangeListener(new ChipGroup.OnCheckedStateChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull ChipGroup chipGroup, @NonNull List<Integer> list) {
                if (binding.conAppointmentChipOnsite.isChecked()) {
                    selectedAppointmentMode = "Onsite";
                    if (binding.conAppointmentModeError.getVisibility() == View.VISIBLE) {
                        binding.conAppointmentModeError.setVisibility(View.GONE);
                    }
                }

                if (binding.conAppointmentChipOnline.isChecked()) {
                    selectedAppointmentMode = "Online";
                    if (binding.conAppointmentModeError.getVisibility() == View.VISIBLE) {
                        binding.conAppointmentModeError.setVisibility(View.GONE);
                    }
                }
            }
        });

        updatePaymentSection(consultantUid, null);

        binding.conAppointmentPaymentBtn.setOnClickListener(v -> {
            proceedPayment();
        });
    }

    private void completeView(View view) {
        fetchConsultantDetails(consultant -> {
            binding.conAppointmentConsultantName.setText(consultant.getTitle()+" "+consultant.getName());
            this.selectedConsultantName = consultant.getTitle()+" "+consultant.getName();
            binding.conAppointmentConsultantSpec.setText(consultant.getSpeciality());
            binding.conAppointmentConsultantBasHos.setText(consultant.getBaseHospital());

            binding.conAppointmentSearchBtn.setOnClickListener(v -> {
                fetchChannellingHospitals(true, results -> {
                    updateHospitals(results);
                });
            });

            fetchChannellingHospitals(false, results -> {
                updateHospitals(results);
            });
        });
    }

    private void fetchConsultantDetails(CallBack<Consultant> callBack){
        if(consultantUid == null){
            requireActivity().finish();
            return;
        }
        firebaseFirestore.collection("consultants").document(consultantUid)
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot == null) {
                            Toast.makeText(requireActivity(), "Unable to fetch consultant details", Toast.LENGTH_SHORT).show();
                            requireActivity().finish();
                            return;
                        }
                        Consultant consultant = documentSnapshot.toObject(Consultant.class);
                        callBack.callBack(consultant);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("ConsultantAppointmentFragment", "onFailure: Firestore consultant details retrieval failed");
                    }
                });
    }

    private void fetchChannellingHospitals(boolean isSearch,
                                             CallBack<List<HospitalDto>> callBack) {

        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);
        Map<String, String> requestData = new HashMap<>();
        requestData.put("consultantUid", consultantUid);

        if (isSearch) {
            String hospitalSearch = binding.conAppointmentSearchField.getText().toString();
            if (!hospitalSearch.isEmpty()) {
                requestData.put("hospital", hospitalSearch);
            }
        }

        Call<List<HospitalDto>> results = apiService.channellingHospitals(requestData);
        results.enqueue(new Callback<List<HospitalDto>>() {
            @Override
            public void onResponse(Call<List<HospitalDto>> call, Response<List<HospitalDto>> response) {
                if (response.isSuccessful()) {
                    callBack.callBack(response.body());
                } else {
                    Toast.makeText(requireActivity(), "Hospitals fetching failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("ConsultantAppointmentFragment", "Hospitals fetching failed for the consultant: " + response);
                }
            }

            @Override
            public void onFailure(Call<List<HospitalDto>> call, Throwable throwable) {
                Log.e("ConsultantAppointmentFragment", "Hospitals fetching failed for the consultant: " + throwable);
            }
        });
    }

    private void updateCalendarView(String hospitalPlaceId) {
        CalendarView appointmentCalendar = binding.conAppointmentCalendar;

        Calendar calendar = Calendar.getInstance();
        Calendar defaultSelected = calendar;
        long today = calendar.getTimeInMillis();

        calendar.add(Calendar.MONTH, 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        long nextMonthLastDate = calendar.getTimeInMillis();

        appointmentCalendar.setMinDate(today);
        appointmentCalendar.setMaxDate(nextMonthLastDate);

        if (hospitalPlaceId != null && !hospitalPlaceId.isEmpty()) {

            List<String> availableDays = new ArrayList<>();
            fetchAvailableDays(consultantUid, hospitalPlaceId, days -> {
                availableDays.addAll(days);

                StringBuilder labelText = new StringBuilder("Available Days: ");

                for (String day : availableDays) {
                    if (!day.equalsIgnoreCase(availableDays.get(0))) {
                        labelText.append(", ");
                    }
                    labelText.append(day.toUpperCase());
                }

                binding.conAppointmentAvaiDays.setText(labelText.toString());
            });

            fetchChannellingDays(hospitalPlaceId, defaultSelected, slotList -> {
                updateAvailableSlots(slotList);
            });

            appointmentCalendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
                @Override
                public void onSelectedDayChange(@NonNull CalendarView calendarView, int year, int month, int date) {

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
                        fetchChannellingDays(hospitalPlaceId, selectedDate, slotList -> {
                            updateAvailableSlots(slotList);
                        });
                        if (binding.conAppointmentCalendarError.getVisibility() == View.VISIBLE) {
                            binding.conAppointmentCalendarError.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(requireActivity(), "Consultant unavailable date", Toast.LENGTH_SHORT).show();
                        updateAvailableSlots(new ArrayList<String>());
                    }
                }
            });
        } else {
            updateAvailableSlots(new ArrayList<String>());
            binding.conAppointmentAvaiDays.setText("Select a hospital");
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

    private void fetchChannellingDays(String hospitalPlaceId, Calendar selectedDate,
                                      CallBack<List<String>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", hospitalPlaceId);
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
                    Log.e("ConsultantAppointmentFragment", "Server Failure[fetchChannellingDays]: " + response);
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable throwable) {
                Log.e("ConsultantAppointmentFragment", "Server Failure[fetchChannellingDays]: " + throwable);
            }
        });
    }

    private void updateAvailableSlots(List<String> availableSlotList) {
        RecyclerView recyclerView = binding.conAppointmentSlotsRecycler;
        Chip slotsUnavailable = binding.conAppointmentSlotsUnavailableChip;

        if (availableSlotList.isEmpty()) {
            // no available slots
            recyclerView.setVisibility(View.GONE);
            slotsUnavailable.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            slotsUnavailable.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            AvailableSlotsAdapter adapter = new AvailableSlotsAdapter(availableSlotList, slot -> {
                this.selectedSlot = slot;
                if (binding.conAppointmentSlotError.getVisibility() == View.VISIBLE) {
                    binding.conAppointmentSlotError.setVisibility(View.GONE);
                }
            });

            recyclerView.setAdapter(adapter);
        }
    }

    private void updateHospitals(List<HospitalDto> results) {
        RecyclerView recyclerView = binding.conAppointmentHosRecycler;
        Chip docNotFound = binding.chipHosNotFound;

        if (results.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            docNotFound.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            docNotFound.setVisibility(View.GONE);

            recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            ConsultantBasedHospitalsAdapter adapter =
                    new ConsultantBasedHospitalsAdapter(results, hospital -> {
                        updateCalendarView(hospital.getPlaceId());
                        updatePaymentSection(consultantUid, hospital.getPlaceId());
                        this.selectedHospital = hospital;
                        if (binding.conAppointmentHosError.getVisibility() == View.VISIBLE) {
                            binding.conAppointmentHosError.setVisibility(View.GONE);
                        }
                    });

            recyclerView.setAdapter(adapter);
        }
    }

    private void updatePaymentSection(String consultantUid, String hospitalPlaceId) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        if (consultantUid == null || hospitalPlaceId == null) {
            binding.conAppointmentConsultantFee.setText("LKR 0.00");
            binding.conAppointmentHospitalFee.setText("LKR 0.00");
            binding.conAppointmentPlatformFee.setText("LKR 0.00");
            binding.conAppointmentTotalAmount.setText("LKR 0.00");
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

                    binding.conAppointmentConsultantFee.setText(consultantFee);
                    binding.conAppointmentHospitalFee.setText(hospitalFee);
                    binding.conAppointmentPlatformFee.setText(platformFee);
                    binding.conAppointmentTotalAmount.setText(totalAmount);
                } else {
                    Toast.makeText(requireActivity(), "Payment details fetching failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    Log.e("ConsultantAppointmentFragment", "Server Failure[updatePaymentSection]: " + response);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Double>> call, Throwable throwable) {
                Log.e("ConsultantAppointmentFragment", "Server Failure[updatePaymentSection]: " + throwable);
            }
        });
    }

    private void proceedPayment() {
        if (this.selectedHospital == null) {
            binding.conAppointmentHosError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.selectedAppointmentDate == null) {
            binding.conAppointmentCalendarError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.selectedSlot == null) {
            binding.conAppointmentSlotError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.selectedAppointmentMode == null) {
            binding.conAppointmentModeError.setVisibility(View.VISIBLE);
            return;
        }

        if (this.totalAmountPayable < 1) {
            binding.conAppointmentHosError.setVisibility(View.VISIBLE);
            Toast.makeText(requireActivity(), "Unable to fetch the total amount", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firebaseAuth.getUid() == null) {
            Toast.makeText(requireActivity(), "No user found. Please login", Toast.LENGTH_SHORT).show();
            return;
        }

        PaymentApiService apiService = retrofit.create(PaymentApiService.class);
        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", selectedHospital.getPlaceId());
        requestData.put("consultantUid", consultantUid);

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
                                                Log.e("ConsultantAppointmentFragment", "FIREBASE FIRESTORE[proceedPayment]: No user");
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
                                            Log.e("ConsultantAppointmentFragment", "FIREBASE FIRESTORE[proceedPayment]: No user");
                                        }
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(requireActivity(), "An error occurred while proceeding", Toast.LENGTH_SHORT).show();
                                        Log.e("ConsultantAppointmentFragment", "FIREBASE FIRESTORE[proceedPayment]: " + e);
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

    private void createPendingAppointment(CallBack<Map<String, String>> callBack) {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);
        Map<String, String> requestData = new HashMap<>();
        requestData.put("hospitalPlaceId", selectedHospital.getPlaceId());
        requestData.put("consultantUid", consultantUid);
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
                Log.e("ConsultantAppointmentFragment", "SERVER[createPendingAppointment]: {}", throwable);
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
                    Log.d("ConsultantAppointmentFragment", "Payhere [APPOINTMENT_CANCEL: SUCCESS ]: " + response.code());

                    // NOTIFICATION
                    checkAndRequestPermission();
                    sendNotification("Appointment Cancelled", "Your appointment for " +
                            selectedConsultantName
                            + " on " + selectedAppointmentDate + " has been cancelled.");
                } else {
                    Log.d("ConsultantAppointmentFragment", "Payhere [APPOINTMENT_CANCEL: FAILED ]: " + response.code());
                    Toast.makeText(requireActivity(), "Appointment cancel failed", Toast.LENGTH_SHORT).show();

                    // NOTIFICATION
                    checkAndRequestPermission();
                    sendNotification("Appointment Cancellation Failed", "Your appointment cancellation for " +
                            selectedConsultantName
                            + " on " + selectedAppointmentDate + " has been failed.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.e("ConsultantAppointmentFragment", "Payhere [APPOINTMENT_CANCEL: FAILED ]: " + throwable);
                Toast.makeText(requireActivity(), "Appointment cancel failed", Toast.LENGTH_SHORT).show();

                // NOTIFICATION
                checkAndRequestPermission();
                sendNotification("Appointment Cancellation Failed", "Your appointment cancellation for " +
                        selectedConsultantName
                        + " on " + selectedAppointmentDate + " has been failed.");
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
                    Log.d("ConsultantAppointmentFragment", "Payhere [APPOINTMENT_CONFIRMATION: SUCCESS ]: " + response.code());
                    Toast.makeText(requireActivity(), "Appointment confirmed", Toast.LENGTH_SHORT).show();

                    // NOTIFICATION
                    checkAndRequestPermission();
                    sendNotification("Appointment Confirmed", "Your appointment for " +
                            selectedConsultantName
                            + " on " + selectedAppointmentDate + " has been confirmed.");

                    BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
                    View view = getLayoutInflater()
                            .inflate(R.layout.appointment_confirmation_dialog, null);

                    TextView hospital = view.findViewById(R.id.appointmentConHospital);
                    hospital.setText(selectedHospital.getName());

                    TextView consultant = view.findViewById(R.id.appointmentConConsultantName);
                    consultant.setText(selectedConsultantName);

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
                            FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.fragmentContainer, new ChannelFragment());
                            transaction.commit();
                        }
                    });
                    dialog.show();

                } else {
                    Log.d("ConsultantAppointmentFragment", "Payhere [APPOINTMENT_CONFIRMATION: FAILED ]: " + response.code());
                    Toast.makeText(requireActivity(), "Appointment confirmation failed", Toast.LENGTH_SHORT).show();

                    // NOTIFICATION
                    checkAndRequestPermission();
                    sendNotification("Appointment Confirmation Failed", "Your appointment confirmation for " +
                            selectedConsultantName
                            + " on " + selectedAppointmentDate + " has been failed.");
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Log.e("ConsultantAppointmentFragment", "Payhere [APPOINTMENT_CONFIRMATION: FAILED ]: " + throwable);
                Toast.makeText(requireActivity(), "Appointment confirmation failed", Toast.LENGTH_SHORT).show();

                // NOTIFICATION
                checkAndRequestPermission();
                sendNotification("Appointment Confirmation Failed", "Your appointment confirmation for " +
                        selectedConsultantName
                        + " on " + selectedAppointmentDate + " has been failed.");
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

    private void sendNotification(String title, String msg) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireActivity(), SplashActivity.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireActivity());
        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
            int notificationId = 1;
            notificationManager.notify(notificationId, builder.build());
        } else {
            checkAndRequestPermission();
        }
    }

    private void checkAndRequestPermission() {
        if (!Permissions.permissionEnabled(requireContext(), Manifest.permission.POST_NOTIFICATIONS)) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }
}