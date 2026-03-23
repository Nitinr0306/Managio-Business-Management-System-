package com.nitin.saas.audit.service;

import com.nitin.saas.audit.dto.AuditLogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface AuditLogService {

    void logAction(Long businessId, String action, String entityType,
                   Long entityId, String details);

    void logActionAsActor(Long businessId,
                          Long actorUserId,
                          String actorType,
                          String actorPublicId,
                          String action,
                          String entityType,
                          Long entityId,
                          String details);

    void logStaffRemoval(Long businessId, Long staffId);

    void logSubscriptionCancellation(Long businessId, Long subscriptionId, String reason);

    void logManualPayment(Long businessId, Long paymentId, String amount, String method);

    void logMemberDeactivation(Long businessId, Long memberId, String memberName);


    Page<AuditLogResponse> getBusinessAuditLogs(Long businessId, Pageable pageable);

    Page<AuditLogResponse> getAuditLogsByEntityType(Long businessId, String entityType, Pageable pageable);

    List<AuditLogResponse> getRecentAuditLogs(Long businessId, int days);
}