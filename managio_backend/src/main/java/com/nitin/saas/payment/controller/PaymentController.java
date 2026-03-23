package com.nitin.saas.payment.controller;

import com.nitin.saas.business.service.BusinessService;
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
import com.nitin.saas.staff.enums.StaffRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment management")
public class PaymentController {

    private final PaymentService      paymentService;
    private final PaymentStatsService paymentStatsService;
    private final PaymentRepository   paymentRepository;
    private final MemberRepository    memberRepository;
    private final BusinessService     businessService;
    private final CSVExportService    csvExportService;

    @PostMapping
    @Operation(summary = "Record a manual payment")
    public ResponseEntity<PaymentResponse> recordPayment(
            @PathVariable Long businessId,
            @Valid @RequestBody RecordPaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.recordPayment(businessId, request));
    }

    @GetMapping
    @Operation(summary = "List all payments for a business (paginated)")
    public ResponseEntity<Page<PaymentResponse>> getPayments(
            @PathVariable Long businessId,
            @RequestParam(required = false) String paymentMethod,
            Pageable pageable) {
        return ResponseEntity.ok(paymentService.getPaymentsByBusiness(businessId, paymentMethod, pageable));
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Get payment history for a member")
    public ResponseEntity<List<PaymentResponse>> getMemberPaymentHistory(
            @PathVariable Long businessId,
            @PathVariable String memberId) {
        return ResponseEntity.ok(paymentService.getMemberPaymentHistory(memberId));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get payment method breakdown statistics")
    public ResponseEntity<PaymentMethodStats> getPaymentMethodStats(
            @PathVariable Long businessId) {
        return ResponseEntity.ok(paymentStatsService.getPaymentMethodStats(businessId));
    }

    @GetMapping("/revenue/monthly")
    @Operation(summary = "Calculate monthly revenue")
    public ResponseEntity<BigDecimal> getMonthlyRevenue(@PathVariable Long businessId) {
        return ResponseEntity.ok(
                paymentService.calculateRevenue(businessId, LocalDateTime.now().minusMonths(1)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent payments")
    public ResponseEntity<List<PaymentResponse>> getRecentPayments(
            @PathVariable Long businessId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(paymentService.getRecentPayments(businessId, days));
    }

    @GetMapping("/export")
    @Operation(summary = "Export payments to CSV")
    public ResponseEntity<byte[]> exportPayments(@PathVariable Long businessId) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.EXPORT_PAYMENTS);
        List<Payment> payments = paymentRepository.findRecentPayments(
                businessId, LocalDateTime.now().minusYears(10));
        List<Member> members = memberRepository
                .findActiveByBusinessId(businessId, Pageable.unpaged()).getContent();
        byte[] csv = csvExportService.exportPayments(payments, members);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("text/csv"));
        h.setContentDispositionFormData("attachment", "payments.csv");
        return ResponseEntity.ok().headers(h).body(csv);
    }
}