package com.ecads.pulse.patient.entity;

import com.ecads.pulse.authentication.entity.User;
import com.ecads.pulse.patient.enums.AffectedSide;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

//@Entity
//@Table(name = "patients")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
public class Patient {
//
//    @Id
//    @Column(name = "user_uid", nullable = false, unique = true)
//    private String userUid;
//
//    @Column(name = "stroke_date", nullable = false)
//    private LocalDate strokeDate;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "affected_side")
//    private AffectedSide affectedSide;
//
//    @Column(name = "functional_level")
//    private String functionalLevel;
//
//    @Column(name = "caregiver_fname", nullable = false)
//    private String caregiverFirstName;
//
//    @Column(name = "caregiver_lname", nullable = false)
//    private String caregiverLastName;
//
//    @Column(name = "caregiver_mobile", nullable = false)
//    private String caregiverMobile;
//
//    @Column(name = "emergency_contact", nullable = false)
//    private String emergencyContact;
//
//    @Column(name = "medical_history")
//    private String medicalHistory;
//
//    @Column(name = "created_at", updatable = false)
//    private LocalDateTime createdAt;
//
//    @Column(name = "updated_at")
//    private LocalDateTime updatedAt;
//
//    @PrePersist
//    protected void onSave() {
//        createdAt = LocalDateTime.now();
//        updatedAt = LocalDateTime.now();
//    }
//
//    @PreUpdate
//    protected void onUpdate() {
//        updatedAt = LocalDateTime.now();
//    }

}
