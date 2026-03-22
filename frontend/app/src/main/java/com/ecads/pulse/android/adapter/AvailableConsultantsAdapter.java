package com.ecads.pulse.android.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.utils.Helper;
import com.google.android.material.chip.Chip;

import java.util.List;

public class AvailableConsultantsAdapter extends RecyclerView.Adapter<AvailableConsultantsAdapter.ViewHolder> {

    private List<AvailableConsultantDto> availableConsultants;
    private OnAvailableConsultantClickListener clickListener;

    public AvailableConsultantsAdapter(List<AvailableConsultantDto> availableConsultants, OnAvailableConsultantClickListener clickListener) {
        this.availableConsultants = availableConsultants;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.consultant_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        AvailableConsultantDto availableConsultantDto = availableConsultants.get(position);

        holder.hospital.setText(availableConsultantDto.getHospital().getName());
        holder.therapistName.setText(availableConsultantDto.getConsultant().getTitle() + " " + availableConsultantDto.getConsultant().getName());
        holder.speciality.setText(availableConsultantDto.getConsultant().getSpeciality());
        holder.baseHospital.setText(availableConsultantDto.getConsultant().getBaseHospital());

        if (!availableConsultantDto.getAvailableSlots().isEmpty()) {
            holder.slotChip1.setVisibility(View.VISIBLE);
            holder.slotChip2.setVisibility(View.VISIBLE);

            holder.slotChip1.setText(availableConsultantDto.getAvailableSlots().get(0));
            holder.slotChip2.setText(availableConsultantDto.getAvailableSlots().get(1));
        } else {
            holder.slotChip1.setVisibility(View.GONE);
            holder.slotChip2.setVisibility(View.GONE);
        }

//        holder.slotChip1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });

        holder.cardLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onAvailableConsultantClick(availableConsultantDto);
            }
        });
    }

    @Override
    public int getItemCount() {
        return availableConsultants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView hospital;
        private TextView therapistName;
        private TextView speciality;
        private TextView baseHospital;
        private Chip slotChip1;
        private Chip slotChip2;
        private LinearLayout cardLayout;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            hospital = cardView.findViewById(R.id.consultant_card_hospital);
            therapistName = cardView.findViewById(R.id.consultant_card_name);
            speciality = cardView.findViewById(R.id.consultant_card_speciality);
            baseHospital = cardView.findViewById(R.id.consultant_card_base_hospital);
            slotChip1 = cardView.findViewById(R.id.consultantCardSlot1);
            slotChip2 = cardView.findViewById(R.id.consultantCardSlot2);
            cardLayout = cardView.findViewById(R.id.consultantCardLayout);
        }
    }

    public interface OnAvailableConsultantClickListener {
        void onAvailableConsultantClick(AvailableConsultantDto availableConsultantDto);
    }

}
