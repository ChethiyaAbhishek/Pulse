package com.ecads.pulse.android.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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

import com.ecads.pulse.android.BuildConfig;
import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.AppointmentActivity;
import com.ecads.pulse.android.adapter.AvailableConsultantsAdapter;
import com.ecads.pulse.android.adapter.ExistingAppointmentsAdapter;
import com.ecads.pulse.android.adapter.HospitalAdapter;
import com.ecads.pulse.android.adapter.SearchResultsAdapter;
import com.ecads.pulse.android.databinding.FragmentChannelBinding;
import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.dto.HospitalDto;
import com.ecads.pulse.android.dto.LocationDto;
import com.ecads.pulse.android.dto.NearByResponseDto;
import com.ecads.pulse.android.dto.SearchItemDto;
import com.ecads.pulse.android.dto.SearchResultDto;
import com.ecads.pulse.android.dto.SingleRequestDataDto;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.model.AppointmentDto;
import com.ecads.pulse.android.model.Hospital;
import com.ecads.pulse.android.network.ChannelingApiService;
import com.ecads.pulse.android.network.MapApiRetrofitClient;
import com.ecads.pulse.android.network.MapApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.ecads.pulse.android.utils.Helper;
import com.ecads.pulse.android.utils.MapApi;
import com.ecads.pulse.android.utils.Permissions;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ChannelFragment extends Fragment {

    private FragmentChannelBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private GeoLocation currentLocation;
    private final int radiusInM;
    private ActivityResultLauncher<String[]> launcher;
    private FusedLocationProviderClient fusedLocationClient;
    private String firebaseToken;
    private Retrofit retrofit;
    private MapApi mapApi;
    private String mapApiKey;
    private List<HospitalDto> nearBySearchResults;

    public ChannelFragment() {
        //within 50km
        radiusInM = 50 * 1000;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        mapApi = new MapApi();
        mapApiKey = BuildConfig.MAPS_API_KEY;
        nearBySearchResults = new ArrayList<>();

        launcher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (result != null) {
                        boolean locationPermissionGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                        if (!locationPermissionGranted) {
                            currentLocation = null;
                            Toast.makeText(requireActivity(), "Please enable permissions for current location fetching for better experience.", Toast.LENGTH_SHORT).show();
                        } else {
                            checkAndRequest();
                        }
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentChannelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        checkAndRequest();
        binding.channelLoadingContainer.setVisibility(View.VISIBLE);
        binding.channelBodyContainer.setVisibility(View.GONE);
        retrieveToken(instantiated -> {
            if (instantiated) {
                loadBody(binding.channelBody);
            }
        });
    }

    private void checkAndRequest() {
        if (!Permissions.permissionEnabled(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)) {
            currentLocation = null;
            launcher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        } else {
            fetchCurrentLocation(geoLocation -> {
                currentLocation = geoLocation;
            });
        }
    }

    private void retrieveToken(CallBack<Boolean> instantiated) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Log.i("Firebase Auth", "Firebase Token retrieved");
                            firebaseToken = getTokenResult.getToken();
                            retrofit = RetrofitClient.getInstance(getTokenResult.getToken());
                            instantiated.callBack(true);
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

    private void loadBody(LinearLayout layout) {

        checkAndRequest();

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        getAppointmentList(inflater, layout, data -> {
            if (data) {
                attachHospitalSection(inflater, layout);
                attachAvailableConsultants(inflater, layout);
            }
        });

        binding.channelSearchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchText = binding.channelSearchField.getText().toString().trim();

                if (!searchText.isEmpty()) {
                    attachChannelSearchResults(searchText, inflater, layout);
                }
            }
        });

        binding.clearSearchChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragmentContainer, new ChannelFragment());
                transaction.commit();
            }
        });
    }

    private void attachAvailableConsultants(LayoutInflater inflater, LinearLayout parent) {
        View section = inflater.inflate(R.layout.card_section, parent, false);
        TextView sectionTitle = section.findViewById(R.id.card_section_title);
        RecyclerView container = section.findViewById(R.id.card_section_container);
        Chip notFound = section.findViewById(R.id.cardSectionResultNotFound);

        container.setVisibility(View.VISIBLE);
        notFound.setVisibility(View.GONE);

        sectionTitle.setText("Today Available Slots");
        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        retrieveHospitalList(results -> {
            List<HospitalDto> hospitals = results;

            fetchConsultantsWithAvailableSlotsForHospitals(hospitals, availableConsultantList -> {
                if (!availableConsultantList.isEmpty()) {

                    AvailableConsultantsAdapter adapter = new AvailableConsultantsAdapter(availableConsultantList, availableConsultantDto -> {
                        Intent intent = new Intent(requireActivity(), AppointmentActivity.class);
                        intent.putExtra("type", Helper.CONSULTANT);
                        intent.putExtra("data", availableConsultantDto.getConsultant().getUid());
                        if (firebaseToken != null) {
                            intent.putExtra("authToken", firebaseToken);
                        }
                        startActivity(intent);
                    });

                    container.setAdapter(adapter);
                    parent.addView(section);
                }
            });
        });

        binding.channelLoadingContainer.setVisibility(View.GONE);
        binding.channelBodyContainer.setVisibility(View.VISIBLE);
    }

    private void fetchConsultantsWithAvailableSlotsForHospitals(List<HospitalDto> hospitals, CallBack<List<AvailableConsultantDto>> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        Call<List<AvailableConsultantDto>> results = apiService.consultantsForHospitalList(hospitals);
        results.enqueue(new Callback<List<AvailableConsultantDto>>() {
            @Override
            public void onResponse(Call<List<AvailableConsultantDto>> call, Response<List<AvailableConsultantDto>> response) {
                if (response.isSuccessful()) {
                    callBack.callBack(response.body());
                } else {
                    Log.d("ChannelFragment", "Consultant available slots retrieving failed: " + response.message());
                    Toast.makeText(requireActivity(), "Available consultant slots retrieve failed: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<AvailableConsultantDto>> call, Throwable throwable) {
                Log.d("ChannelFragment", "Consultant available slots retrieving failed: " + throwable);
                Toast.makeText(requireActivity(), "Available consultant slots retrieve failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void attachHospitalSection(LayoutInflater inflater, LinearLayout parent) {
        View section = inflater.inflate(R.layout.card_section, parent, false);
        TextView sectionTitle = section.findViewById(R.id.card_section_title);
        RecyclerView container = section.findViewById(R.id.card_section_container);
        Chip notFound = section.findViewById(R.id.cardSectionResultNotFound);

        container.setVisibility(View.VISIBLE);
        notFound.setVisibility(View.GONE);

        sectionTitle.setText("Hospitals near you");
        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        retrieveHospitalList(results -> {
            Log.i("Channel Fragment", "Hospital section attached");

            HospitalAdapter adapter = new HospitalAdapter(results, hospital -> {

                String mapUrl = "http://maps.google.com/maps?q=" + hospital.getLocation().getLatitude() + "," + hospital.getLocation().getLongitude();

                Intent intent = new Intent(requireActivity(), AppointmentActivity.class);
                intent.putExtra("type", Helper.HOSPITAL);
                intent.putExtra("data", hospital.getPlaceId());

                intent.putExtra("imageUrl", hospital.getImageUrl());
                intent.putExtra("name", hospital.getName());
                intent.putExtra("address", hospital.getAddress());
                intent.putExtra("mapUrl", mapUrl);
                intent.putExtra("lat", hospital.getLocation().getLatitude());
                intent.putExtra("lng", hospital.getLocation().getLongitude());

                Log.i("MapCheck", "initial: lat[" + hospital.getLocation().getLatitude() + "] lan[" + hospital.getLocation().getLongitude() + "]");

                if (firebaseToken != null) {
                    intent.putExtra("authToken", firebaseToken);
                }
                startActivity(intent);

                Log.i("PlaceIdCheck", "Name: [" + hospital.getName() + "] placeId[" + hospital.getPlaceId() + "]");
            });

            container.setAdapter(adapter);
            parent.addView(section);
        });
    }

    private void retrieveHospitalList(CallBack<List<HospitalDto>> callBack) {
        mapApi.getCurrentLocation(requireActivity(), latLng -> {
            retrieveNearHospitalsFromPlacesApi(latLng, hospitalDtoList -> {

                GeoLocation center = new GeoLocation(latLng.latitude, latLng.longitude);
                List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);

                Log.i("Channel Fragment", "Hospitals retrieving started for bounds");

                List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (GeoQueryBounds b : bounds) {
                    Query q = firestore.collection("hospitals")
                            .limit(10)
                            .orderBy("geohash")
                            .startAt(b.startHash)
                            .endAt(b.endHash);

                    tasks.add(q.get());
                }

                Tasks.whenAllComplete(tasks)
                        .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                            @Override
                            public void onComplete(@NonNull Task<List<Task<?>>> t) {
                                List<HospitalDto> hospitalList = new ArrayList<>();

                                for (Task<QuerySnapshot> task : tasks) {
                                    QuerySnapshot snap = task.getResult();
                                    for (DocumentSnapshot doc : snap.getDocuments()) {

                                        GeoPoint geoPoint = doc.getGeoPoint("location");

                                        if (geoPoint != null) {
                                            double lat = geoPoint.getLatitude();
                                            double lng = geoPoint.getLongitude();

                                            // filter false matches
                                            GeoLocation docLocation = new GeoLocation(lat, lng);
                                            double distanceInM = GeoFireUtils.getDistanceBetween(docLocation, currentLocation);
                                            if (distanceInM <= radiusInM) {
                                                if (hospitalList.size() <= 10) {
                                                    Hospital hospital = doc.toObject(Hospital.class);
                                                    HospitalDto dto = new HospitalDto(
                                                            hospital.getPlaceId(),
                                                            hospital.getImageUrl(),
                                                            hospital.getName(),
                                                            hospital.getLowerName(),
                                                            hospital.getAddress(),
                                                            hospital.getContact(),
                                                            hospital.getEmail(),
                                                            new LocationDto(hospital.getLocation().getLatitude(), hospital.getLocation().getLongitude()),
                                                            hospital.getGeohash(),
                                                            hospital.getMapUrl()
                                                    );
                                                    hospitalList.add(dto);
                                                } else {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }

                                for (HospitalDto hospital : hospitalDtoList) {
                                    if (!hospitalList.contains(hospital)) {
                                        hospitalList.add(hospital);
                                    }
                                }

                                if (hospitalList.isEmpty()) {
                                    backupHospitalList(backupHospitals -> {
                                        hospitalList.addAll(backupHospitals);
                                    });
                                }
                                callBack.callBack(hospitalList);
                            }
                        });
            });
        });
    }

    private void backupHospitalList(CallBack<List<HospitalDto>> callBack) {
        firestore.collection("hospitals")
                .limit(10).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        List<HospitalDto> hospitalList = new ArrayList<>();
                        if (qds != null) {
                            for (Hospital hospital : qds.toObjects(Hospital.class)) {
                                HospitalDto hospitalDto = new HospitalDto(
                                        hospital.getPlaceId(),
                                        hospital.getImageUrl(),
                                        hospital.getName(),
                                        hospital.getLowerName(),
                                        hospital.getAddress(),
                                        hospital.getContact(),
                                        hospital.getEmail(),
                                        new LocationDto(hospital.getLocation().getLatitude(), hospital.getLocation().getLongitude()),
                                        hospital.getGeohash(),
                                        hospital.getMapUrl()
                                );
                                hospitalList.add(hospitalDto);
                            }
                        }
                        callBack.callBack(hospitalList);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        callBack.callBack(null);
                    }
                });
    }

    private void retrieveNearHospitalsFromPlacesApi(LatLng latLng, CallBack<List<HospitalDto>> callBack) {

        MapApiService api = MapApiRetrofitClient
                .getInstance()
                .create(MapApiService.class);

        String location = latLng.latitude + "," + latLng.longitude;

        Call<NearByResponseDto> responseCall = api.getNearbyHospitals(
                location,
                radiusInM,
                "hospital",
                "hospital medical",
                mapApiKey
        );

        responseCall.enqueue(new Callback<NearByResponseDto>() {
            @Override
            public void onResponse(Call<NearByResponseDto> call, Response<NearByResponseDto> response) {

                if (response.isSuccessful() && response.body() != null) {

                    List<NearByResponseDto.Result> results = response.body().getResults();
                    Set<HospitalDto> filteredResults = new HashSet<>();

                    for (NearByResponseDto.Result result : results) {
                        if (!result.getName().contains("veterinary")
                                && !result.getName().contains("pharmacy")
                                && !result.getName().contains("pet")
                                && !result.getName().contains("animal")
                                && !result.getName().contains("care")) {

                            String imageUrl = "";

                            if (result.getPhotos() != null && !result.getPhotos().isEmpty()) {

                                String photoRef = result.getPhotos().get(0).getPhoto_reference();

                                imageUrl = "https://maps.googleapis.com/maps/api/place/photo"
                                        + "?maxwidth=400"
                                        + "&photo_reference=" + photoRef
                                        + "&key=" + mapApiKey;
                            }

                            double lat = result.getGeometry().getLocation().getLat();
                            double lng = result.getGeometry().getLocation().getLng();

                            String mapUrl = "https://www.google.com/maps?q="
                                    + lat + "," + lng;

                            HospitalDto apiDto = new HospitalDto(
                                    result.getPlace_id(),
                                    imageUrl,
                                    result.getName(),
                                    result.getName().toLowerCase(),
                                    result.getVicinity(),
                                    "",
                                    "",
                                    new LocationDto(lat, lng),
                                    GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng)),
                                    mapUrl
                            );

                            filteredResults.add(apiDto);
                            Log.i("check", "hospital: name[" + result.getName() + "] lat[" + lat + "] lng[" + lng + "]");
                        } else {
                            Log.e("PlacesApi", "excluded: " + result);
                        }
                    }

                    nearBySearchResults = new ArrayList<HospitalDto>(filteredResults);

                    callBack.callBack(nearBySearchResults);
                }
            }

            @Override
            public void onFailure(Call<NearByResponseDto> call, Throwable throwable) {
                Log.e("PlacesApi", "onFailure: ", throwable);
            }
        });
    }

    private void fetchCurrentLocation(CallBack<GeoLocation> callBack) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        callBack.callBack(
                                new GeoLocation(location.getLatitude(), location.getLongitude())
                        );
                    }
                });
    }

    private void getAppointmentList(LayoutInflater inflater, LinearLayout parent, CallBack<Boolean> callBack) {
        ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);

        Call<List<AppointmentDto>> result = apiService.retrieveAppointments(new SingleRequestDataDto(firebaseAuth.getUid()));
        result.enqueue(new Callback<List<AppointmentDto>>() {
            @Override
            public void onResponse(Call<List<AppointmentDto>> call, Response<List<AppointmentDto>> response) {
                if (response.isSuccessful()) {

                    List<AppointmentDto> appointments = response.body();
                    if (appointments != null && !appointments.isEmpty()) {
                        View section = inflater.inflate(R.layout.card_section, parent, false);
                        TextView sectionTitle = section.findViewById(R.id.card_section_title);
                        RecyclerView container = section.findViewById(R.id.card_section_container);

                        sectionTitle.setText("My Appointments");

                        Log.d("Appointments", "Appointment 01: " + appointments.get(0));

                        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
                        ExistingAppointmentsAdapter adapter = new ExistingAppointmentsAdapter(appointments, appointmentDto -> {
                            // onclick listener
//                            Toast.makeText(requireContext(), "Appointment Onclick", Toast.LENGTH_SHORT).show();
                        });
                        container.setAdapter(adapter);

                        parent.addView(section);
                    } else {
                        Log.d("ChannelFragment", "No appointments scheduled");
                    }

                    callBack.callBack(true);
                } else {
                    Log.d("ChannelFragment", "onResponse: Appointments fetching failed");
                }
            }

            @Override
            public void onFailure(Call<List<AppointmentDto>> call, Throwable throwable) {
                Log.d("ChannelFragment", "Appointments fetching failed: " + throwable);
            }
        });
    }

    private void attachChannelSearchResults(String searchText, LayoutInflater inflater, LinearLayout parent) {
        if (searchText != null && !searchText.isEmpty()) {

            View section = inflater.inflate(R.layout.card_section, parent, false);
            TextView sectionTitle = section.findViewById(R.id.card_section_title);
            RecyclerView container = section.findViewById(R.id.card_section_container);
            Chip notFound = section.findViewById(R.id.cardSectionResultNotFound);

            container.setVisibility(View.VISIBLE);
            notFound.setVisibility(View.GONE);

            sectionTitle.setText("Search results for “" + searchText + "” ");
            container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));

            ChannelingApiService apiService = retrofit.create(ChannelingApiService.class);
            Call<SearchResultDto> searchResults = apiService.retrieveSearchResults(new SingleRequestDataDto(searchText));
            searchResults.enqueue(new Callback<SearchResultDto>() {
                @Override
                public void onResponse(Call<SearchResultDto> call, Response<SearchResultDto> response) {
                    if (response.isSuccessful()) {
                        SearchResultDto resultDto = response.body();
                        boolean showNoResult = false;

                        List<HospitalDto> list = new ArrayList<>();
                        if (nearBySearchResults != null && !nearBySearchResults.isEmpty()) {
                            for (HospitalDto nearBy : nearBySearchResults) {
                                if (nearBy.getLowerName().contains(searchText)) {
                                    if (list.isEmpty()) {
                                        list.add(nearBy);
                                    } else if (!list.contains(nearBy)) {
                                        list.add(nearBy);
                                    }
                                }
                            }
                        }

                        if (resultDto != null) {
                            if ((resultDto.getConsultantList() != null && !resultDto.getConsultantList().isEmpty())
                                    || (resultDto.getHospitalList() != null && !resultDto.getHospitalList().isEmpty())) {

                                if (resultDto.getHospitalList() != null && !resultDto.getHospitalList().isEmpty()) {
                                    for (HospitalDto savedHospital : resultDto.getHospitalList()) {
                                        if (list.isEmpty()) {
                                            list.add(savedHospital);
                                        } else if (!list.contains(savedHospital)) {
                                            list.add(savedHospital);
                                        }
                                    }
                                }
                            }
                        }

                        if (!list.isEmpty()) {
                            if (resultDto == null) {
                                resultDto = new SearchResultDto();
                            }

                            resultDto.setHospitalList(list);

                            if (resultDto.getConsultantList() == null) {
                                resultDto.setConsultantList(new ArrayList<>());
                            }

                            SearchResultsAdapter adapter = new SearchResultsAdapter(resultDto, searchItem -> {

                                Intent intent = new Intent(requireActivity(), AppointmentActivity.class);

                                if (searchItem.getType() == SearchItemDto.TYPE_HOSPITAL) {

                                    String mapUrl = "http://maps.google.com/maps?q=" + searchItem.getHospitalDto().getLocation().getLatitude() + "," + searchItem.getHospitalDto().getLocation().getLongitude();

                                    intent.putExtra("type", Helper.HOSPITAL);
                                    intent.putExtra("data", searchItem.getHospitalDto().getPlaceId());
                                    intent.putExtra("imageUrl", searchItem.getHospitalDto().getImageUrl());
                                    intent.putExtra("name", searchItem.getHospitalDto().getName());
                                    intent.putExtra("address", searchItem.getHospitalDto().getAddress());
                                    intent.putExtra("mapUrl", mapUrl);
                                    intent.putExtra("lat", searchItem.getHospitalDto().getLocation().getLatitude());
                                    intent.putExtra("lng", searchItem.getHospitalDto().getLocation().getLongitude());
                                } else {
                                    intent.putExtra("type", Helper.CONSULTANT);
                                    intent.putExtra("data", searchItem.getConsultantDto().getConsultant().getUid());
                                }

                                if (firebaseToken != null) {
                                    intent.putExtra("authToken", firebaseToken);
                                }
                                startActivity(intent);
                            });

                            container.setAdapter(adapter);

                        } else {
                            showNoResult = true;
                        }

                        if (showNoResult) {
                            container.setVisibility(View.GONE);
                            notFound.setVisibility(View.VISIBLE);
                        }

                        parent.removeAllViews();
                        parent.addView(section);

                    } else {
                        Log.d("ChannelFragment", "Search results retrieving failed: [" + response.code() + "] " + response.message());
                        Toast.makeText(requireActivity(), "Search results retrieve failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<SearchResultDto> call, Throwable throwable) {
                    Log.d("ChannelFragment", "Search results retrieving failed: " + throwable);
                    Toast.makeText(requireActivity(), "Search results retrieve failed", Toast.LENGTH_SHORT).show();
                }
            });

        }
    }

}