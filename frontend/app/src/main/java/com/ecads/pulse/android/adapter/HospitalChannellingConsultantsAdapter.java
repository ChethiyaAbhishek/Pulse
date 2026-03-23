package com.ecads.pulse.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.model.Consultant;

import java.util.List;

public class HospitalChannellingConsultantsAdapter extends
        RecyclerView.Adapter<HospitalChannellingConsultantsAdapter.ViewHolder> {
    private List<Consultant> consultants;
    private OnConsultantClickListener clickListener;
    private int selectedPosition = -1;

    public HospitalChannellingConsultantsAdapter(List<Consultant> consultants, OnConsultantClickListener clickListener) {
        this.consultants = consultants;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_chip_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Consultant consultant = consultants.get(position);
        holder.consultantName.setText(consultant.getName());

        if (position == selectedPosition) {
            holder.consultantName.setBackgroundResource(R.drawable.custom_chip);
        } else {
            holder.consultantName.setBackgroundResource(R.drawable.card_bg);
        }

        holder.layout.setOnClickListener(view -> {
            int previousSelected = selectedPosition;

            selectedPosition = position;

            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            clickListener.onConsultantClick(consultant);
        });
    }

    @Override
    public int getItemCount() {
        return consultants.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView consultantName;
        private ConstraintLayout layout;

        public ViewHolder(@NonNull View chip) {
            super(chip);
            consultantName = chip.findViewById(R.id.customChipText);
            layout = chip.findViewById(R.id.customChip);
        }
    }

    public interface OnConsultantClickListener {
        void onConsultantClick(Consultant consultant);
    }

}
