package com.ecads.pulse.patient.entity;

import com.ecads.pulse.patient.enums.NeurologicalDeficit;
import com.ecads.pulse.patient.enums.SecondaryComplication;
import com.ecads.pulse.patient.enums.StrokeLocation;
import com.ecads.pulse.patient.enums.StrokeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "diagnosis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private StrokeType strokeType;

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
