package com.ecads.pulse.patient.repository;

import com.ecads.pulse.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
