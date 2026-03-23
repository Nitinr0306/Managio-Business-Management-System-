package com.nitin.saas.staff.service;

import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.staff.dto.MarkSalaryPaidRequest;
import com.nitin.saas.staff.dto.StaffSalaryPaymentResponse;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.entity.StaffSalaryPayment;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.repository.StaffRepository;
import com.nitin.saas.staff.repository.StaffSalaryPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffSalaryService {

    private final StaffRepository staffRepository;
    private final StaffSalaryPaymentRepository salaryPaymentRepository;
    private final BusinessService businessService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Transactional
    public void ensureMonthlyLedger(Long staffId, LocalDate salaryMonth) {
        Staff staff = staffRepository.findActiveById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        if (staff.getSalary() == null) {
            return;
        }

        LocalDate month = normalizeMonth(salaryMonth);

        salaryPaymentRepository.findByStaffIdAndSalaryMonth(staffId, month)
                .orElseGet(() -> salaryPaymentRepository.save(StaffSalaryPayment.builder()
                        .staffId(staffId)
                        .salaryMonth(month)
                        .monthlySalary(staff.getSalary())
                        .pendingAmount(staff.getSalary())
                        .paymentStatus(StaffSalaryPayment.PaymentStatus.UNPAID)
                        .build()));
    }

    @Transactional(readOnly = true)
    public List<StaffSalaryPaymentResponse> getMonthlyPayments(Long businessId, LocalDate salaryMonth) {
        requireSalaryReadAccess(businessId);

        LocalDate month = normalizeMonth(salaryMonth != null ? salaryMonth : LocalDate.now());

        return salaryPaymentRepository.findByBusinessIdAndSalaryMonth(businessId, month).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StaffSalaryPaymentResponse> getUnpaidPayments(Long businessId, LocalDate salaryMonth) {
        requireSalaryReadAccess(businessId);

        LocalDate month = salaryMonth != null ? normalizeMonth(salaryMonth) : null;

        return salaryPaymentRepository.findUnpaidByBusiness(businessId, month).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffSalaryPaymentResponse markSalaryPaid(Long businessId, String staffIdentifier, MarkSalaryPaidRequest request) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.RECORD_PAYMENTS);

        Staff staff = resolveStaff(staffIdentifier);
        Long staffId = staff.getId();

        if (!staff.getBusinessId().equals(businessId)) {
            throw new BadRequestException("Staff does not belong to this business");
        }

        if (staff.getSalary() == null) {
            throw new BadRequestException("Staff has no monthly salary configured");
        }

        LocalDate month = normalizeMonth(request.getSalaryMonth() != null ? request.getSalaryMonth() : LocalDate.now());
        BigDecimal paidAmount = request.getPaidAmount() != null ? request.getPaidAmount() : staff.getSalary();

        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Paid amount must be greater than zero");
        }

        StaffSalaryPayment ledger = salaryPaymentRepository
                .findByStaffIdAndSalaryMonth(staffId, month)
                .orElseGet(() -> StaffSalaryPayment.builder()
                        .staffId(staffId)
                        .salaryMonth(month)
                        .monthlySalary(staff.getSalary())
                        .paidAmount(BigDecimal.ZERO)
                        .pendingAmount(staff.getSalary())
                        .paymentStatus(StaffSalaryPayment.PaymentStatus.UNPAID)
                        .build());

        BigDecimal totalPaid = ledger.getPaidAmount().add(paidAmount);
        BigDecimal pending = ledger.getMonthlySalary().subtract(totalPaid);
        if (pending.compareTo(BigDecimal.ZERO) < 0) {
            pending = BigDecimal.ZERO;
        }

        ledger.setPaidAmount(totalPaid);
        ledger.setPendingAmount(pending);
        ledger.setPaidAt((request.getPaidAt() != null ? request.getPaidAt().atStartOfDay() : LocalDateTime.now()));
        ledger.setManuallyMarked(true);
        ledger.setNotes(request.getNotes());
        ledger.setPaymentStatus(pending.compareTo(BigDecimal.ZERO) == 0
                ? StaffSalaryPayment.PaymentStatus.PAID
                : StaffSalaryPayment.PaymentStatus.UNPAID);

        ledger = salaryPaymentRepository.save(ledger);

        auditLogService.logAction(
                businessId,
                "MARK_SALARY_PAID",
                "STAFF_SALARY",
                ledger.getId(),
                String.format("staffId=%d,month=%s,paid=%s,pending=%s,status=%s",
                        staffId,
                        month,
                        paidAmount,
                        ledger.getPendingAmount(),
                        ledger.getPaymentStatus())
        );

        return mapToResponse(ledger);
    }

    private Staff resolveStaff(String staffIdentifier) {
        String value = staffIdentifier == null ? "" : staffIdentifier.trim().toUpperCase(Locale.ROOT);
        if (value.isBlank()) {
            throw new BadRequestException("Staff identifier is required");
        }

        if (value.startsWith("STF-")) {
            return staffRepository.findActiveByPublicId(value)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffIdentifier));
        }

        try {
            Long id = Long.valueOf(value);
            return staffRepository.findActiveById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffIdentifier));
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid staff identifier format");
        }
    }

    private StaffSalaryPaymentResponse mapToResponse(StaffSalaryPayment payment) {
        Staff staff = staffRepository.findById(payment.getStaffId())
                .orElse(null);
        User user = (staff != null)
            ? userRepository.findById(staff.getUserId()).orElse(null)
            : null;

        String staffName = null;
        if (user != null && user.getFullName() != null && !user.getFullName().isBlank()) {
            staffName = user.getFullName();
        } else if (staff != null && staff.getEmail() != null && !staff.getEmail().isBlank()) {
            staffName = staff.getEmail();
        }

        return StaffSalaryPaymentResponse.builder()
                .id(payment.getId())
                .staffId(payment.getStaffId())
                .staffPublicId(staff != null ? staff.getPublicId() : null)
            .staffName(staffName)
                .employeeId(staff != null ? staff.getEmployeeId() : null)
                .salaryMonth(payment.getSalaryMonth())
                .monthlySalary(payment.getMonthlySalary())
                .paidAmount(payment.getPaidAmount())
                .pendingAmount(payment.getPendingAmount())
                .paymentStatus(payment.getPaymentStatus())
                .paidAt(payment.getPaidAt())
                .manuallyMarked(payment.getManuallyMarked())
                .notes(payment.getNotes())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }

    private LocalDate normalizeMonth(LocalDate date) {
        return LocalDate.of(date.getYear(), date.getMonth(), 1);
    }

    private void requireSalaryReadAccess(Long businessId) {
        try {
            businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_REPORTS);
            return;
        } catch (AccessDeniedException ignored) {
            // Fallback for payment managers who are allowed salary visibility in the staff portal.
        }
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_PAYMENTS);
    }
}
