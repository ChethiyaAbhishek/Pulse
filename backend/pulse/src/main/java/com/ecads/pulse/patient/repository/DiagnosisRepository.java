package com.ecads.pulse.patient.repository;

import com.ecads.pulse.patient.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    List<Diagnosis> getByPatientUid(String patientUid);
}
