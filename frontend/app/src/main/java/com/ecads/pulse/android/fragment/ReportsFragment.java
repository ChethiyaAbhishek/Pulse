package com.ecads.pulse.android.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.ReportViewActivity;
import com.ecads.pulse.android.adapter.ReportsAdapter;
import com.ecads.pulse.android.databinding.FragmentReportsBinding;
import com.ecads.pulse.android.dto.ReportDto;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportsFragment extends Fragment {

    private FragmentReportsBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private ActivityResultLauncher<String> galleryLauncher;
    private Uri imageUri;
    private View dialogLayout;
    BottomSheetDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        dialogLayout = getLayoutInflater()
                .inflate(R.layout.report_uploading_dialog, null);
        dialog = new BottomSheetDialog(requireContext());
        dialog.setContentView(dialogLayout);

        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            imageUri = uri;

                            dialogLayout.findViewById(R.id.imgContainer).setVisibility(View.VISIBLE);
                            dialogLayout.findViewById(R.id.imgSelectorContainer).setVisibility(View.GONE);
                            ImageView imgContainer = dialogLayout.findViewById(R.id.reportCardImage);

                            Glide.with(requireContext())
                                    .load(uri)
                                    .centerCrop()
                                    .into(imgContainer);
                        }
                    }
                });

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentReportsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (firebaseAuth.getCurrentUser() == null) {
            Toast.makeText(requireActivity(), "No user found, Please try again after logging in", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        binding.addReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.show();
            }
        });

        dialogLayout.findViewById(R.id.imgSelectorContainer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                galleryLauncher.launch("image/*");
            }
        });

        dialogLayout.findViewById(R.id.uploadReport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadReport();
                dialogLayout.findViewById(R.id.reportCardloadingProgress).setVisibility(View.VISIBLE);
                dialogLayout.findViewById(R.id.uploadReport).setVisibility(View.GONE);
            }
        });

        loadReports();
    }

    private void loadReports() {
        binding.noReportsCard.setVisibility(View.GONE);
        binding.reportFragmentRecycler.setVisibility(View.VISIBLE);

        firestore.collection("patients")
                .document("PA_" + firebaseAuth.getUid())
                .collection("reports").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {
                            List<ReportDto> reportDtoList = qds.toObjects(ReportDto.class);

                            RecyclerView recyclerView = binding.reportFragmentRecycler;
                            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
                            ReportsAdapter adapter = new ReportsAdapter(reportDtoList, reportDto -> {
                                Intent intent = new Intent(requireActivity(), ReportViewActivity.class);
                                intent.putExtra("uri", reportDto.getImageUri());
                                startActivity(intent);
                            });
                            recyclerView.setAdapter(adapter);

                        } else {
                            // no reports
                            binding.noReportsCard.setVisibility(View.VISIBLE);
                            binding.reportFragmentRecycler.setVisibility(View.GONE);
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireActivity(), "Reports loading failed", Toast.LENGTH_SHORT).show();
                        Log.e("ReportFragment", "Reports loading failed: " + e.getMessage());
                    }
                });
    }

    private void uploadReport() {
        EditText imgLabel = dialogLayout.findViewById(R.id.reportCardLabel);
        dialogLayout.findViewById(R.id.reportCardLabelError).setVisibility(View.GONE);
        dialogLayout.findViewById(R.id.reportCardImageError).setVisibility(View.GONE);

        if (imgLabel.getText().toString().trim().isEmpty()) {
            dialogLayout.findViewById(R.id.reportCardLabelError).setVisibility(View.VISIBLE);
            return;
        }

        if (imageUri == null) {
            dialogLayout.findViewById(R.id.reportCardImageError).setVisibility(View.VISIBLE);
            return;
        }

        String fileName = "patient/PA_" + firebaseAuth.getUid() + "/reports/RE_" + System.currentTimeMillis() + ".jpg";
        StorageReference reference = storageReference.child(fileName);

        reference.putFile(imageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                reference.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveInFirestore(uri, imgLabel.getText().toString().trim());
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(requireActivity(), "Report uploading failed", Toast.LENGTH_SHORT).show();
                Log.e("ReportFragment", "Report img uploading failed: " + e.getMessage());
            }
        });
    }

    private void saveInFirestore(Uri uploadedUri, String label) {
        ReportDto reportDto = new ReportDto();
        reportDto.setImageUri(String.valueOf(uploadedUri));
        reportDto.setLabel(label);
        reportDto.setUploadedDateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("hh:mm a yyyy-MM-dd")));

        firestore.collection("patients").document("PA_" + firebaseAuth.getUid())
                .collection("reports")
                .document().set(reportDto).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(requireActivity(), "Report uploaded", Toast.LENGTH_SHORT).show();

                        dialogLayout.findViewById(R.id.reportCardloadingProgress).setVisibility(View.GONE);
                        dialogLayout.findViewById(R.id.uploadReport).setVisibility(View.VISIBLE);

                        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.settingsSubFragment, new ReportsFragment());
                        transaction.commit();
                        dialog.cancel();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(requireActivity(), "Report saving failed", Toast.LENGTH_SHORT).show();
                        Log.e("ReportFragment", "Report img saving failed: " + e.getMessage());
                    }
                });
    }
}