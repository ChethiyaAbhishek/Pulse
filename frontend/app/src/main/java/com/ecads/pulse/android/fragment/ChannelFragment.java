package com.ecads.pulse.android.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.AppointmentActivity;
import com.ecads.pulse.android.adapter.ExistingAppointmentsAdapter;
import com.ecads.pulse.android.adapter.HospitalAdapter;
import com.ecads.pulse.android.databinding.CardSectionBinding;
import com.ecads.pulse.android.databinding.FragmentChannelBinding;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.model.Appointment;
import com.ecads.pulse.android.model.Hospital;
import com.ecads.pulse.android.utils.Helper;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ChannelFragment extends Fragment {

    private FragmentChannelBinding binding;
    private FirebaseFirestore firestore;
    private GeoLocation currentLocation;

    private final double radiusInM;

    public ChannelFragment() {
        //within 50km
        radiusInM = 50 * 1000;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        fetchCurrentLocation();
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
        loadBody(binding.channelBody);
    }

    private void loadBody(LinearLayout layout) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        getAppointmentList(inflater, layout);
        attachHospitalSection(inflater, layout);
        attachAvailableConsultants(inflater, layout);
    }

    private void attachAvailableConsultants(LayoutInflater inflater, LinearLayout parent) {

    }

    private void attachHospitalSection(LayoutInflater inflater, LinearLayout parent) {
        //TODO: Fetch the nearest hospital list[Google Map] & check for hospital existence in firestore
        List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(currentLocation, radiusInM);

        View section = inflater.inflate(R.layout.card_section, parent, false);
        TextView sectionTitle = section.findViewById(R.id.card_section_title);
        RecyclerView container = section.findViewById(R.id.card_section_container);

        sectionTitle.setText("Hospitals near you");
        container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        //TODO: Data type
        List<Hospital> placesApiList = retrieveNearHospitalsFromPlacesApi();
        retrieveHospitalList(bounds, placesApiList, results -> {

            List<Hospital> hospitals = results;
            HospitalAdapter adapter = new HospitalAdapter(results, hospital -> {
                // on click of the hospital card
                //TODO: Navigate to the appointment booking fragment
                Intent intent = new Intent(requireActivity(), AppointmentActivity.class);
                intent.putExtra("type", Helper.HOSPITAL);
                intent.putExtra("data", hospital.getPlaceId());
                startActivity(intent);
            });

            container.setAdapter(adapter);
            parent.addView(section);
        });
    }

    private void retrieveHospitalList(
            List<GeoQueryBounds> bounds, List<Hospital> placesApiList, CallBack<List<Hospital>> callBack) {

        List<Task<QuerySnapshot>> tasks = new ArrayList<>();

        for (GeoQueryBounds b : bounds) {
            Query q = firestore.collection("hospitals")
                    .orderBy("geohash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);

            tasks.add(q.get());
        }

        Tasks.whenAllComplete(tasks)
                .addOnCompleteListener(new OnCompleteListener<List<Task<?>>>() {
                    @Override
                    public void onComplete(@NonNull Task<List<Task<?>>> t) {
                        List<Hospital> hospitalList = new ArrayList<>();

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
                                        hospitalList.add(doc.toObject(Hospital.class));
                                    }
                                }
                            }
                        }

                        for (Hospital hospital : placesApiList) {
                            if (!hospitalList.contains(hospital)) {
                                hospitalList.add(hospital);
                            }
                        }
                        callBack.callBack(hospitalList);
                    }
                });
    }

    private List<Hospital> retrieveNearHospitalsFromPlacesApi() {
        return null;
    }

    private void fetchCurrentLocation() {
        //TODO: Fetch location from map api
        currentLocation = new GeoLocation(6.194806, 80.094667);
    }

    private void getAppointmentList(LayoutInflater inflater, LinearLayout parent) {

        //TODO:Firebase user and backend data fetch
        List<Appointment> appointments = new ArrayList<>();

        Appointment a1 = new Appointment("Dr. Darshani Dharmadasa",
                "10.00 PM - 10.30 PM",
                "National Hospital, Colombo",
                "25-02-2026",
                "https://maps.app.goo.gl/RbNGMRBtjeCau5wPA",
                "");

        Appointment a2 = new Appointment("Dr. Sahan Wikramathunga",
                "07.00 PM - 07.30 PM",
                "Durdans Hospital, Colombo",
                "28-02-2026",
                "https://maps.app.goo.gl/RbNGMRBtjeCau5wPA",
                "");

//        appointments.add(a1);
//        appointments.add(a2);

        if (!appointments.isEmpty()) {
            View section = inflater.inflate(R.layout.card_section, parent, false);
            TextView sectionTitle = section.findViewById(R.id.card_section_title);
            RecyclerView container = section.findViewById(R.id.card_section_container);

            sectionTitle.setText("My Appointments");

            container.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
            ExistingAppointmentsAdapter adapter = new ExistingAppointmentsAdapter(appointments, appointment -> {
                // onclick listener
                Toast.makeText(requireContext(), "Appointment Onclick", Toast.LENGTH_SHORT).show();
            });
            container.setAdapter(adapter);

            parent.addView(section);
        }
    }

}