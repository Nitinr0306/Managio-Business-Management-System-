package com.nitin.saas.audit.controller;

import com.nitin.saas.audit.dto.AuditLogResponse;
import com.nitin.saas.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Audit log management")
public class AuditLogController {

        private final AuditLogService auditLogService;

        @GetMapping
        @Operation(summary = "Get all audit logs for business")
        public ResponseEntity<Page<AuditLogResponse>> getBusinessAuditLogs(
                @PathVariable Long businessId,
                Pageable pageable) {
                Page<AuditLogResponse> response = auditLogService.getBusinessAuditLogs(businessId, pageable);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/entity/{entityType}")
        @Operation(summary = "Get audit logs by entity type")
        public ResponseEntity<Page<AuditLogResponse>> getAuditLogsByEntityType(
                @PathVariable Long businessId,
                @PathVariable String entityType,
                Pageable pageable) {
                Page<AuditLogResponse> response = auditLogService.getAuditLogsByEntityType(businessId, entityType, pageable);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/recent")
        @Operation(summary = "Get recent audit logs")
        public ResponseEntity<List<AuditLogResponse>> getRecentAuditLogs(
                @PathVariable Long businessId,
                @RequestParam(defaultValue = "7") int days) {
                List<AuditLogResponse> response = auditLogService.getRecentAuditLogs(businessId, days);
                return ResponseEntity.ok(response);
        }


}