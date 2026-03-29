package com.ecads.pulse.platform.service;

import com.ecads.pulse.appointments.entity.Appointment;
import com.ecads.pulse.appointments.enums.AppointmentStatus;
import com.ecads.pulse.appointments.repository.AppointmentRepository;
import com.ecads.pulse.channeling.repository.ConsultantChannelingHospitalRepository;
import com.ecads.pulse.common.exception.ShowErrorException;
import com.ecads.pulse.hospital.dto.HospitalDto;
import com.ecads.pulse.payment.entity.AppointmentPayment;
import com.ecads.pulse.payment.enums.PaymentStatus;
import com.ecads.pulse.payment.repository.AppointmentPaymentsRepository;
import com.ecads.pulse.platform.dto.HospitalAnalyticsWebResponseDto;
import com.ecads.pulse.platform.dto.PlatformFinancialSummaryWebResponseDto;
import com.ecads.pulse.platform.entity.PlatformFees;
import com.ecads.pulse.platform.repository.PlatformFeesRepository;
import com.google.cloud.firestore.AggregateQuerySnapshot;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PlatformService {

    private final ConsultantChannelingHospitalRepository channellingHospitalRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentPaymentsRepository paymentsRepository;
    private final PlatformFeesRepository feesRepository;
    private final Firestore firestore;

    public ResponseEntity<HospitalAnalyticsWebResponseDto> fetchPlatformHospitalAnalytics() {

        try {
            CollectionReference hospitalCollection = firestore.collection("hospitals");

            AggregateQuerySnapshot hospitalSnapshot = hospitalCollection.count().get().get();
            long totalHospitalCount = hospitalSnapshot.getCount();

            AggregateQuerySnapshot consultantSnapshot = firestore.collection("consultants")
                    .count().get().get();
            long totalConsultantCount = consultantSnapshot.getCount();

            long totalAppointments = appointmentRepository.countByAppointmentStatusIsNot(
                    AppointmentStatus.CANCELLED);

            List<HospitalAnalyticsWebResponseDto.HospitalAnalyticsHospitalDto>
                    analyticsHospitalsList = new ArrayList<>();

            QuerySnapshot querySnapshot = hospitalCollection.limit(20).get().get();
            for (HospitalDto hospitalDto : querySnapshot.toObjects(HospitalDto.class)) {

                long activeHospitalConsultants = channellingHospitalRepository.countByIsActiveAndHospitalPlaceId(
                        true, hospitalDto.getPlaceId());

                List<Appointment> appointments = appointmentRepository
                        .getAllByConsultantChannelingHospital_HospitalPlaceId(hospitalDto.getPlaceId());

                double totalRevenue = 0.0;
                int totalHospitalAppointments = 0;

                if (appointments != null) {
                    for (Appointment appointment : appointments) {

                        if (appointment.getPaymentStatus() != PaymentStatus.CANCELLED
                                && appointment.getPaymentStatus() != PaymentStatus.REFUNDED) {

                            totalHospitalAppointments++;

                            AppointmentPayment payment = paymentsRepository
                                    .getAppointmentPaymentByAppointmentId(appointment.getId());

                            if (payment != null) {
                                double hospitalCharge = payment.getConsultantFee() * payment.getHospitalPercent() / 100;
                                totalRevenue += hospitalCharge;
                            }
                        }
                    }
                }

                HospitalAnalyticsWebResponseDto.HospitalAnalyticsHospitalDto analyticsHospitalDto =
                        new HospitalAnalyticsWebResponseDto.HospitalAnalyticsHospitalDto();
                analyticsHospitalDto.setHospitalPlaceId(hospitalDto.getPlaceId());
                analyticsHospitalDto.setName(hospitalDto.getName());
                analyticsHospitalDto.setAddress(hospitalDto.getAddress());
                analyticsHospitalDto.setConsultantCount(String.valueOf(activeHospitalConsultants));
                analyticsHospitalDto.setAppointmentsCount(String.valueOf(totalHospitalAppointments));
                analyticsHospitalDto.setTotalRevenue(String.valueOf(totalRevenue));

                analyticsHospitalsList.add(analyticsHospitalDto);
            }

            HospitalAnalyticsWebResponseDto responseDto = new HospitalAnalyticsWebResponseDto();
            responseDto.setTotalHospitalCount(String.valueOf(totalHospitalCount));
            responseDto.setTotalConsultants(String.valueOf(totalConsultantCount));
            responseDto.setTotalAppointments(String.valueOf(totalAppointments));
            responseDto.setHospitals(analyticsHospitalsList);

            return ResponseEntity.ok(responseDto);

        } catch (InterruptedException e) {
            throw new ShowErrorException("An error occurred while fetching hospital analytics");
        } catch (ExecutionException e) {
            throw new ShowErrorException("An error occurred while fetching hospital analytics");
        }
    }

    public ResponseEntity<PlatformFinancialSummaryWebResponseDto> fetchFinancialSummary() {

        try {
            AggregateQuerySnapshot hospitalSnapshot = firestore.collection("hospitals").count().get().get();
            long totalHospitalCount = hospitalSnapshot.getCount();

            List<AppointmentPayment> allPayments = paymentsRepository
                    .getAllByPaymentStatusIsNot(PaymentStatus.REFUNDED);

            double totalPlatformRevenue = 0.0;
            double totalCommission = 0.0;

            if (allPayments != null) {
                for (AppointmentPayment payment : allPayments) {
                    totalPlatformRevenue += payment.getAmount();

                    double platformCommission = payment.getConsultantFee() * payment.getPlatformPercent() / 100;
                    totalCommission += platformCommission;
                }
            }

            List<AppointmentPayment> allRefundedPayments = paymentsRepository
                    .getAllByPaymentStatus(PaymentStatus.REFUNDED);

            double totalRefundedAmount = 0.0;
            if(allRefundedPayments!=null){
                for(AppointmentPayment payment: allRefundedPayments){
                    totalRefundedAmount += payment.getAmount();
                }
            }

            PlatformFees platformFees = feesRepository.getByIsActive(true);

            double platformCommission = 0.0;
            if(platformFees!=null){
                platformCommission = platformFees.getPercentage();
            }

            double netPlatformRevenue = (totalPlatformRevenue * platformCommission) - totalRefundedAmount;

            long completedAppointments = appointmentRepository.countByAppointmentStatus(AppointmentStatus.COMPLETED);

            double avgAppointmentFee = totalPlatformRevenue / completedAppointments;

            PlatformFinancialSummaryWebResponseDto responseDto =
                    new PlatformFinancialSummaryWebResponseDto();

            responseDto.setTotalPlatformRevenue(String.valueOf(totalPlatformRevenue));
            responseDto.setTotalCommission(String.valueOf(totalCommission));
            responseDto.setTotalRefundedAmount(String.valueOf(totalRefundedAmount));
            responseDto.setNetPlatformRevenue(String.valueOf(netPlatformRevenue));
            responseDto.setAvgAppointmentFee(String.valueOf(avgAppointmentFee));
            responseDto.setHospitalCount(String.valueOf(totalHospitalCount));

            return ResponseEntity.ok(responseDto);

        } catch (InterruptedException e) {
            throw new ShowErrorException("An error occurred while fetching hospital analytics");
        } catch (ExecutionException e) {
            throw new ShowErrorException("An error occurred while fetching hospital analytics");
        }
    }

}
