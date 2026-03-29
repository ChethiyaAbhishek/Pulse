package com.ecads.pulse.patient.entity;

import com.ecads.pulse.patient.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_uid")
    private String patientUid;

    @Column(name = "stroke_date")
    private LocalDate strokeDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StrokeType strokeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "affected_side")
    private AffectedSide affectedSide;

    @Enumerated(EnumType.STRING)
    private StrokeLocation location;

    @Enumerated(EnumType.STRING)
    private NeurologicalDeficit deficit;

    @Enumerated(EnumType.STRING)
    private SecondaryComplication complication;

    private String comorbidity;

    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onSave() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
