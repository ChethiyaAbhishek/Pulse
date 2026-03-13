package com.ecads.pulse.payment.service;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.channeling.entity.ConsultantChannelingHospital;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.payment.dto.PayhereNotifyDto;
import com.ecads.pulse.payment.dto.PaymentDetailsDto;
import com.ecads.pulse.payment.entity.AppointmentPayment;
import com.ecads.pulse.payment.enums.PayhereStatusCode;
import com.ecads.pulse.payment.enums.PaymentMethod;
import com.ecads.pulse.payment.enums.PaymentStatus;
import com.ecads.pulse.payment.repository.AppointmentPaymentsRepository;
import com.ecads.pulse.platform.entity.PlatformFees;
import com.ecads.pulse.platform.repository.PlatformFeesRepository;
import com.google.cloud.firestore.Firestore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final Firestore firestore;
    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final PlatformFeesRepository feesRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentPaymentsRepository paymentsRepository;

    @Value("${payhere.merchant-id}")
    private String merchantId;
    @Value("${payhere.merchant-secret}")
    private String merchantSecret;

    public ResponseEntity<PaymentDetailsDto> fetchChannelingPaymentDetails(
            Map<String, String> requestData) {
        if (requestData == null || requestData.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String hospitalPlaceId = requestData.get("hospitalPlaceId");
        String consultantUid = requestData.get("consultantUid");

        ConsultantChannelingHospital channelingRecord = channellingHospitalRepository.
                getByHospitalPlaceIdAndConsultantUid(hospitalPlaceId, consultantUid);

        double totalAmount;

        if (channelingRecord != null && channelingRecord.getIsActive()) {
            PlatformFees platformFees = feesRepository.getByIsActive(true);

            double consultantFee = channelingRecord.getConsultationFee();
            double hospitalFee = consultantFee * channelingRecord.getHospitalCommissionPercentage() / 100;
            double platformFee = consultantFee * platformFees.getPercentage() / 100;

            totalAmount = consultantFee + hospitalFee + platformFee;
        } else {
            throw new ShowErrorException("An error occurred while retrieving data");
        }

        PaymentDetailsDto detailsDto = new PaymentDetailsDto(totalAmount, merchantId);

        return ResponseEntity.ok(detailsDto);
    }

    @Transactional
    public ResponseEntity<Void> bookAppointment(PayhereNotifyDto notifyDto) {
        if (notifyDto == null) {
            throw new ShowErrorException("Appointment booking failed");
        }

        Appointment appointment = appointmentRepository
                .getAppointmentById(Long.valueOf(notifyDto.getAppointmentId()));

        if (appointment == null) {
            throw new ShowErrorException("No appointment found");
        }

        if (verifyMd5(notifyDto)) {
            int statusCode = Integer.parseInt(notifyDto.getStatusCode());

            if (statusCode == PayhereStatusCode.SUCCESS.getCode()) {
                log.debug("Payhere payment successful");

                PlatformFees platformFees = feesRepository.getByIsActive(true);

                if (platformFees == null) {
                    throw new ShowErrorException("Platform fees record not found");
                }

                AppointmentPayment payment = new AppointmentPayment();
                payment.setAppointmentId(Long.valueOf(notifyDto.getAppointmentId()));
                payment.setAmount(Double.parseDouble(notifyDto.getAmount()));
                payment.setPaymentMethod(PaymentMethod.PAYHERE);
                payment.setPaymentId(notifyDto.getPaymentId());
                payment.setPaymentStatus(PaymentStatus.PAID);
                payment.setConsultantFee(appointment.getConsultantChannelingHospital().getConsultationFee());
                payment.setHospitalPercent(appointment.getConsultantChannelingHospital().getHospitalCommissionPercentage());
                payment.setPlatformPercent(platformFees.getPercentage());
                paymentsRepository.save(payment);

                appointment.setAppointmentStatus(AppointmentStatus.CONFIRMED);
                appointment.setPaymentStatus(PaymentStatus.PAID);

            } else if (statusCode == PayhereStatusCode.FAILED.getCode()) {
                log.debug("Payhere payment failed");
                appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                appointment.setPaymentStatus(PaymentStatus.FAILED);

            } else if (statusCode == PayhereStatusCode.CANCELLED.getCode()) {
                log.debug("Payhere payment cancelled");
                appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
                appointment.setPaymentStatus(PaymentStatus.CANCELLED);
            }

            return ResponseEntity.ok().build();
        } else {
            throw new ShowErrorException("Payhere payment hash does not match");
        }
    }

    private boolean verifyMd5(PayhereNotifyDto dto) {

        String secretHash = DigestUtils.md5Hex(this.merchantSecret).toUpperCase();

        String raw = dto.getMerchantId()
                + dto.getAppointmentId()
                + dto.getAmount()
                + dto.getCurrency()
                + dto.getStatusCode()
                + secretHash;

        String generatedHash = DigestUtils.md5Hex(raw).toUpperCase();

        return generatedHash.equals(dto.getMd5sig());
    }

}
