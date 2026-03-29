package com.ecads.pulse.platform.controller;

import com.ecads.pulse.platform.dto.HospitalAnalyticsWebResponseDto;
import com.ecads.pulse.platform.dto.PlatformFinancialSummaryWebResponseDto;
import com.ecads.pulse.platform.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform")
@Validated
@RequiredArgsConstructor
@CrossOrigin(origins = {
        "http://localhost:8082",
        "http://localhost:8081"
})
public class PlatformController {

    private final PlatformService service;

    @PostMapping("/fetchPlatformHospitalAnalytics")
    public ResponseEntity<HospitalAnalyticsWebResponseDto> retrievePlatformHospitalAnalytics() {
        return service.fetchPlatformHospitalAnalytics();
    }

    @PostMapping("/fetchFinancialSummary")
    public ResponseEntity<PlatformFinancialSummaryWebResponseDto> retrieveFinancialSummary() {
        return service.fetchFinancialSummary();
    }

}
