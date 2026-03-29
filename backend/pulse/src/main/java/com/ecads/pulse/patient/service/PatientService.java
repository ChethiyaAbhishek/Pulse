package com.ecads.pulse.patient.service;

import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.common.dto.UserDto;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.patient.dto.PatientDetailsWebResponseDto;
import com.ecads.pulse.patient.dto.PatientDto;
import com.ecads.pulse.patient.entity.Diagnosis;
import com.ecads.pulse.patient.repository.DiagnosisRepository;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PatientService {

    private final DiagnosisRepository diagnosisRepository;
    private final AppointmentRepository appointmentRepository;
    private final Firestore firestore;

    public ResponseEntity<PatientDetailsWebResponseDto> fetchPatientDetails(
            SingleRequestDataDto requestData) {
        if (requestData == null || requestData.getData().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String patientUid = requestData.getData();
        PatientDetailsWebResponseDto patientDetails = new PatientDetailsWebResponseDto();

        try {
            DocumentSnapshot snapshot = firestore.collection("users").document(patientUid).get().get();
            UserDto userDto = snapshot.toObject(UserDto.class);

            if (userDto != null) {

                LocalDate today = LocalDate.now();
                LocalDate parsed = LocalDate.parse(userDto.getDob(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                Period dob = Period.between(parsed, today);

                patientDetails.setName(userDto.getFname() + " " + userDto.getLname());
                patientDetails.setAge(String.valueOf(dob.getYears()));
                patientDetails.setGender(userDto.getGender());
                patientDetails.setPhone(userDto.getMobile());
                patientDetails.setEmail(userDto.getEmail());
                patientDetails.setAddress("");

                DocumentSnapshot details = firestore.collection("patients").document("PA_" + patientUid).get().get();
                PatientDto patientDto = details.toObject(PatientDto.class);

                if (patientDto != null) {
                    patientDetails.setEmergencyContact(patientDto.getEmerContact());
                    patientDetails.setCareGiverName(patientDto.getCgiverFname() + " " + patientDto.getCgiverLname());
                    patientDetails.setCareGiverPhone(patientDto.getCgiverContact());

                    List<Diagnosis> diagnoses = diagnosisRepository.getByPatientUid(patientUid);
                    if (diagnoses != null) {
                        patientDetails.setDiagnosesList(diagnoses);
                    }
                }
            }
        } catch (InterruptedException e) {
            log.error("Firestore data fetching error [Interrupted]: {}", e.getMessage());
            throw new ShowErrorException("Data fetching failed");
        } catch (ExecutionException e) {
            log.error("Firestore data fetching error [Execution Exception]: {}", e.getMessage());
            throw new ShowErrorException("Data fetching failed");
        }

        return ResponseEntity.ok(patientDetails);
    }


}
