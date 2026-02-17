package com.nitin.saas.payment.controller;

import com.nitin.saas.common.export.CSVExportService;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.dto.PaymentMethodStats;
import com.nitin.saas.payment.dto.PaymentResponse;
import com.nitin.saas.payment.dto.RecordPaymentRequest;
import com.nitin.saas.payment.entity.Payment;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.payment.service.PaymentService;
import com.nitin.saas.payment.service.PaymentStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentStatsService paymentStatsService;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final CSVExportService csvExportService;

    @PostMapping
    @Operation(summary = "Record manual payment")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable Long businessId,
            @Valid @RequestBody RecordPaymentRequest request) {
        PaymentResponse response = paymentService.recordPayment(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all payments for business")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @PathVariable Long businessId,
            Pageable pageable) {
        Page<PaymentResponse> response = paymentService.getPaymentsByBusiness(businessId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    @Operation(summary = "Export all payments to CSV")
    public ResponseEntity<byte[]> exportPayments(@PathVariable Long businessId) {
        List<Payment> payments = paymentRepository.findRecentPayments(businessId,
                LocalDateTime.now().minusYears(10));
        List<Member> members = memberRepository.findActiveByBusinessId(businessId, Pageable.unpaged()).getContent();

        byte[] csv = csvExportService.exportPayments(payments, members);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "payments.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get payment history for member")
    public ResponseEntity<List<PaymentResponse>> getMemberPaymentHistory(
            @PathVariable Long memberId) {
        List<PaymentResponse> response = paymentService.getMemberPaymentHistory(memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get payment method statistics")
    public ResponseEntity<PaymentMethodStats> getPaymentMethodStats(@PathVariable Long businessId) {
        PaymentMethodStats response = paymentStatsService.getPaymentMethodStats(businessId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue/monthly")
    @Operation(summary = "Calculate monthly revenue")
    public ResponseEntity<BigDecimal> getMonthlyRevenue(@PathVariable Long businessId) {
        LocalDateTime since = LocalDateTime.now().minusMonths(1);
        BigDecimal revenue = paymentService.calculateRevenue(businessId, since);
        return ResponseEntity.ok(revenue);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent payments")
    public ResponseEntity<List<PaymentResponse>> getRecentPayments(
            @PathVariable Long businessId,
            @RequestParam(defaultValue = "7") int days) {
        List<PaymentResponse> response = paymentService.getRecentPayments(businessId, days);
        return ResponseEntity.ok(response);
    }
}