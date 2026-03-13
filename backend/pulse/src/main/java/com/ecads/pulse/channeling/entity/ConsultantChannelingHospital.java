package com.ecads.pulse.channeling.entity;

import com.ecads.pulse.channeling.enums.SlotDurations;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "hospital_has_consultants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsultantChannelingHospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hospital_place_id", nullable = false)
    private String hospitalPlaceId;

    @Column(name = "consultant_uid", nullable = false)
    private String consultantUid;

    @Column(name = "max_daily_slots", nullable = false)
    private Integer maxDailySlots;

    @Column(name = "session_starts_at", nullable = false)
    private LocalTime sessionStartsAt;

    @Column(name = "sessions_end_at", nullable = false)
    private LocalTime sessionsEndsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_per_patient", nullable = false)
    private SlotDurations durationPerPatient;

    @Column(name = "consultation_fee", nullable = false)
    private Double consultationFee;

    @Column(name = "hospital_commi_percent", nullable = false)
    private Double hospitalCommissionPercentage;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @PrePersist
    protected void onSave() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
