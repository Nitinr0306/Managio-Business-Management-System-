package com.nitin.saas.member.controller;

import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.export.BulkImportService;
import com.nitin.saas.common.export.CSVExportService;
import com.nitin.saas.member.dto.*;
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
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/members")
@RequiredArgsConstructor
@Tag(name = "Members", description = "Member management")
public class MemberController {

    private final MemberService        memberService;
    private final MemberProfileService memberProfileService;
    private final PaymentService       paymentService;
    private final MemberRepository     memberRepository;
    private final BusinessRepository   businessRepository;
    private final BusinessService      businessService;
    private final CSVExportService     csvExportService;
    private final BulkImportService    bulkImportService;

    @PostMapping
    @Operation(summary = "Add a member manually")
    public ResponseEntity<MemberResponse> createMember(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.createMember(businessId, request));
    }

    @GetMapping
    @Operation(summary = "List members (paginated)")
    public ResponseEntity<Page<MemberResponse>> getMembers(
            @PathVariable Long businessId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(memberService.getMembers(businessId, search, status, pageable));
    }

    @GetMapping("/with-subscriptions")
    @Operation(summary = "List members with active subscription info")
    public ResponseEntity<Page<MemberListItemResponse>> getMembersWithSubscriptions(
            @PathVariable Long businessId, Pageable pageable) {
        return ResponseEntity.ok(
                memberProfileService.getMembersWithSubscriptions(businessId, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search members by name or phone")
    public ResponseEntity<Page<MemberResponse>> searchMembers(
            @PathVariable Long businessId,
            @RequestParam String query, Pageable pageable) {
        return ResponseEntity.ok(memberService.searchMembers(businessId, query, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Filter members by status")
    public ResponseEntity<Page<MemberResponse>> getMembersByStatus(
            @PathVariable Long businessId,
            @PathVariable String status, Pageable pageable) {
        return ResponseEntity.ok(memberService.getMembersByStatus(businessId, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a member by ID")
    public ResponseEntity<MemberResponse> getMemberById(
            @PathVariable Long businessId, @PathVariable String id) {
        return ResponseEntity.ok(memberService.getMemberById(id));
    }

    @GetMapping("/{id}/profile")
    @Operation(summary = "Get full member profile with subscription and payment history")
    public ResponseEntity<MemberDetailResponse> getMemberProfile(
            @PathVariable Long businessId, @PathVariable String id) {
        return ResponseEntity.ok(memberProfileService.getMemberProfile(id));
    }

    @GetMapping("/{id}/subscription-history")
    @Operation(summary = "Get member subscription history")
    public ResponseEntity<List<SubscriptionHistoryResponse>> getSubscriptionHistory(
            @PathVariable Long businessId, @PathVariable String id) {
        return ResponseEntity.ok(memberProfileService.getSubscriptionHistory(id));
    }

    @GetMapping("/{id}/payment-history")
    @Operation(summary = "Get member payment history")
    public ResponseEntity<List<PaymentResponse>> getPaymentHistory(
            @PathVariable Long businessId, @PathVariable String id) {
        return ResponseEntity.ok(paymentService.getMemberPaymentHistory(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member details")
    public ResponseEntity<MemberResponse> updateMember(
            @PathVariable Long businessId, @PathVariable String id,
            @Valid @RequestBody CreateMemberRequest request) {
        return ResponseEntity.ok(memberService.updateMember(id, request));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a member (soft delete)")
    public ResponseEntity<Void> deactivateMember(
            @PathVariable Long businessId, @PathVariable String id) {
        memberService.deactivateMember(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    @Operation(summary = "Count total members")
    public ResponseEntity<Long> countMembers(@PathVariable Long businessId) {
        return ResponseEntity.ok(memberService.countMembers(businessId));
    }

    @GetMapping("/count/active")
    @Operation(summary = "Count active members")
    public ResponseEntity<Long> countActiveMembers(@PathVariable Long businessId) {
        return ResponseEntity.ok(memberService.countActiveMembers(businessId));
    }

    @GetMapping("/export")
    @Operation(summary = "Export members to CSV")
    public ResponseEntity<byte[]> exportMembers(@PathVariable Long businessId) {
        businessService.requireAccess(businessId);
        List<Member> members = memberRepository
                .findActiveByBusinessId(businessId, Pageable.unpaged()).getContent();
        byte[] csv = csvExportService.exportMembers(members);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("text/csv"));
        h.setContentDispositionFormData("attachment", "members.csv");
        return ResponseEntity.ok().headers(h).body(csv);
    }

    @GetMapping("/import-template")
    @Operation(summary = "Download CSV import template")
    public ResponseEntity<byte[]> getImportTemplate() {
        byte[] template = csvExportService.getMemberTemplate();
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.parseMediaType("text/csv"));
        h.setContentDispositionFormData("attachment", "member_import_template.csv");
        return ResponseEntity.ok().headers(h).body(template);
    }

    @PostMapping("/import")
    @Operation(summary = "Import members from CSV file")
    public ResponseEntity<BulkImportService.BulkImportResult> importMembers(
            @PathVariable Long businessId,
            @RequestParam("file") MultipartFile file) throws Exception {
        businessService.requireAccess(businessId);
        BulkImportService.BulkImportResult result =
                bulkImportService.importMembers(businessId, file);
        if (!result.getMembers().isEmpty()) {
            memberRepository.saveAll(result.getMembers());
            businessRepository.findActiveById(businessId).ifPresent(business -> {
                for (int i = 0; i < result.getSuccessCount(); i++) business.incrementMemberCount();
                businessRepository.save(business);
            });
        }
        return ResponseEntity.ok(result);
    }
}