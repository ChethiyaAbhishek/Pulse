package com.ecads.pulse.channeling.entity;

import com.ecads.pulse.channeling.enums.Days;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "available_channelling_days")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableChannelingDays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hospital_has_consultants_id", nullable = false)
    private ConsultantChannelingHospital channellingHospital;

    @Enumerated(EnumType.STRING)
    @Column(name = "available_day", nullable = false)
    private Days availableDay;

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
