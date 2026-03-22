package com.ecads.pulse.channeling.service;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.channeling.dto.*;
import com.ecads.pulse.channeling.entity.AvailableChannelingDays;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.channeling.repository.AvailableChannelingDaysRepository;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.common.enums.UserRole;
import com.ecads.pulse.common.exception.FirebaseFirestoreException;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.hospital.dto.HospitalApiDto;
import com.ecads.pulse.hospital.dto.HospitalDto;
import com.ecads.pulse.hospital.dto.LocationDto;
import com.ecads.pulse.platform.entity.PlatformFees;
import com.ecads.pulse.platform.repository.PlatformFeesRepository;
import com.ecads.pulse.telemedicine.dto.ChannelNameAndTokenDto;
import com.ecads.pulse.telemedicine.util.TeleMedicineHelper;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChannellingService {

    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final AvailableChannelingDaysRepository daysRepository;
    private final PlatformFeesRepository feesRepository;
    private final AppointmentRepository appointmentRepository;
    private final Firestore firestore;
    private final TeleMedicineHelper teleMedicineHelper;

    public ResponseEntity<List<ConsultantDTO>> fetchConsultants(
            Map<String, String> requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.get("hospitalPlaceId");
        List<ConsultantDTO> channellingConsultants = new ArrayList<>();

        // CONSULTANT SEARCH
        if (requestData.containsKey("consultant")) {
            String searchText = requestData.get("consultant").trim();
            if (!searchText.isEmpty()) {
                List<ConsultantDTO> consultantList = searchConsultantByName(searchText);

                Map<String, ConsultantDTO> consultantMap = new HashMap<>();

                if (consultantList != null && !consultantList.isEmpty()) {
                    for (ConsultantDTO dto : consultantList) {
                        consultantMap.put(dto.getUid(), dto);
                    }

                    // consultants fetched
                    List<ConsultantDTO> consultantResultList = filterConsultantsByHospitalPlaceId(hospitalPlaceId, consultantMap);

                    if (requestData.containsKey("speciality") &&
                            requestData.get("speciality") != null &&
                            !requestData.get("speciality").isEmpty()) {
                        if (consultantResultList != null) {
                            for (ConsultantDTO dto : consultantResultList) {
                                if (dto.getSpeciality().equalsIgnoreCase(requestData.get("speciality"))) {
                                    channellingConsultants.add(dto);
                                }
                            }
                        }
                    } else if (consultantResultList != null) {
                        channellingConsultants = consultantResultList;
                    }
                }
            }
        } else {
            // FETCH HOSPITAL CONSULTANTS
            List<ConsultantDTO> consultantResultList = filterConsultantsByHospitalPlaceId(hospitalPlaceId, null);

            if (requestData.containsKey("speciality") &&
                    requestData.get("speciality") != null &&
                    !requestData.get("speciality").isEmpty()) {
                if (consultantResultList != null) {
                    for (ConsultantDTO dto : consultantResultList) {
                        if (dto.getSpeciality().equalsIgnoreCase(requestData.get("speciality"))) {
                            channellingConsultants.add(dto);
                        }
                    }
                }
            } else if (consultantResultList != null) {
                channellingConsultants = consultantResultList;
            }
        }
        return ResponseEntity.ok(channellingConsultants);
    }

    public ResponseEntity<List<HospitalApiDto>> fetchHospitals(
            Map<String, String> requestData) {

        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String consultantUid = requestData.get("consultantUid");
        List<HospitalDto> channellingHospitals = new ArrayList<>();

        Map<String, HospitalDto> hospitalMap = null;

        // HOSPITAL SEARCH
        if (requestData.containsKey("hospital")) {
            String searchText = requestData.get("hospital").trim();

            if (!searchText.isEmpty()) {
                List<HospitalDto> hospitalList = searchHospitalsByName(searchText);

                hospitalMap = new HashMap<>();

                if (hospitalList != null && !hospitalList.isEmpty()) {
                    for (HospitalDto dto : hospitalList) {
                        hospitalMap.put(dto.getPlaceId(), dto);
                    }
                }
            }
        }

        // filter hospitals by consultant
        List<HospitalDto> hospitalResultList = filterHospitalsByConsultantUid(consultantUid, hospitalMap);

        if (hospitalResultList != null) {
            channellingHospitals = hospitalResultList;
        }

        return ResponseEntity.ok(convertHospitalsToApiDto(channellingHospitals));
    }

    public ResponseEntity<List<AvailableConsultantDto>> fetchConsultantsForHospitalList(
            List<HospitalApiDto> hospitals) {
        if (hospitals == null || hospitals.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, List<ConsultantDTO>> hospitalViseConsultants = new HashMap<>();
        Map<String, HospitalApiDto> hospitalMap = new HashMap<>();
        for (HospitalApiDto hospital : hospitals) {
            List<ConsultantDTO> consultants = filterConsultantsByHospitalPlaceId(hospital.getPlaceId(), null);
            if (consultants != null && !consultants.isEmpty()) {
                hospitalViseConsultants.put(hospital.getPlaceId(), consultants);
                hospitalMap.put(hospital.getPlaceId(), hospital);
            }
        }

        List<ConsultantDTO> selectedConsultants = new ArrayList<>();
        List<AvailableConsultantDto> availableConsultants = new ArrayList<>();
        for (Map.Entry<String, List<ConsultantDTO>> dto : hospitalViseConsultants.entrySet()) {
            if (selectedConsultants.size() >= 6) {
                break;
            }

            if (dto.getValue() != null && !dto.getValue().isEmpty()) {
                for (ConsultantDTO consultant : dto.getValue()) {

                    if (!selectedConsultants.contains(consultant)) {
                        ConsultantChannelingHospital channelingHospital = channellingHospitalRepository
                                .getByHospitalPlaceIdAndConsultantUid(dto.getKey(), consultant.getUid());

                        if (channelingHospital == null) {
                            log.debug("No channeling records found for consultant: {} in hospitalPlaceId {}", consultant.getName(), dto.getKey());
                            throw new ShowErrorException("No channeling records found");
                        }

                        List<String> availableSlots = getAvailableSlotsForConsultant(channelingHospital,
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                        if (!availableSlots.isEmpty()) {
                            availableConsultants.add(new AvailableConsultantDto(
                                    consultant,
                                    hospitalMap.get(dto.getKey()),
                                    availableSlots
                            ));
                            selectedConsultants.add(consultant);
                            break;
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok(availableConsultants);
    }

    public ResponseEntity<List<String>> fetchConsultantChannelingSlots(
            Map<String, String> requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.get("hospitalPlaceId");
        String consultantUid = requestData.get("consultantUid");
        String selectedDate = requestData.get("selectedDate");

        ConsultantChannelingHospital channellingRecord = channellingHospitalRepository.getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        if (channellingRecord != null && channellingRecord.getIsActive()) {
            List<String> availableList = getAvailableSlotsForConsultant(channellingRecord, selectedDate);
            return ResponseEntity.ok(availableList);
        } else {
            throw new ShowErrorException("This consultant currently not available for channelling");
        }
    }

    public ResponseEntity<List<String>> fetchConsultantAvailableDays(
            Map<String, String> requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.get("hospitalPlaceId");
        String consultantUid = requestData.get("consultantUid");

        ConsultantChannelingHospital channellingRecord = channellingHospitalRepository.getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        if (channellingRecord != null && channellingRecord.getIsActive()) {

            List<AvailableChannelingDays> channelingDays =
                    daysRepository.getAllByChannellingHospital_IdAndIsActive(channellingRecord.getId(), true);

            if (channelingDays != null && !channelingDays.isEmpty()) {

                List<String> availableDays = new ArrayList<>();

                for (AvailableChannelingDays day : channelingDays) {
                    if (day.getIsActive()) {
                        availableDays.add(day.getAvailableDay().name());
                    }
                }

                return ResponseEntity.ok(availableDays);

            } else {
                throw new ShowErrorException("This consultant currently not available for channeling");
            }
        } else {
            throw new ShowErrorException("This consultant currently not available for channeling");
        }
    }

    public ResponseEntity<Map<String, Double>> fetchPricingDetails(
            Map<String, String> requestData) {

        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.get("hospitalPlaceId");
        String consultantUid = requestData.get("consultantUid");

        ConsultantChannelingHospital channelingRecord = channellingHospitalRepository.
                getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        Map<String, Double> resultsMap = new HashMap<>();

        if (channelingRecord != null && channelingRecord.getIsActive()) {
            PlatformFees platformFees = feesRepository.getByIsActive(true);

            double consultantFee = channelingRecord.getConsultationFee();
            double hospitalFee = consultantFee * channelingRecord.getHospitalCommissionPercentage() / 100;
            double platformFee = consultantFee * platformFees.getPercentage() / 100;

            resultsMap.put("consultantFee", consultantFee);
            resultsMap.put("hospitalFee", hospitalFee);
            resultsMap.put("platformFee", platformFee);

            return ResponseEntity.ok(resultsMap);

        } else {
            throw new ShowErrorException("An error occurred while retrieving data");
        }
    }

    public ResponseEntity<List<UpComingAppointmentsDto>> fetchUpComingAppointments(
            SingleRequestDataDto requestData) {
        if (requestData.getData() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Appointment> appointmentList = appointmentRepository.getAllByPatientUidAndAppointmentStatus(requestData.getData(), AppointmentStatus.CONFIRMED);

//        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
//        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//
//        List<UpComingAppointmentsDto> dtoList = new ArrayList<>();
//
//        for (Appointment appointment : appointmentList) {
//
//            ConsultantDTO consultant = getConsultantByUid(appointment.getConsultantChannelingHospital().getConsultantUid());
//            if (consultant == null) {
//                log.debug("Appointment consultant not found, firestore fetching error");
//                throw new ShowErrorException("Consultant not found");
//            }
//
//            HospitalDto hospital = getHospitalByPlaceId(appointment.getConsultantChannelingHospital().getHospitalPlaceId());
//            if (hospital == null) {
//                log.debug("Appointment hospital not found, firestore fetching error");
//                throw new ShowErrorException("Hospital not found");
//            }
//
//            UpComingAppointmentsDto appointments = new UpComingAppointmentsDto(
//                    String.valueOf(appointment.getId()),
//                    consultant.getTitle() + " " + consultant.getName(),
//                    appointment.getAppointmentStartsAt().format(timeFormatter) + " - " + appointment.getAppointmentEndsAt().format(timeFormatter),
//                    hospital.getName(),
//                    appointment.getAppointmentDate().format(dateFormatter),
//                    hospital.getMapUrl(),
//                    appointment.getMeetingLink()
//            );
//
//            dtoList.add(appointments);
//        }

        List<UpComingAppointmentsDto> dtoList = getFormattedResponseAppointments(appointmentList);

        return ResponseEntity.ok(dtoList);
    }

    public ResponseEntity<ChannelNameAndTokenDto> fetchTeleMedChannelAndToken(
            TeleMedTokenReqDto requestData) {
        if (requestData.getPatientUid() == null || requestData.getAppointmentId() == null) {
            return ResponseEntity.badRequest().build();
        }

        Appointment appointment = appointmentRepository.getAppointmentById(Long.valueOf(requestData.getAppointmentId()));
        if (appointment == null) {
            throw new ShowErrorException("No such appointment found");
        }

        if (appointment.getPatientUid().equals(requestData.getPatientUid())) {
            ChannelNameAndTokenDto tokenDto = new ChannelNameAndTokenDto();

            if (appointment.getAppointmentDate().equals(LocalDate.now())
                    && !LocalTime.now().isBefore(appointment.getAppointmentStartsAt().minusMinutes(15))
                    && LocalTime.now().isBefore(appointment.getAppointmentEndsAt())) {

                String channelName = appointment.getMeetingLink();
                String channelToken = teleMedicineHelper.generateAgoraChannelToken(channelName, UserRole.PATIENT);

                tokenDto.setChannelName(channelName);
                tokenDto.setChannelToken(channelToken);
            }

            return ResponseEntity.ok(tokenDto);
        } else {
            throw new ShowErrorException("Patient id does not match");
        }
    }

    public ResponseEntity<SearchResultsDto> fetchSearchResults(
            SingleRequestDataDto requestData) {
        if (requestData == null || requestData.getData().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        //CONSULTANT SEARCH RESULTS
        List<ConsultantDTO> consultantList = searchConsultantByName(requestData.getData());

        List<AvailableConsultantDto> availableConsultants = new ArrayList<>();
        List<ConsultantDTO> selectedConsultants = new ArrayList<>();

        if (consultantList != null && !consultantList.isEmpty()) {
            Map<String, List<ConsultantChannelingHospital>> consultantRecords = new HashMap<>();
            Map<String, ConsultantDTO> consultantMap = new HashMap<>();

            for (ConsultantDTO consultant : consultantList) {
                List<ConsultantChannelingHospital> consultantBasesHospitals =
                        channellingHospitalRepository.getAllByConsultantUid(consultant.getUid());
                consultantRecords.put(consultant.getUid(), consultantBasesHospitals);
                consultantMap.put(consultant.getUid(), consultant);
            }

            for (Map.Entry<String, List<ConsultantChannelingHospital>> records : consultantRecords.entrySet()) {

                if (!selectedConsultants.contains(consultantMap.get(records.getKey())) && records.getValue() != null) {
                    for (ConsultantChannelingHospital channelingRecord : records.getValue()) {

                        List<String> availableSlots = getAvailableSlotsForConsultant(channelingRecord,
                                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

                        if (!availableSlots.isEmpty()) {

                            HospitalDto hospital = getHospitalByPlaceId(channelingRecord.getHospitalPlaceId());
                            if (hospital != null) {
                                HospitalApiDto apiDto = new HospitalApiDto(
                                        hospital.getPlaceId(),
                                        hospital.getImageUrl(),
                                        hospital.getName(),
                                        hospital.getLowerName(),
                                        hospital.getAddress(),
                                        hospital.getContact(),
                                        hospital.getEmail(),
                                        new LocationDto(hospital.getLocation().getLatitude(), hospital.getLocation().getLongitude()),
                                        hospital.getGeohash(),
                                        hospital.getMapUrl()
                                );

                                availableConsultants.add(new AvailableConsultantDto(
                                        consultantMap.get(records.getKey()),
                                        apiDto,
                                        availableSlots
                                ));
                                selectedConsultants.add(consultantMap.get(records.getKey()));
                                break;
                            }
                        }
                    }
                }
            }
        }

        //HOSPITAL SEARCH RESULTS
        List<HospitalDto> hospitalList = searchHospitalsByName(requestData.getData());
        List<HospitalApiDto> hospitalApiDtos = new ArrayList<>();

        if (hospitalList != null && !hospitalList.isEmpty()) {
            hospitalApiDtos = convertHospitalsToApiDto(hospitalList);
        }

        SearchResultsDto resultsDto = new SearchResultsDto(
                availableConsultants,
                hospitalApiDtos
        );

        return ResponseEntity.ok(resultsDto);
    }

    public ResponseEntity<List<UpComingAppointmentsDto>> fetchTodayAppointments(
            SingleRequestDataDto requestData) {
        if (requestData.getData() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Appointment> appointmentList = appointmentRepository
                .getAllByPatientUidAndAppointmentStatusAndAppointmentDate(
                        requestData.getData(), AppointmentStatus.CONFIRMED, LocalDate.now());

        List<UpComingAppointmentsDto> dtoList = getFormattedResponseAppointments(appointmentList);

        return ResponseEntity.ok(dtoList);
    }

    public ResponseEntity<List<AvailableConsultantDto>> fetchRecentlyBookedConsultants(
            SingleRequestDataDto requestData) {
        if (requestData.getData() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<Appointment> appointmentList = appointmentRepository
                .getAllByPatientUidAndAppointmentStatusIsNot(
                        requestData.getData(), AppointmentStatus.CANCELLED);

        Set<AvailableConsultantDto> suggestedList = new HashSet<>();
        suggestedList.addAll(getSuggestedConsultantDetails(appointmentList));

        if (suggestedList.isEmpty() || suggestedList.size() < 6) {
            List<Appointment> extraList = appointmentRepository
                    .getAllByAppointmentStatusIsNot(AppointmentStatus.CANCELLED,
                            PageRequest.of(0, 15));

            Set<AvailableConsultantDto> dtoList = getSuggestedConsultantDetails(extraList);

            for (AvailableConsultantDto dto : dtoList) {
                if (suggestedList.size() >= 6) {
                    break;
                }

                if (suggestedList.stream().noneMatch(
                        exDto -> exDto.getConsultant().getUid().equals(dto.getConsultant().getUid()))) {
                    suggestedList.add(dto);
                }
            }
        }

        return ResponseEntity.ok(suggestedList.stream().toList());
    }


    private Set<AvailableConsultantDto> getSuggestedConsultantDetails(List<Appointment> appointmentList) {
        Set<AvailableConsultantDto> resultDto = new HashSet<>();
        if (!appointmentList.isEmpty()) {

            Map<String, HospitalDto> hospitalMap = new HashMap<>();
            Map<String, ConsultantDTO> consultantMap = new HashMap<>();

            for (Appointment appointment : appointmentList) {
                if (appointment.getConsultantChannelingHospital().getIsActive()) {

                    String consultant = appointment.getConsultantChannelingHospital().getConsultantUid();
                    String hospital = appointment.getConsultantChannelingHospital().getHospitalPlaceId();

                    AvailableConsultantDto availableConsultantDto = new AvailableConsultantDto();
                    availableConsultantDto.setAvailableSlots(new ArrayList<>());

                    if (consultantMap.containsKey(consultant)) {
                        availableConsultantDto.setConsultant(consultantMap.get(consultant));
                    } else {
                        ConsultantDTO consultantDTO = getConsultantByUid(consultant);
                        consultantMap.put(consultant, consultantDTO);
                        availableConsultantDto.setConsultant(consultantDTO);
                    }

                    List<HospitalDto> hospitalDtoToConvert = new ArrayList<>();
                    if (hospitalMap.containsKey(hospital)) {
                        hospitalDtoToConvert.add(hospitalMap.get(hospital));
                        HospitalApiDto hospitalApiDto = convertHospitalsToApiDto(hospitalDtoToConvert).get(0);

                        availableConsultantDto.setHospital(hospitalApiDto);
                    } else {
                        HospitalDto hospitalDto = getHospitalByPlaceId(hospital);

                        hospitalDtoToConvert.add(hospitalDto);
                        HospitalApiDto hospitalApiDto = convertHospitalsToApiDto(hospitalDtoToConvert).get(0);

                        hospitalMap.put(hospital, hospitalDto);
                        availableConsultantDto.setHospital(hospitalApiDto);
                    }

                    if (resultDto.stream().noneMatch(exDto -> exDto.getConsultant().getUid().equals(consultant))) {
                        resultDto.add(availableConsultantDto);
                    }
                }
            }
        }

        return resultDto;
    }

    private List<UpComingAppointmentsDto> getFormattedResponseAppointments(
            List<Appointment> appointmentList) {

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        List<UpComingAppointmentsDto> dtoList = new ArrayList<>();

        for (Appointment appointment : appointmentList) {

            ConsultantDTO consultant = getConsultantByUid(appointment.getConsultantChannelingHospital().getConsultantUid());
            if (consultant == null) {
                log.debug("Appointment consultant not found, firestore fetching error");
                throw new ShowErrorException("Consultant not found");
            }

            HospitalDto hospital = getHospitalByPlaceId(appointment.getConsultantChannelingHospital().getHospitalPlaceId());
            if (hospital == null) {
                log.debug("Appointment hospital not found, firestore fetching error");
                throw new ShowErrorException("Hospital not found");
            }

            UpComingAppointmentsDto appointments = new UpComingAppointmentsDto(
                    String.valueOf(appointment.getId()),
                    consultant.getTitle() + " " + consultant.getName(),
                    appointment.getAppointmentStartsAt().format(timeFormatter) + " - " + appointment.getAppointmentEndsAt().format(timeFormatter),
                    hospital.getName(),
                    appointment.getAppointmentDate().format(dateFormatter),
                    hospital.getMapUrl(),
                    appointment.getMeetingLink()
            );

            dtoList.add(appointments);
        }

        return dtoList;
    }

    private List<ConsultantDTO> searchConsultantByName(String searchText) {
        if (!searchText.isEmpty()) {
            try {
                String lowerSearchText = searchText.trim().toLowerCase();

                QuerySnapshot querySnapshot = firestore.collection("consultants")
                        .orderBy("lowerName")
                        .startAt(lowerSearchText)
                        .endAt(lowerSearchText + "\uf8ff")
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    return querySnapshot.toObjects(ConsultantDTO.class);
                } else {
                    log.warn("Received querySnapshot for consultant search by name is empty");
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("Firestore data retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore data retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            log.warn("Search Text is empty");
            return null;
        }
    }

    private List<ConsultantDTO> getConsultantsByUids(List<String> uidList) {
        if (uidList != null && !uidList.isEmpty()) {
            try {
                QuerySnapshot querySnapshot = firestore.collection("consultants").whereIn("uid", uidList)
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    return querySnapshot.toObjects(ConsultantDTO.class);
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("Firestore consultantUid retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore consultantUid retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            return null;
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

    private HospitalDto getHospitalByPlaceId(String placeId) {
        if (placeId != null && !placeId.isEmpty()) {
            try {
                QuerySnapshot querySnapshot = firestore.collection("hospitals").whereEqualTo("placeId", placeId)
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    QueryDocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                    return documentSnapshot.toObject(HospitalDto.class);
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

    private List<ConsultantDTO> filterConsultantsByHospitalPlaceId(
            String placeId, Map<String, ConsultantDTO> consultantMap) {
        if (placeId != null && !placeId.isEmpty()) {
            List<ConsultantChannelingHospital> hospitalConsultants = channellingHospitalRepository.getAllByHospitalPlaceId(placeId);
            if (hospitalConsultants != null) {
                List<ConsultantDTO> resultList = new ArrayList<>();
//                List<Long> channelingHospitalIds = new ArrayList<>();

                if (consultantMap != null && !consultantMap.isEmpty()) {
                    for (ConsultantChannelingHospital consultant : hospitalConsultants) {
                        if (consultantMap.containsKey(consultant.getConsultantUid()) &&
                                consultant.getIsActive()) {
                            resultList.add(consultantMap.get(consultant.getConsultantUid()));
//                            channelingHospitalIds.add(consultant.getId());
                        }
                    }
                } else {
                    List<String> uidList = new ArrayList<>();
                    for (ConsultantChannelingHospital consultant : hospitalConsultants) {
                        if (consultant.getIsActive()) {
                            uidList.add(consultant.getConsultantUid());
                        }
//                        channelingHospitalIds.add(consultant.getId());
                    }

                    List<ConsultantDTO> consultantsByUids = getConsultantsByUids(uidList);
                    if (consultantsByUids != null) {
                        resultList = consultantsByUids;
//                        resultList = getConsultantsByUids(uidList);
                    }
                }

                // RETRIEVE AVAILABLE SLOTS
//                getAvailableSlots(channelingHospitalIds);


                return resultList;
            } else {
                // no records related to the placeId
                return null;
            }
        } else {
            throw new RuntimeException("No hospitalPlaceId found");
        }
    }

    private List<HospitalDto> filterHospitalsByConsultantUid(
            String consultantUid, Map<String, HospitalDto> hospitalMap) {

        if (consultantUid != null && !consultantUid.isEmpty()) {
            List<ConsultantChannelingHospital> consultantBasedHospitals = channellingHospitalRepository.getAllByConsultantUid(consultantUid);

            if (consultantBasedHospitals != null) {
                List<HospitalDto> resultList = new ArrayList<>();

                if (hospitalMap != null && !hospitalMap.isEmpty()) {
                    for (ConsultantChannelingHospital hospital : consultantBasedHospitals) {
                        if (hospitalMap.containsKey(hospital.getHospitalPlaceId()) &&
                                hospital.getIsActive()) {
                            resultList.add(hospitalMap.get(hospital.getHospitalPlaceId()));
                        }
                    }
                } else {
                    List<String> placeIdList = new ArrayList<>();
                    for (ConsultantChannelingHospital hospital : consultantBasedHospitals) {
                        if (hospital.getIsActive()) {
                            placeIdList.add(hospital.getHospitalPlaceId());
                        }
                    }

                    List<HospitalDto> hospitalsByPlaceIds = getHospitalsByPlaceIds(placeIdList);
                    if (hospitalsByPlaceIds != null) {
                        resultList = hospitalsByPlaceIds;
                    }
                }

                return resultList;
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("No hospitalPlaceId found");
        }
    }

    private List<HospitalDto> getHospitalsByPlaceIds(List<String> placeIdList) {
        if (placeIdList != null && !placeIdList.isEmpty()) {
            try {
                QuerySnapshot querySnapshot = firestore.collection("hospitals").whereIn("placeId", placeIdList)
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    return querySnapshot.toObjects(HospitalDto.class);
                } else {
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("Firestore hospital retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore hospital retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            return null;
        }
    }

    private List<HospitalDto> searchHospitalsByName(String searchText) {
        if (!searchText.isEmpty()) {
            try {
                String lowerSearchText = searchText.trim().toLowerCase();

                QuerySnapshot querySnapshot = firestore.collection("hospitals")
                        .orderBy("lowerName")
                        .startAt(lowerSearchText)
                        .endAt(lowerSearchText + "\uf8ff")
                        .get().get();

                if (!querySnapshot.isEmpty()) {
                    return querySnapshot.toObjects(HospitalDto.class);
                } else {
                    log.warn("Received hospital querySnapshot is empty");
                    return null;
                }
            } catch (InterruptedException e) {
                log.error("Firestore hospital data retrieval interrupted: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            } catch (ExecutionException e) {
                log.error("Firestore hospital data retrieval failed: {}", e.getMessage());
                throw new FirebaseFirestoreException("An error occurred while fetching data");
            }
        } else {
            log.warn("Hospital search Text is empty");
            return null;
        }
    }

    private List<HospitalApiDto> convertHospitalsToApiDto(List<HospitalDto> hospitalDtoList) {
        List<HospitalApiDto> returningList = new ArrayList<>();
        for (HospitalDto dto : hospitalDtoList) {
            HospitalApiDto apiDto = new HospitalApiDto(
                    dto.getPlaceId(),
                    dto.getImageUrl(),
                    dto.getName(),
                    dto.getLowerName(),
                    dto.getAddress(),
                    dto.getContact(),
                    dto.getEmail(),
                    new LocationDto(dto.getLocation().getLatitude(), dto.getLocation().getLongitude()),
                    dto.getGeohash(),
                    dto.getMapUrl()
            );
            returningList.add(apiDto);
        }
        return returningList;
    }

    private List<String> getAvailableSlotsForConsultant(
            ConsultantChannelingHospital channelingHospital, String date) {

        if (channelingHospital != null && channelingHospital.getId() > 0) {
            List<AvailableChannelingDays> channelingDays =
                    daysRepository.getAllByChannellingHospital_IdAndIsActive(channelingHospital.getId(), true);

            LocalDate selectedDate = LocalDate.parse(date);
            List<Appointment> existingAppointments = appointmentRepository
                    .getAppointmentByConsultantChannelingHospitalAndAppointmentDate(
                            channelingHospital, selectedDate);

            List<LocalTime> bookedSlots = new ArrayList<>();

            for (Appointment appointment : existingAppointments) {
                if (appointment.getAppointmentStatus() != AppointmentStatus.CANCELLED) {
                    if (!bookedSlots.contains(appointment.getAppointmentStartsAt())) {
                        bookedSlots.add(appointment.getAppointmentStartsAt());
                    }
                }
            }

            if (channelingDays != null && !channelingDays.isEmpty()) {
                DayOfWeek dayOfWeek = selectedDate.getDayOfWeek();

                List<String> slotList = new ArrayList<>();

                for (AvailableChannelingDays session : channelingDays) {
                    if (session.getAvailableDay().name().equalsIgnoreCase(dayOfWeek.toString())) {

                        List<LocalTime> availableSlots = new ArrayList<>();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");

                        int duration = session.getChannellingHospital().getDurationPerPatient().getMinutes();
                        LocalTime startsAt = session.getChannellingHospital().getSessionStartsAt();
                        LocalTime endsAt = session.getChannellingHospital().getSessionsEndsAt();

                        LocalTime slot = startsAt;

                        while (!slot.plusMinutes(duration).isAfter(endsAt)) {
                            if (!bookedSlots.contains(slot)) {
                                availableSlots.add(slot);
                            }
                            slot = slot.plusMinutes(duration);
                        }

                        for (LocalTime avSlot : availableSlots) {
                            slotList.add(formatter.format(avSlot));
                        }
                        break;
                    }
                }

                return slotList;
            } else {
                log.info("null status: {}", channelingDays == null);
                log.info("empty status: {}", channelingDays.isEmpty());
                throw new ShowErrorException("This consultant currently not available for channelling");
            }
        } else {
            throw new RuntimeException();
        }
    }

}
