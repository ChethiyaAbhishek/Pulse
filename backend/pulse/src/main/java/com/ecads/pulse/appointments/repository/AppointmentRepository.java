package com.ecads.pulse.appointments.repository;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.payment.enums.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> getAppointmentByConsultantChannelingHospitalAndAppointmentDate(ConsultantChannelingHospital consultantChannelingHospital, LocalDate appointmentDate);

    Appointment getAppointmentById(Long id);

    List<Appointment> getAllByAppointmentStatus(AppointmentStatus appointmentStatus);

    List<Appointment> getAllByPatientUidAndPaymentStatusIsNot(String patientUid, PaymentStatus paymentStatus);

    List<Appointment> getAllByPatientUid(String patientUid);

    List<Appointment> getAllByPatientUidAndAppointmentStatus(String patientUid, AppointmentStatus appointmentStatus);

    List<Appointment> getAllByPatientUidAndPaymentStatusIsNot(String patientUid, PaymentStatus paymentStatus, Sort sort);

    List<Appointment> getAllByPatientUidAndAppointmentStatusAndAppointmentDate(String patientUid, AppointmentStatus appointmentStatus, LocalDate appointmentDate);

    List<Appointment> getAllByPatientUidAndAppointmentStatusIsNot(String patientUid, AppointmentStatus appointmentStatus);

    List<Appointment> getAllByAppointmentStatusIsNot(AppointmentStatus appointmentStatus, Pageable pageable);

    List<Appointment> getAllByConsultantChannelingHospitalAndAppointmentStatus(ConsultantChannelingHospital consultantChannelingHospital, AppointmentStatus appointmentStatus);

    List<Appointment> getAllByConsultantChannelingHospitalAndAppointmentStatusIsNot(ConsultantChannelingHospital consultantChannelingHospital, AppointmentStatus appointmentStatus);

    List<Appointment> getAllByConsultantChannelingHospital(ConsultantChannelingHospital consultantChannelingHospital, Pageable pageable);

    List<Appointment> getAllByConsultantChannelingHospital_HospitalPlaceId(String consultantChannelingHospitalHospitalPlaceId, Pageable pageable);

    long countByAppointmentStatusIsNot(AppointmentStatus appointmentStatus);

    List<Appointment> getAllByConsultantChannelingHospital_HospitalPlaceId(String consultantChannelingHospitalHospitalPlaceId);

    long countByAppointmentStatus(AppointmentStatus appointmentStatus);
}
