package com.ecads.pulse.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.ecads.pulse.android.R;
import com.ecads.pulse.android.dto.ReportDto;

import java.util.List;

public class ReportsAdapter extends RecyclerView.Adapter<ReportsAdapter.ViewHolder> {

    private List<ReportDto> reportDtoList;
    private OnCardClickListener clickListener;

    public ReportsAdapter(List<ReportDto> reportDtoList, OnCardClickListener clickListener) {
        this.reportDtoList = reportDtoList;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.report_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (!reportDtoList.isEmpty()) {
            ReportDto reportDto = reportDtoList.get(position);

            holder.label.setText(reportDto.getLabel());
            holder.uploadedAt.setText(reportDto.getUploadedDateTime());

            Glide.with(holder.reportImg.getContext())
                    .load(reportDto.getImageUri())
//                    .centerCrop()
                    .into(holder.reportImg);

            holder.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.click_animation);
                    view.startAnimation(animation);
                    if (clickListener != null) {
                        clickListener.onCardClick(reportDto);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return reportDtoList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView reportImg;
        private TextView label;
        private TextView uploadedAt;
        private LinearLayout card;

        public ViewHolder(@NonNull View cardView) {
            super(cardView);

            reportImg = cardView.findViewById(R.id.reportCardImage);
            label = cardView.findViewById(R.id.reportCardText);
            uploadedAt = cardView.findViewById(R.id.reportCardUploadedDateTime);
            card = cardView.findViewById(R.id.reportCardLayout);
        }
    }

    public interface OnCardClickListener {
        void onCardClick(ReportDto reportDto);
    }

}
