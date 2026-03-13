package com.ecads.pulse.platform.repository;

import com.ecads.pulse.platform.entity.PlatformFees;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlatformFeesRepository extends JpaRepository<PlatformFees, Long> {
    PlatformFees getByIsActive(Boolean isActive);
}
