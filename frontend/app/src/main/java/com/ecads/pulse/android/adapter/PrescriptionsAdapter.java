package com.ecads.pulse.android.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ecads.pulse.android.R;
import com.ecads.pulse.android.dto.PrescriptionDto;
import com.ecads.pulse.android.model.Hospital;
import com.google.android.material.chip.Chip;

import org.w3c.dom.Text;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrescriptionsAdapter extends RecyclerView.Adapter<PrescriptionsAdapter.ViewHolder> {

    private List<PrescriptionDto> prescriptionList;
    private OnCardClickListener clickListener;

    public PrescriptionsAdapter(List<PrescriptionDto> prescriptionList, OnCardClickListener clickListener) {
        this.prescriptionList = prescriptionList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.prescription_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!prescriptionList.isEmpty()) {
            PrescriptionDto prescription = prescriptionList.get(position);

            LocalDate parsed = LocalDate.parse(prescription.getIssuedDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String formatted = parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));

            holder.date.setText(formatted);
            holder.status.setText(prescription.getStatus().toUpperCase());
            holder.consultant.setText(prescription.getConsultantName());
            holder.note.setText(prescription.getNotes());

            holder.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.click_animation);
                    view.startAnimation(animation);
                    if (clickListener != null) {
                        clickListener.onCardClick(prescription);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return prescriptionList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView date;
        private Chip status;
        private TextView consultant;
        private TextView note;
        private LinearLayout card;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            date = cardView.findViewById(R.id.prescriptionCardDate);
            status = cardView.findViewById(R.id.prescriptionCardStatus);
            consultant = cardView.findViewById(R.id.prescriptionCardConsulName);
            note = cardView.findViewById(R.id.prescriptionCardNote);
            card = cardView.findViewById(R.id.prescriptionCardLayout);
        }
    }

    public interface OnCardClickListener {
        void onCardClick(PrescriptionDto prescriptionDto);
    }

}
