package com.ecads.pulse.android.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.dto.HospitalDto;

import java.util.List;

public class ConsultantBasedHospitalsAdapter extends
        RecyclerView.Adapter<ConsultantBasedHospitalsAdapter.ViewHolder> {
    private List<HospitalDto> hospitals;
    private OnHospitalClickListener clickListener;
    private int selectedPosition = -1;

    public ConsultantBasedHospitalsAdapter(List<HospitalDto> hospitals, OnHospitalClickListener clickListener) {

        Log.i("check", "adapter hospital details:" + hospitals.get(0).getName()+", "+ hospitals.get(0).getAddress());

        this.hospitals = hospitals;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_hospital_chip_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HospitalDto hospital = hospitals.get(position);
        holder.hospitalName.setText(hospital.getName());
        holder.hospitalAddress.setText(hospital.getAddress());

        if (position == selectedPosition) {
            holder.layout.setBackgroundResource(R.drawable.custom_chip);
        } else {
            holder.layout.setBackgroundResource(R.drawable.card_bg);
        }

        holder.layout.setOnClickListener(view -> {
            int previousSelected = selectedPosition;

            selectedPosition = position;

            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            clickListener.onHospitalClick(hospital);
        });
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView hospitalName;
        private TextView hospitalAddress;
        private LinearLayout layout;

        public ViewHolder(@NonNull View chip) {
            super(chip);
            hospitalName = chip.findViewById(R.id.customHosChipName);
            hospitalAddress = chip.findViewById(R.id.customHosChipAddress);
            layout = chip.findViewById(R.id.customHosChip);
        }
    }

    public interface OnHospitalClickListener {
        void onHospitalClick(HospitalDto hospital);
    }

}
