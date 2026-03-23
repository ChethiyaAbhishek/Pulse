package com.ecads.pulse.android.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.activity.OnlineAppointmentActivity;
import com.ecads.pulse.android.dto.PreviousAppointmentDto;
import com.ecads.pulse.android.enums.PaymentStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PreviousAppointmentsAdapter extends RecyclerView.Adapter<PreviousAppointmentsAdapter.ViewHolder> {

    private List<PreviousAppointmentDto> appointments;
    private OnInvoiceBtnClickListener onInvoiceBtnClickListener;
    private OnCancelBtnClickListener onCancelBtnClickListener;

    public PreviousAppointmentsAdapter(List<PreviousAppointmentDto> appointments,
                                       OnInvoiceBtnClickListener onInvoiceBtnClickListener,
                                       OnCancelBtnClickListener onCancelBtnClickListener) {
        this.appointments = appointments;
        this.onInvoiceBtnClickListener = onInvoiceBtnClickListener;
        this.onCancelBtnClickListener = onCancelBtnClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.previous_appointment_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        PreviousAppointmentDto appointmentDto = appointments.get(position);

        LocalDate localDate = LocalDate.parse(appointmentDto.getDate());

        holder.channelingHospital.setText(appointmentDto.getChannelingHospital());
        holder.scheduledTime.setText(appointmentDto.getSlot().toUpperCase() + " - " + localDate.format(DateTimeFormatter.ofPattern("dd MMM")));
        holder.consultantName.setText(appointmentDto.getConsultantName());
        holder.consultantSpeciality.setText(appointmentDto.getConsultantSpeciality());
        holder.consultantBaseHospital.setText(appointmentDto.getBaseHospital());

        holder.locationBtn.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(appointmentDto.getHospitalMapUrl()));
            view.getContext().startActivity(intent);
        });

        holder.videoCallBtn.setEnabled(true);
        holder.videoCallBtn.setAlpha(1f);

        if (appointmentDto.getOnlineChannelLink() == null || appointmentDto.getOnlineChannelLink().isEmpty()) {
            holder.videoCallBtn.setEnabled(false);
            holder.videoCallBtn.setAlpha(0.5f);
        } else {

            LocalTime endsAt = LocalTime.parse(appointmentDto.getEndsAt().toUpperCase(), DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));

            if (LocalTime.now().isAfter(endsAt)
                    || appointmentDto.getPaymentStatus() == PaymentStatus.REFUNDED) {
                holder.videoCallBtn.setEnabled(false);
                holder.videoCallBtn.setAlpha(0.5f);
            } else {

                holder.videoCallBtn.setOnClickListener(view -> {
                    Intent intent = new Intent(holder.videoCallBtn.getContext(), OnlineAppointmentActivity.class);
                    intent.putExtra("appointmentId", appointmentDto.getAppointmentId());
                    intent.putExtra("name", appointmentDto.getConsultantName());
                    intent.putExtra("session", appointmentDto.getSlot() + " - " + appointmentDto.getEndsAt());
                    intent.putExtra("hospital", appointmentDto.getChannelingHospital());
                    intent.putExtra("date", appointmentDto.getDate());
                    holder.videoCallBtn.getContext().startActivity(intent);
                });
            }
        }

        holder.invoiceBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onInvoiceBtnClickListener.onInvoiceBtnClick(appointmentDto);
            }
        });

        holder.invoiceBtn.setVisibility(View.GONE);
        holder.cancelBtn.setEnabled(true);
        holder.cancelBtn.setAlpha(1f);

        if (appointmentDto.getPaymentStatus() == PaymentStatus.REFUNDED) {
            holder.cancelBtn.setText("Cancelled");

            holder.invoiceBtn.setText("REFUNDED");
            holder.invoiceBtn.setVisibility(View.VISIBLE);

            holder.cancelBtn.setEnabled(false);
            holder.cancelBtn.setAlpha(0.5f);
        } else {
            if (localDate.isAfter(LocalDate.now())) {
                holder.cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        onCancelBtnClickListener.onCancelBtnClick(appointmentDto);
                    }
                });
            } else {
                holder.cancelBtn.setEnabled(false);
                holder.cancelBtn.setAlpha(0.5f);
            }
        }

    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private TextView channelingHospital;
        private TextView scheduledTime;
        private TextView consultantName;
        private TextView consultantSpeciality;
        private TextView consultantBaseHospital;
        private ImageButton locationBtn;
        private ImageButton videoCallBtn;
        private Button invoiceBtn;
        private Button cancelBtn;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            channelingHospital = cardView.findViewById(R.id.preAppointmentChannelHospital);
            scheduledTime = cardView.findViewById(R.id.preAppointmentSlot);
            consultantName = cardView.findViewById(R.id.preAppointmentConsultantName);
            consultantSpeciality = cardView.findViewById(R.id.preAppointmentConsultantSpecialty);
            consultantBaseHospital = cardView.findViewById(R.id.preAppointmentConsultantBaseHosAddress);
            locationBtn = cardView.findViewById(R.id.preAppointmentLocationBtn);
            videoCallBtn = cardView.findViewById(R.id.preAppointmentVideoBtn);
            invoiceBtn = cardView.findViewById(R.id.preAppointmentInvoiceBtn);
            cancelBtn = cardView.findViewById(R.id.preAppointmentCancelBtn);
        }
    }

    public interface OnInvoiceBtnClickListener {
        void onInvoiceBtnClick(PreviousAppointmentDto appointmentDto);
    }

    public interface OnCancelBtnClickListener {
        void onCancelBtnClick(PreviousAppointmentDto appointmentDto);
    }

}
