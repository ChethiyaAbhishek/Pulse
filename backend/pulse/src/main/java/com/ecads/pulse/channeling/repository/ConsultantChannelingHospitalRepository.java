package com.ecads.pulse.channeling.repository;

import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultantChannelingHospitalRepository extends JpaRepository<ConsultantChannelingHospital, Long> {
    List<ConsultantChannelingHospital> getAllByHospitalPlaceId(String hospitalPlaceId);

    ConsultantChannelingHospital getByHospitalPlaceIdAndConsultantUid(String hospitalPlaceId, String consultantUid);

    List<ConsultantChannelingHospital> getAllByConsultantUid(String consultantUid);

    ConsultantChannelingHospital getByHospitalPlaceId(String hospitalPlaceId);

    long countByIsActive(Boolean isActive);

    long countByIsActiveAndHospitalPlaceId(Boolean isActive, String hospitalPlaceId);
}
