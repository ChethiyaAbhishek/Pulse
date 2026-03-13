package com.ecads.pulse.payment.repository;

import com.ecads.pulse.payment.entity.AppointmentPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentPaymentsRepository extends JpaRepository<AppointmentPayment, Long> {
    AppointmentPayment getAppointmentPaymentByAppointmentId(Long appointmentId);
}
