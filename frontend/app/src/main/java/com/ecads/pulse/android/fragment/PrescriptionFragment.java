package com.ecads.pulse.android.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.adapter.PrescriptionItemsAdapter;
import com.ecads.pulse.android.adapter.PrescriptionsAdapter;
import com.ecads.pulse.android.databinding.FragmentPrescriptionBinding;
import com.ecads.pulse.android.dto.PrescriptionDto;
import com.ecads.pulse.android.dto.PrescriptionItemDto;
import com.ecads.pulse.android.model.Consultant;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PrescriptionFragment extends Fragment {

    private FragmentPrescriptionBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPrescriptionBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(requireActivity(), "No user found. Loading failed", Toast.LENGTH_SHORT).show();
            binding.noPrescriptionsCard.setVisibility(View.VISIBLE);
            binding.prescriptionRecycler.setVisibility(View.GONE);
            return;
        }

        loadPrescriptions();
    }

    private void loadPrescriptions() {
        firestore.collection("prescriptions")
                .whereEqualTo("patientId", "PA_" + firebaseAuth.getUid())
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {
                            List<PrescriptionDto> prescriptionList = qds.toObjects(PrescriptionDto.class);
                            if (!prescriptionList.isEmpty()) {

                                RecyclerView recycler = binding.prescriptionRecycler;
                                recycler.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false));
                                PrescriptionsAdapter adapter = new PrescriptionsAdapter(prescriptionList, prescription -> {
                                    loadPrescriptionItems(prescription);
                                });
                                recycler.setAdapter(adapter);

                            } else {
                                binding.noPrescriptionsCard.setVisibility(View.VISIBLE);
                                binding.prescriptionRecycler.setVisibility(View.GONE);
                            }
                        } else {
                            binding.noPrescriptionsCard.setVisibility(View.VISIBLE);
                            binding.prescriptionRecycler.setVisibility(View.GONE);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireActivity(), "Prescriptions loading failed", Toast.LENGTH_SHORT).show();
                        Log.e("PrescriptionFragment", "Prescriptions loading failed: " + e);
                    }
                });
    }

    private void loadPrescriptionItems(PrescriptionDto prescription) {
        View dialogLayout = getLayoutInflater()
                .inflate(R.layout.prescription_item_view_dialog, null);
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogLayout);
        dialog.show();

        TextView consultant = dialog.findViewById(R.id.presItemViewConsulName);
        consultant.setText(prescription.getConsultantName());

        LocalDate parsed = LocalDate.parse(prescription.getIssuedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String formatted = parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

        TextView date = dialog.findViewById(R.id.presItemViewDate);
        date.setText(formatted);

        Chip status = dialog.findViewById(R.id.presItemViewStatus);
        status.setText(prescription.getStatus().toUpperCase());

        firestore.collection("prescriptionItems")
                .whereEqualTo("prescriptionId", prescription.getPrescriptionId())
                .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {

                            List<PrescriptionItemDto> items = qds.toObjects(PrescriptionItemDto.class);
                            if (!items.isEmpty()) {

                                RecyclerView recycler = dialog.findViewById(R.id.presItemViewRecycler);
                                recycler.setLayoutManager(new LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false));
                                PrescriptionItemsAdapter adapter = new PrescriptionItemsAdapter(items);
                                recycler.setAdapter(adapter);

                            } else {
                                dialog.findViewById(R.id.noPrescriptionItemsCard).setVisibility(View.VISIBLE);
                                dialog.findViewById(R.id.presItemViewRecycler).setVisibility(View.GONE);
                            }
                        } else {
                            dialog.findViewById(R.id.noPrescriptionItemsCard).setVisibility(View.VISIBLE);
                            dialog.findViewById(R.id.presItemViewRecycler).setVisibility(View.GONE);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireActivity(), "Prescription medications loading failed", Toast.LENGTH_SHORT).show();
                        Log.e("PrescriptionFragment", "Prescriptions medications loading failed: " + e.getMessage());
                    }
                });


    }
}