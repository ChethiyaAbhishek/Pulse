package com.ecads.pulse.payment.repository;

import com.ecads.pulse.payment.entity.AppointmentPayment;
import com.ecads.pulse.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentPaymentsRepository extends JpaRepository<AppointmentPayment, Long> {
    AppointmentPayment getAppointmentPaymentByAppointmentId(Long appointmentId);

    List<AppointmentPayment> getAllByPaymentStatusIsNot(PaymentStatus paymentStatus);

    List<AppointmentPayment> getAllByPaymentStatus(PaymentStatus paymentStatus);
}
