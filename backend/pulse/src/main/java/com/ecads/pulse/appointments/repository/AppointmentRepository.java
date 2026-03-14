package com.ecads.pulse.appointments.repository;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> getAppointmentByConsultantChannelingHospitalAndAppointmentDate(ConsultantChannelingHospital consultantChannelingHospital, LocalDate appointmentDate);

    Appointment getAppointmentById(Long id);

    List<Appointment> getAllByAppointmentStatus(AppointmentStatus appointmentStatus);

    List<Appointment> getAllByPatientUidAndPaymentStatusIsNot(String patientUid, PaymentStatus paymentStatus);

    List<Appointment> getAllByPatientUid(String patientUid);
}
