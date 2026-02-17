package com.nitin.saas.member.controller;

import com.nitin.saas.common.export.BulkImportService;
import com.nitin.saas.common.export.CSVExportService;
import com.nitin.saas.member.dto.CreateMemberRequest;
import com.nitin.saas.member.dto.MemberDetailResponse;
import com.nitin.saas.member.dto.MemberListItemResponse;
import com.nitin.saas.member.dto.MemberResponse;
import com.nitin.saas.member.dto.SubscriptionHistoryResponse;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.member.service.MemberProfileService;
import com.nitin.saas.member.service.MemberService;
import com.nitin.saas.payment.dto.PaymentResponse;
import com.nitin.saas.payment.service.PaymentService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member management")
public class MemberController {

    private final MemberService memberService;
    private final MemberProfileService memberProfileService;
    private final PaymentService paymentService;
    private final MemberRepository memberRepository;
    private final CSVExportService csvExportService;
    private final BulkImportService bulkImportService;

    @PostMapping
    @Operation(summary = "Add member")
    public ResponseEntity<MemberResponse> createMember(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateMemberRequest request) {
        MemberResponse response = memberService.createMember(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List members with pagination")
    public ResponseEntity<Page<MemberResponse>> getMembers(
            @PathVariable Long businessId,
            Pageable pageable) {
        Page<MemberResponse> response = memberService.getMembers(businessId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/with-subscriptions")
    @Operation(summary = "List members with subscription details")
    public ResponseEntity<Page<MemberListItemResponse>> getMembersWithSubscriptions(
            @PathVariable Long businessId,
            Pageable pageable) {
        Page<MemberListItemResponse> response = memberProfileService.getMembersWithSubscriptions(businessId, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export")
    @Operation(summary = "Export all members to CSV")
    public ResponseEntity<byte[]> exportMembers(@PathVariable Long businessId) {
        List<Member> members = memberRepository.findActiveByBusinessId(businessId, Pageable.unpaged()).getContent();
        byte[] csv = csvExportService.exportMembers(members);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "members.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
    }

    @GetMapping("/import-template")
    @Operation(summary = "Download CSV import template")
    public ResponseEntity<byte[]> getImportTemplate() {
        byte[] template = csvExportService.getMemberTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "member_import_template.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(template);
    }

    @PostMapping("/import")
    @Operation(summary = "Import members from CSV")
    public ResponseEntity<BulkImportService.BulkImportResult> importMembers(
            @PathVariable Long businessId,
            @RequestParam("file") MultipartFile file) throws Exception {

        BulkImportService.BulkImportResult result = bulkImportService.importMembers(businessId, file);

        // Save successful members
        if (!result.getMembers().isEmpty()) {
            memberRepository.saveAll(result.getMembers());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/search")
    @Operation(summary = "Search members by name or phone")
    public ResponseEntity<Page<MemberResponse>> searchMembers(
            @PathVariable Long businessId,
            @RequestParam String query,
            Pageable pageable) {
        Page<MemberResponse> response = memberService.searchMembers(businessId, query, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get members by status")
    public ResponseEntity<Page<MemberResponse>> getMembersByStatus(
            @PathVariable Long businessId,
            @PathVariable String status,
            Pageable pageable) {
        Page<MemberResponse> response = memberService.getMembersByStatus(businessId, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member by ID")
    public ResponseEntity<MemberResponse> getMemberById(@PathVariable Long id) {
        MemberResponse response = memberService.getMemberById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get complete member profile with subscription and payment history")
    public ResponseEntity<MemberDetailResponse> getMemberProfile(@PathVariable Long id) {
        MemberDetailResponse response = memberProfileService.getMemberProfile(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/subscription-history")
    @Operation(summary = "Get member subscription history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> getSubscriptionHistory(@PathVariable Long id) {
        List<SubscriptionHistoryResponse> response = memberProfileService.getSubscriptionHistory(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/payment-history")
    @Operation(summary = "Get member payment history")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(@PathVariable Long id) {
        List<PaymentResponse> response = paymentService.getMemberPaymentHistory(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit member")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody CreateMemberRequest request) {
        MemberResponse response = memberService.updateMember(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate member (soft delete)")
    public ResponseEntity<Void> deactivateMember(@PathVariable Long id) {
        memberService.deactivateMember(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    @Operation(summary = "Count total members")
    public ResponseEntity<Long> countMembers(@PathVariable Long businessId) {
        Long count = memberService.countMembers(businessId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/active")
    @Operation(summary = "Count active members")
    public ResponseEntity<Long> countActiveMembers(@PathVariable Long businessId) {
        Long count = memberService.countActiveMembers(businessId);
        return ResponseEntity.ok(count);
    }
}