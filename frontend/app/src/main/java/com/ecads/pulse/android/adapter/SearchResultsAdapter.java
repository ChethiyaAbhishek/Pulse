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
import com.ecads.pulse.android.dto.AvailableConsultantDto;
import com.ecads.pulse.android.dto.HospitalDto;
import com.ecads.pulse.android.dto.SearchItemDto;
import com.ecads.pulse.android.dto.SearchResultDto;
import com.ecads.pulse.android.model.Hospital;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<SearchItemDto> searchItems;

    private OnResultClickListener clickListener;

    public SearchResultsAdapter(SearchResultDto searchResults, OnResultClickListener clickListener) {

        searchItems = new ArrayList<>();

        if (searchResults.getHospitalList() != null && !searchResults.getHospitalList().isEmpty()) {
            for (HospitalDto hospital : searchResults.getHospitalList()) {
                searchItems.add(new SearchItemDto(hospital));
            }
        }

        if (searchResults.getConsultantList() != null && !searchResults.getConsultantList().isEmpty()) {
            for (AvailableConsultantDto consultant : searchResults.getConsultantList()) {
                searchItems.add(new SearchItemDto(consultant));
            }
        }

        this.clickListener = clickListener;
    }

    @Override
    public int getItemViewType(int position) {
        SearchItemDto result = searchItems.get(position);
        return result.getType();
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == SearchItemDto.TYPE_CONSULTANT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.consultant_card, parent, false);
            return new ConsultantViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.hospital_card, parent, false);
            return new HospitalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ConsultantViewHolder) {
            ConsultantViewHolder viewHolder = (ConsultantViewHolder) holder;
            AvailableConsultantDto availableConsultantDto = searchItems.get(position).getConsultantDto();

            viewHolder.hospital.setText(availableConsultantDto.getHospital().getName());
            viewHolder.consultantName.setText(availableConsultantDto.getConsultant().getTitle() + " " + availableConsultantDto.getConsultant().getName());
            viewHolder.speciality.setText(availableConsultantDto.getConsultant().getSpeciality());
            viewHolder.baseHospital.setText(availableConsultantDto.getConsultant().getBaseHospital());

            viewHolder.slotChip1.setText(availableConsultantDto.getAvailableSlots().get(0));
            viewHolder.slotChip2.setText(availableConsultantDto.getAvailableSlots().get(1));

            viewHolder.cardLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    clickListener.onSearchResultClick(searchItems.get(position));
                }
            });



        } else if (holder instanceof HospitalViewHolder) {
            HospitalViewHolder viewHolder = (HospitalViewHolder) holder;
            HospitalDto hospital = searchItems.get(position).getHospitalDto();

            viewHolder.hospitalName.setText(hospital.getName());
            viewHolder.hospitalAddress.setText(hospital.getAddress());
            viewHolder.hospitalImg.setImageURI(Uri.parse(hospital.getImageUrl()));

            Glide.with(viewHolder.hospitalImg.getContext())
                    .load(hospital.getImageUrl())
                    .centerCrop()
                    .into(viewHolder.hospitalImg);

            viewHolder.contactBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + hospital.getContact()));
                view.getContext().startActivity(intent);
            });

            viewHolder.locationBtn.setOnClickListener(view -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(hospital.getMapUrl()));
                view.getContext().startActivity(intent);
            });

            viewHolder.hospitalCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.click_animation);
                    view.startAnimation(animation);
                    if (clickListener != null) {
                        clickListener.onSearchResultClick(searchItems.get(position));
                    }
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return searchItems.size();
    }

    public static class ConsultantViewHolder extends RecyclerView.ViewHolder {

        private TextView hospital;
        private TextView consultantName;
        private TextView speciality;
        private TextView baseHospital;
        private Chip slotChip1;
        private Chip slotChip2;
        private LinearLayout cardLayout;

        public ConsultantViewHolder(@NonNull View cardView) {
            super(cardView);
            hospital = cardView.findViewById(R.id.consultant_card_hospital);
            consultantName = cardView.findViewById(R.id.consultant_card_name);
            speciality = cardView.findViewById(R.id.consultant_card_speciality);
            baseHospital = cardView.findViewById(R.id.consultant_card_base_hospital);
            slotChip1 = cardView.findViewById(R.id.consultantCardSlot1);
            slotChip2 = cardView.findViewById(R.id.consultantCardSlot2);
            cardLayout = cardView.findViewById(R.id.consultantCardLayout);
        }
    }

    public static class HospitalViewHolder extends RecyclerView.ViewHolder {

        private TextView hospitalName;
        private TextView hospitalAddress;
        private ImageView hospitalImg;
        private ImageButton contactBtn;
        private ImageButton locationBtn;
        private RecyclerView consultantView;
        private LinearLayout hospitalCard;

        public HospitalViewHolder(@NonNull View cardView) {
            super(cardView);
            hospitalName = cardView.findViewById(R.id.hospital_card_name);
            hospitalAddress = cardView.findViewById(R.id.hospital_card_address);
            hospitalImg = cardView.findViewById(R.id.hospital_card_img);
            contactBtn = cardView.findViewById(R.id.hospital_card_call_btn);
            locationBtn = cardView.findViewById(R.id.hospital_card_location_btn);
            consultantView = cardView.findViewById(R.id.hospital_card_doc_availability_container);
            hospitalCard = cardView.findViewById(R.id.hospital_card_layout);
        }
    }

    public interface OnResultClickListener {
        void onSearchResultClick(SearchItemDto searchItem);
    }

}
