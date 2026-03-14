package com.ecads.pulse.android.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.dto.SearchResultDto;
import com.google.android.material.chip.Chip;

import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private List<SearchResultDto> searchResults;
    private OnResultClickListener clickListener;
    private static final int VIEW_TYPE_CONSULTANT = 1;
    private static final int VIEW_TYPE_HOSPITAL = 2;

    public SearchResultsAdapter(List<SearchResultDto> searchResults, OnResultClickListener clickListener) {
        this.searchResults = searchResults;
        this.clickListener = clickListener;
    }

    @Override
    public int getItemViewType(int position) {
        SearchResultDto result = searchResults.get(position);

        if (result.getConsultantDto() != null) {
            return VIEW_TYPE_CONSULTANT;
        } else {
            return VIEW_TYPE_HOSPITAL;
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

//        if (viewType == VIEW_TYPE_CONSULTANT) {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.consultant_card, parent, false);
//            return new ConsultantViewHolder(view);
//        }else {
//            View view = LayoutInflater.from(parent.getContext())
//                    .inflate(R.layout.hospital_card, parent, false);
////            return new HospitalViewHolder(view);
//        }

        return null;

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//
//        AvailableConsultantDto availableConsultantDto = availableConsultants.get(position);
//
//        holder.hospital.setText(availableConsultantDto.getHospital().getName());
//        holder.therapistName.setText(availableConsultantDto.getConsultant().getTitle() + " " + availableConsultantDto.getConsultant().getName());
//        holder.speciality.setText(availableConsultantDto.getConsultant().getSpeciality());
//        holder.baseHospital.setText(availableConsultantDto.getConsultant().getBaseHospital());
//
//        holder.slotChip1.setText(availableConsultantDto.getAvailableSlots().get(0));
//        holder.slotChip2.setText(availableConsultantDto.getAvailableSlots().get(1));

//        holder.cardLayout.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                clickListener.onAvailableConsultantClick(availableConsultantDto);
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return searchResults.size();
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

    public interface OnResultClickListener {
        void onSearchResultClick(SearchResultDto searchResultDto);
    }

}
