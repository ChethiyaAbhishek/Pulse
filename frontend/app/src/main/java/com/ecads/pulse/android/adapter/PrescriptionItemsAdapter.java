package com.ecads.pulse.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.dto.PrescriptionItemDto;
import com.google.android.material.chip.Chip;

import java.util.List;

public class PrescriptionItemsAdapter extends RecyclerView.Adapter<PrescriptionItemsAdapter.ViewHolder> {

    private List<PrescriptionItemDto> prescriptionItemsList;

    public PrescriptionItemsAdapter(List<PrescriptionItemDto> prescriptionItemsList) {
        this.prescriptionItemsList = prescriptionItemsList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.prescription_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!prescriptionItemsList.isEmpty()) {
            PrescriptionItemDto prescriptionItem = prescriptionItemsList.get(position);

            holder.medicineName.setText(prescriptionItem.getMedicineName());
            holder.dose.setText(prescriptionItem.getDosage());
            holder.frequency.setText(prescriptionItem.getFrequency());
            holder.duration.setText(prescriptionItem.getDuration().toUpperCase());
            holder.instructions.setText(prescriptionItem.getInstructions());
        }
    }

    @Override
    public int getItemCount() {
        return prescriptionItemsList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView medicineName;
        private Chip dose;
        private Chip frequency;
        private Chip duration;
        private TextView instructions;
        private LinearLayout card;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            medicineName = cardView.findViewById(R.id.prescriptionItemMedName);
            dose = cardView.findViewById(R.id.prescriptionItemDoseChip);
            frequency = cardView.findViewById(R.id.prescriptionItemFrequencyChip);
            duration = cardView.findViewById(R.id.prescriptionItemDurationChip);
            instructions = cardView.findViewById(R.id.prescriptionItemInstructions);
            card = cardView.findViewById(R.id.prescriptionItemCardLayout);
        }
    }

}
