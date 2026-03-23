package com.nitin.saas.audit.service;

import com.nitin.saas.audit.dto.AuditLogResponse;
import com.nitin.saas.audit.entity.AuditLog;
import com.nitin.saas.audit.repository.AuditLogRepository;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.utils.IpAddressUtil;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.staff.enums.StaffRole;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final RBACService rbacService;
    private final BusinessService businessService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    /**
     * Core write method. Uses REQUIRES_NEW so the audit log entry is committed
     * independently of the calling transaction — even if the outer transaction
     * rolls back, the audit trail is preserved.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long businessId, String action,
                          String entityType, Long entityId, String details) {
        try {
            Long userId = resolveUserId();
            String actorType = resolveActorType();
            String actorPublicId = resolveActorPublicId(actorType, userId);
            HttpServletRequest request = currentRequest();

            persistAudit(
                businessId,
                userId,
                actorType,
                actorPublicId,
                action,
                entityType,
                entityId,
                details,
                request
            );

            log.debug("Audit: businessId={} action={} entityType={} entityId={}",
                businessId, action, entityType, entityId);

        } catch (Exception ex) {
            // Never let audit failures break the main flow
            log.error("Failed to write audit log: action={} entityType={} error={}",
                action, entityType, ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActionAsActor(Long businessId,
                                 Long actorUserId,
                                 String actorType,
                                 String actorPublicId,
                                 String action,
                                 String entityType,
                                 Long entityId,
                                 String details) {
        try {
            HttpServletRequest request = currentRequest();
            persistAudit(
                businessId,
                actorUserId,
                actorType,
                actorPublicId,
                action,
                entityType,
                entityId,
                details,
                request
            );

            log.debug("Audit: businessId={} action={} entityType={} entityId={}",
                    businessId, action, entityType, entityId);

        } catch (Exception ex) {
            // Never let audit failures break the main flow
            log.error("Failed to write audit log: action={} entityType={} error={}",
                    action, entityType, ex.getMessage(), ex);
        }
    }

    private void persistAudit(Long businessId,
                              Long userId,
                              String actorType,
                              String actorPublicId,
                              String action,
                              String entityType,
                              Long entityId,
                              String details,
                              HttpServletRequest request) {
            AuditLog entry = AuditLog.builder()
                .businessId(businessId)
                .userId(userId == null ? 0L : userId)
                .actorType(actorType)
                .actorPublicId(actorPublicId)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(request != null ? IpAddressUtil.getClientIp(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .build();

            auditLogRepository.save(entry);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStaffRemoval(Long businessId, Long staffId) {
        logAction(businessId, "STAFF_REMOVED", "STAFF", staffId,
                "Staff member removed from business");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSubscriptionCancellation(Long businessId, Long subscriptionId, String reason) {
        logAction(businessId, "SUBSCRIPTION_CANCELLED", "SUBSCRIPTION", subscriptionId,
                "Subscription cancelled. Reason: " + reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logManualPayment(Long businessId, Long paymentId, String amount, String method) {
        logAction(businessId, "MANUAL_PAYMENT_RECORDED", "PAYMENT", paymentId,
                String.format("Manual payment of %s via %s recorded", amount, method));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logMemberDeactivation(Long businessId, Long memberId, String memberName) {
        logAction(businessId, "MEMBER_DEACTIVATED", "MEMBER", memberId,
                "Member deactivated: " + memberName);
    }

    /**
     * Resolves the current user ID from the security context.
     * Returns 0 if called from a scheduled job or unauthenticated context.
     */
    private Long resolveUserId() {
        try {
            return rbacService.getCurrentUserId();
        } catch (Exception ex) {
            return 0L; // system/scheduled context
        }
    }

    private String resolveActorType() {
        if (rbacService.isMemberPrincipal()) {
            return "MEMBER";
        }
        if (rbacService.isAuthenticated()) {
            return "USER";
        }
        return "SYSTEM";
    }

    private String resolveActorPublicId(String actorType, Long userId) {
        if (userId == null || userId == 0L) {
            return null;
        }

        if ("MEMBER".equals(actorType)) {
            return memberRepository.findById(userId)
                    .map(m -> m.getPublicId())
                    .orElse(null);
        }

        if ("USER".equals(actorType)) {
            return userRepository.findById(userId)
                    .map(u -> u.getPublicId())
                    .orElse(null);
        }

        return null;
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }


    @Override
    public Page<AuditLogResponse> getBusinessAuditLogs(Long businessId, Pageable pageable) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_AUDIT_LOGS);
        return auditLogRepository
                .findByBusinessIdOrderByCreatedAtDesc(businessId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AuditLogResponse> getAuditLogsByEntityType(Long businessId, String entityType, Pageable pageable) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_AUDIT_LOGS);
        return auditLogRepository
                .findByBusinessIdAndEntityType(businessId, entityType, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<AuditLogResponse> getRecentAuditLogs(Long businessId, int days) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_AUDIT_LOGS);

        LocalDateTime since = LocalDateTime.now().minusDays(days);

        return auditLogRepository
                .findRecentLogs(businessId, since)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .logId(log.getLogId())
                .businessId(log.getBusinessId())
                .userId(log.getUserId())
                .actorType(log.getActorType())
                .actorPublicId(log.getActorPublicId())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .createdAt(log.getCreatedAt())
                .build();
    }
}