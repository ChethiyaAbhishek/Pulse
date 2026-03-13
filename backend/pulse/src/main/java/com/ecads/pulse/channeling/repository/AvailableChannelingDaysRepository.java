package com.ecads.pulse.channeling.repository;

import com.ecads.pulse.channeling.entity.AvailableChannelingDays;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvailableChannelingDaysRepository extends JpaRepository<AvailableChannelingDays, Long> {
    List<AvailableChannelingDays> getAllByChannellingHospital_IdAndIsActive(Long channellingHospitalId, Boolean isActive);
}
