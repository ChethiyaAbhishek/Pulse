package com.ecads.pulse.android.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.model.Appointment;
import com.ecads.pulse.android.utils.Helper;

import java.util.List;

public class ExistingAppointmentsAdapter extends RecyclerView.Adapter<ExistingAppointmentsAdapter.ViewHolder> {

    private List<Appointment> appointments;
    private OnAppointmentClickListener clickListener;

    public ExistingAppointmentsAdapter(List<Appointment> appointments, OnAppointmentClickListener clickListener) {
        this.appointments = appointments;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.appointment_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Appointment appointment = appointments.get(position);

        holder.therapistName.setText(appointment.getTherapistName());
        holder.scheduledTime.setText(appointment.getScheduledTime());
        holder.hospital.setText(appointment.getHospital());

        String date = Helper.formatDate(appointment.getDate());

        if (date.equals("Today")) {
//            holder.cardLayout.setBackground(?attr/colorGreenCard);
        }

        holder.date.setText(date);

        holder.locationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(appointment.getLocationLink()));
            view.getContext().startActivity(intent);
        });

        if (appointment.getVideoLink().isEmpty()) {
            holder.videoCallBtn.setEnabled(false);
            holder.videoCallBtn.setAlpha(0.5f);
        } else {
            holder.videoCallBtn.setOnClickListener(view -> {

            });
        }

    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView therapistName;
        private TextView scheduledTime;
        private TextView hospital;
        private TextView date;
        private ImageButton locationBtn;
        private ImageButton videoCallBtn;
        private LinearLayout cardLayout;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            therapistName = cardView.findViewById(R.id.appointment_card_doctor);
            scheduledTime = cardView.findViewById(R.id.appointment_card_time);
            hospital = cardView.findViewById(R.id.appointment_card_address);
            date = cardView.findViewById(R.id.appointment_card_date);
            locationBtn = cardView.findViewById(R.id.appointment_card_location_btn);
            videoCallBtn = cardView.findViewById(R.id.appointment_card_video_btn);
            cardLayout = cardView.findViewById(R.id.appointment_card_layout);
        }
    }

    public interface OnAppointmentClickListener {
        void onAppointmentClick(Appointment appointment);
    }

}
