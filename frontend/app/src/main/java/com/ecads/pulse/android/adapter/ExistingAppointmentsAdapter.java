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
import com.ecads.pulse.android.activity.OnlineAppointmentActivity;
import com.ecads.pulse.android.model.AppointmentDto;
import com.ecads.pulse.android.utils.Helper;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ExistingAppointmentsAdapter extends RecyclerView.Adapter<ExistingAppointmentsAdapter.ViewHolder> {

    private List<AppointmentDto> appointments;
    private OnAppointmentClickListener clickListener;

    public ExistingAppointmentsAdapter(List<AppointmentDto> appointments, OnAppointmentClickListener clickListener) {
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

        AppointmentDto appointmentDto = appointments.get(position);

        holder.therapistName.setText(appointmentDto.getTherapistName());
        holder.scheduledTime.setText(appointmentDto.getScheduledTime().toUpperCase());
        holder.hospital.setText(appointmentDto.getHospital());

        String date = Helper.formatDate(appointmentDto.getDate());

        if (date.equals("Today")) {
//            holder.cardLayout.setBackground(?attr/colorGreenCard);
        }

        holder.date.setText(date);

        holder.locationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(appointmentDto.getLocationLink()));
            view.getContext().startActivity(intent);
        });

        String endsAt = appointmentDto.getScheduledTime().substring(11, 19);
        System.out.println("time:"+endsAt+";");
        LocalTime localEndAt = LocalTime.parse(endsAt.toUpperCase(), DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));

        if (appointmentDto.getVideoLink() == null
                || appointmentDto.getVideoLink().isEmpty()
                || LocalTime.now().isAfter(localEndAt)) {

            holder.videoCallBtn.setEnabled(false);
            holder.videoCallBtn.setAlpha(0.5f);
        } else {
            holder.videoCallBtn.setOnClickListener(view -> {
                Intent intent = new Intent(holder.videoCallBtn.getContext(), OnlineAppointmentActivity.class);
                intent.putExtra("appointmentId", appointmentDto.getAppointmentId());
                intent.putExtra("name", appointmentDto.getTherapistName());
                intent.putExtra("session", appointmentDto.getScheduledTime());
                intent.putExtra("hospital", appointmentDto.getHospital());
                intent.putExtra("date", appointmentDto.getDate());
                holder.videoCallBtn.getContext().startActivity(intent);
            });
        }

        holder.cardLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onAppointmentClick(appointmentDto);
            }
        });

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
        void onAppointmentClick(AppointmentDto appointmentDto);
    }

}
