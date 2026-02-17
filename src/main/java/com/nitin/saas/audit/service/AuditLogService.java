package com.nitin.saas.audit.service;

import com.nitin.saas.audit.dto.AuditLogResponse;
import com.nitin.saas.audit.entity.AuditLog;
import com.nitin.saas.audit.repository.AuditLogRepository;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.service.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final BusinessService businessService;
    private final RBACService rbacService;

    @Transactional
    public void logAction(Long businessId, String action, String entityType, Long entityId, String details) {
        Long userId = rbacService.getCurrentUserId();

        AuditLog auditLog = AuditLog.builder()
                .businessId(businessId)
                .userId(userId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Audit log created: action={}, entityType={}, entityId={}", action, entityType, entityId);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getBusinessAuditLogs(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);
        return auditLogRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogsByEntityType(Long businessId, String entityType, Pageable pageable) {
        businessService.requireAccess(businessId);
        return auditLogRepository.findByBusinessIdAndEntityType(businessId, entityType, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecentAuditLogs(Long businessId, int days) {
        businessService.requireAccess(businessId);
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return auditLogRepository.findRecentLogs(businessId, since).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Specific audit log methods for critical actions
    @Transactional
    public void logStaffRemoval(Long businessId, Long staffId) {
        logAction(businessId, "STAFF_REMOVED", "STAFF", staffId, "Staff member removed from business");
    }

    @Transactional
    public void logSubscriptionCancellation(Long businessId, Long subscriptionId, String reason) {
        logAction(businessId, "SUBSCRIPTION_CANCELLED", "SUBSCRIPTION", subscriptionId,
                "Subscription cancelled. Reason: " + reason);
    }

    @Transactional
    public void logManualPayment(Long businessId, Long paymentId, String amount, String method) {
        logAction(businessId, "MANUAL_PAYMENT_RECORDED", "PAYMENT", paymentId,
                String.format("Manual payment of %s via %s", amount, method));
    }

    @Transactional
    public void logMemberDeactivation(Long businessId, Long memberId, String memberName) {
        logAction(businessId, "MEMBER_DEACTIVATED", "MEMBER", memberId,
                "Member deactivated: " + memberName);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .businessId(log.getBusinessId())
                .userId(log.getUserId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .createdAt(log.getCreatedAt())
                .build();
    }
}