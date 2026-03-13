package com.ecads.pulse.payment.entity;

import com.ecads.pulse.payment.enums.PaymentMethod;
import com.ecads.pulse.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointment_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_id", nullable = false)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "consultant_fee", nullable = false)
    private Double consultantFee;

    @Column(name = "hospital_percentage", nullable = false)
    private Double hospitalPercent;

    @Column(name = "platform_percentage", nullable = false)
    private Double platformPercent;

    @Column(name = "paid_at", updatable = false)
    private LocalDateTime paidAt;

    @PrePersist
    protected void onSave() {
        paidAt = LocalDateTime.now();
    }
}
