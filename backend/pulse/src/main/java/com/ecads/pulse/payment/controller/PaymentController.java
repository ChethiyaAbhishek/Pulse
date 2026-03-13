package com.ecads.pulse.payment.controller;

import com.ecads.pulse.payment.dto.PayhereNotifyDto;
import com.ecads.pulse.payment.dto.PaymentDetailsDto;
import com.ecads.pulse.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/payment")
@Validated
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService service;

    @PostMapping("/channeling")
    public ResponseEntity<PaymentDetailsDto> retrieveChannelingPaymentDetails(
            @RequestBody Map<String, String> requestData) {
        return service.fetchChannelingPaymentDetails(requestData);
    }

    @PostMapping("/notifyAppointment")
    public ResponseEntity<Void> notifyPayhereAppointment(
            @RequestParam("merchant_id") String merchantId,
            @RequestParam("order_id") String orderId,
            @RequestParam("payment_id") String paymentId,
            @RequestParam("payhere_amount") String amount,
            @RequestParam("payhere_currency") String currency,
            @RequestParam("status_code") String statusCode,
            @RequestParam("md5sig") String md5sig) {

        PayhereNotifyDto notifyDto = new PayhereNotifyDto(
                merchantId,
                orderId,
                paymentId,
                amount,
                currency,
                statusCode,
                md5sig
        );
        return service.bookAppointment(notifyDto);
    }

}
