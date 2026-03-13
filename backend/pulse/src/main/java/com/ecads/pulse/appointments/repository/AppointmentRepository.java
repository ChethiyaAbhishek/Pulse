package com.ecads.pulse.appointments.repository;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> getAppointmentByConsultantChannelingHospitalAndAppointmentDate(ConsultantChannelingHospital consultantChannelingHospital, LocalDate appointmentDate);

    Appointment getAppointmentById(Long id);

    List<Appointment> getAllByAppointmentStatus(AppointmentStatus appointmentStatus);
}
