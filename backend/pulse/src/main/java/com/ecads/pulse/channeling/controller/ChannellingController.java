package com.ecads.pulse.channeling.controller;

import com.ecads.pulse.channeling.dto.*;
import com.ecads.pulse.channeling.service.ChannellingService;
import com.ecads.pulse.common.dto.SingleRequestDataDto;
import com.ecads.pulse.hospital.dto.HospitalApiDto;
import com.ecads.pulse.telemedicine.dto.ChannelNameAndTokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channelling")
@Validated
@RequiredArgsConstructor
public class ChannellingController {

    private final ChannellingService service;

    @PostMapping("/upComingAppointments")
    public ResponseEntity<List<UpComingAppointmentsDto>> retrieveUpComingAppointments(
            @RequestBody SingleRequestDataDto requestData) {
        return service.fetchUpComingAppointments(requestData);
    }

    @PostMapping("/consultantsForHospitalList")
    public ResponseEntity<List<AvailableConsultantDto>> retrieveConsultantsForHospitalList(
            @RequestBody List<HospitalApiDto> hospitals) {
        return service.fetchConsultantsForHospitalList(hospitals);
    }

    @PostMapping("/hospitalBasedConsultants")
    public ResponseEntity<List<ConsultantDTO>> retrieveHospitalBasedConsultants(
            @RequestBody Map<String, String> requestData) {
        return service.fetchConsultants(requestData);
    }

    @PostMapping("/consultantBasedHospitals")
    public ResponseEntity<List<HospitalApiDto>> retrieveConsultantBasedHospitals(
            @RequestBody Map<String, String> requestData) {
        return service.fetchHospitals(requestData);
    }

    @PostMapping("/hospitalBasedConsultantChannelingSlots")
    public ResponseEntity<List<String>> retrieveConsultantChannellingSlots(
            @RequestBody Map<String, String> requestData) {
        return service.fetchConsultantChannelingSlots(requestData);
    }

    @PostMapping("/hospitalBasedConsultantChannelingDays")
    public ResponseEntity<List<String>> retrieveConsultantAvailableDays(
            @RequestBody Map<String, String> requestData) {
        return service.fetchConsultantAvailableDays(requestData);
    }

    @PostMapping("/pricingDetails")
    public ResponseEntity<Map<String, Double>> retrievePricingDetails(
            @RequestBody Map<String, String> requestData) {
        return service.fetchPricingDetails(requestData);
    }

    @PostMapping("/telemedChannelAndToken")
    public ResponseEntity<ChannelNameAndTokenDto> retrieveTeleMedChannelAndToken(
            @RequestBody TeleMedTokenReqDto requestData) {
        return service.fetchTeleMedChannelAndToken(requestData);
    }

    @PostMapping("/searchConsultantOrHospital")
    public ResponseEntity<SearchResultsDto> retrieveSearchResults(
            @RequestBody SingleRequestDataDto requestData) {
        return service.fetchSearchResults(requestData);
    }
}
