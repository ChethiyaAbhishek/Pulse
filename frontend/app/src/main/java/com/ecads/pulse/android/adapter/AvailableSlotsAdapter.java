package com.ecads.pulse.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;

import java.util.List;

public class AvailableSlotsAdapter extends
        RecyclerView.Adapter<AvailableSlotsAdapter.ViewHolder> {
    private List<String> availableSlots;
    private OnConsultantClickListener clickListener;
    private int selectedPosition = -1;

    public AvailableSlotsAdapter(List<String> availableSlots, OnConsultantClickListener clickListener) {
        this.availableSlots = availableSlots;
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
        String slot = availableSlots.get(position);
        holder.slot.setText(slot);

        if (position == selectedPosition) {
            holder.slot.setBackgroundResource(R.drawable.custom_chip);
        } else {
            holder.slot.setBackgroundResource(R.drawable.card_bg);
        }

        holder.layout.setOnClickListener(view -> {
            int previousSelected = selectedPosition;

            selectedPosition = position;

            if (previousSelected != -1) {
                notifyItemChanged(previousSelected);
            }
            notifyItemChanged(selectedPosition);

            clickListener.onAvailableSlotClick(slot);
        });
    }

    @Override
    public int getItemCount() {
        return availableSlots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView slot;
        private ConstraintLayout layout;

        public ViewHolder(@NonNull View chip) {
            super(chip);
            slot = chip.findViewById(R.id.customChipText);
            layout = chip.findViewById(R.id.customChip);
        }
    }

    public interface OnConsultantClickListener {
        void onAvailableSlotClick(String slot);
    }
}
