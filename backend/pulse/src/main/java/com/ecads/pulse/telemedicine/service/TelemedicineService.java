package com.ecads.pulse.telemedicine.service;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.authentication.repository.UserRepository;
import com.ecads.pulse.channeling.dto.ConsultantDTO;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.common.exception.FirebaseFirestoreException;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.common.util.MessagingService;
import com.ecads.pulse.telemedicine.dto.FirebasePrescriptionDto;
import com.ecads.pulse.telemedicine.dto.FirebasePrescriptionItemDto;
import com.ecads.pulse.telemedicine.dto.PrescriptionSaveDto;
import com.google.cloud.firestore.*;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelemedicineService {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final Firestore firestore;
    private final MessagingService messagingService;

    public ResponseEntity<Void> notifyPatientSessionStarted(
            SingleRequestDataDto requestData) {

        if (requestData == null || requestData.getData().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository
                .getAppointmentById(Long.valueOf(requestData.getData()));

        if (appointment != null) {

            Boolean pushNotification = messagingService.sendPushNotification(
                    appointment.getPatientUid(),
                    "Consultant is Waiting",
                    "Your video session has started. Please join now."
            );

            if(pushNotification){
                return ResponseEntity.ok().build();
            }else {
                log.error("Appointment session started notification sending failed");
                throw new ShowErrorException("Appointment session started notification sending failed");
            }

        } else {
            log.error("No appointment found");
            return ResponseEntity.badRequest().build();
        }
    }

    public ResponseEntity<Void> savePrescription(PrescriptionSaveDto requestData) {
        if (requestData == null || requestData.getAppointmentId().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.getAppointmentById(
                Long.valueOf(requestData.getAppointmentId()));

        if (appointment != null) {

            String consultantUid = appointment.getConsultantChannelingHospital().getConsultantUid();
            String patientUid = appointment.getPatientUid();

            ConsultantDTO consultant = getConsultantByUid(consultantUid);

            try {
                WriteBatch batch = firestore.batch();

                DocumentReference documentReference = firestore.collection("prescriptions").document();
                String prescriptionId = documentReference.getId();

                FirebasePrescriptionDto prescriptionDto = new FirebasePrescriptionDto(
                        requestData.getAppointmentId(),
                        consultant.getTitle() + " " + consultant.getName(),
                        consultantUid,
                        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                        requestData.getGeneralNote(),
                        "PA_"+patientUid,
                        prescriptionId,
                        "ACTIVE"
                );

                batch.set(documentReference, prescriptionDto);

                for (PrescriptionSaveDto.PrescriptionItemDto itemDto : requestData.getItems()) {
                    DocumentReference presItemRef = firestore.collection("prescriptionItems").document();

                    FirebasePrescriptionItemDto prescriptionItemDto = new FirebasePrescriptionItemDto(
                            itemDto.getDosage(),
                            itemDto.getDuration(),
                            itemDto.getFrequency(),
                            itemDto.getInstructions(),
                            itemDto.getMedicineName(),
                            prescriptionId
                    );

                    batch.set(presItemRef, prescriptionItemDto);
                }

                batch.commit().get();

                return ResponseEntity.ok().build();

            } catch (InterruptedException e) {
                log.error("Firestore prescription saving interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while prescription saving");
            } catch (ExecutionException e) {
                log.error("Firestore prescription saving failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while prescription saving");
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
    }


    private ConsultantDTO getConsultantByUid(String uid) {
        if (uid != null && !uid.isEmpty()) {
            try {
                QuerySnapshot querySnapshot = firestore.collection("consultants").whereEqualTo("uid", uid)
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    QueryDocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                    return documentSnapshot.toObject(ConsultantDTO.class);
                } else {
                    return null;
                }

            } catch (InterruptedException e) {
                log.error("Firestore consultant retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore consultant retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            return null;
        }
    }
}
