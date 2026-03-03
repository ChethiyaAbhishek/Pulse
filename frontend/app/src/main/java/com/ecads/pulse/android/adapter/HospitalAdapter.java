package com.ecads.pulse.android.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.model.Appointment;
import com.ecads.pulse.android.model.Hospital;
import com.ecads.pulse.android.utils.Helper;

import java.util.List;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {

    private List<Hospital> hospitals;
    private OnCardClickListener clickListener;

    public HospitalAdapter(List<Hospital> hospitals, OnCardClickListener clickListener) {
        this.hospitals = hospitals;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.hospital_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        if (!hospitals.isEmpty()) {
            Hospital hospital = hospitals.get(position);

            holder.hospitalName.setText(hospital.getName());
            holder.hospitalAddress.setText(hospital.getAddress());
            holder.hospitalImg.setImageURI(Uri.parse(hospital.getImageUrl()));

            holder.contactBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + hospital.getContact()));
                view.getContext().startActivity(intent);
            });

            holder.locationBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(hospital.getMapUrl()));
                view.getContext().startActivity(intent);
            });

//            RecyclerView recyclerView = holder.consultantView;
//            recyclerView.setLayoutManager(
//                    new LinearLayoutManager(recyclerView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        }
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView hospitalName;
        private TextView hospitalAddress;
        private ImageView hospitalImg;
        private ImageButton contactBtn;
        private ImageButton locationBtn;
        private RecyclerView consultantView;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            hospitalName = cardView.findViewById(R.id.hospital_card_name);
            hospitalAddress = cardView.findViewById(R.id.hospital_card_address);
            hospitalImg = cardView.findViewById(R.id.hospital_card_img);
            contactBtn = cardView.findViewById(R.id.hospital_card_call_btn);
            locationBtn = cardView.findViewById(R.id.hospital_card_location_btn);
            consultantView = cardView.findViewById(R.id.hospital_card_doc_availability_container);
        }
    }

    public interface OnCardClickListener {
        void onCardClick(Hospital hospital);
    }

}
