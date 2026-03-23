package com.nitin.saas.staff.service;

import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ConflictException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.staff.dto.*;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.entity.StaffPermission;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.repository.StaffPermissionRepository;
import com.nitin.saas.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final StaffRepository           staffRepository;
    private final StaffPermissionRepository permissionRepository;
    private final UserRepository            userRepository;
    private final BusinessRepository        businessRepository;
    private final BusinessService           businessService;
    private final RBACService               rbacService;
    private final AuditLogService           auditLogService;
    private final StaffSalaryService        staffSalaryService;

    // ── Add Staff ─────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse addStaff(Long businessId, CreateStaffRequest request) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.ADD_STAFF);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + request.getEmail()
                                + ". The user must register first."));

        if (staffRepository.existsByBusinessIdAndUserId(businessId, user.getId())) {
            throw new ConflictException("This user is already a staff member in this business");
        }

        if (request.getEmployeeId() != null
                && staffRepository.existsByBusinessIdAndEmployeeId(businessId, request.getEmployeeId())) {
            throw new ConflictException("Employee ID '" + request.getEmployeeId()
                    + "' is already used in this business");
        }

        LocalDate hireDate = request.getHireDate() != null ? request.getHireDate() : LocalDate.now();

        Staff staff = Staff.builder()
                .businessId(businessId)
                .userId(user.getId())
                .role(request.getRole())
                .status(Staff.StaffStatus.ACTIVE)
                .hireDate(hireDate)
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .salary(request.getSalary())
                .employeeId(request.getEmployeeId())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .notes(request.getNotes())
                .emergencyContact(request.getEmergencyContact())
                .canLogin(request.getCanLogin())
                .canManageMembers(request.getCanManageMembers())
                .canManagePayments(request.getCanManagePayments())
                .canManageSubscriptions(request.getCanManageSubscriptions())
                .canViewReports(request.getCanViewReports())
                .build();

        staff = staffRepository.save(staff);

        staffSalaryService.ensureMonthlyLedger(staff.getId(), LocalDate.now());

        businessRepository.findById(businessId).ifPresent(b -> {
            b.setStaffCount(b.getStaffCount() + 1);
            businessRepository.save(b);
        });

        auditLogService.logAction(businessId, "STAFF_ADDED", "STAFF", staff.getId(),
                String.format("Staff added: %s as %s", user.getEmail(), request.getRole()));

        log.info("Staff added: id={}, businessId={}, userId={}, role={}",
                staff.getId(), businessId, user.getId(), request.getRole());

        return mapToResponse(staff, user);
    }

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StaffResponse getStaffById(String staffIdentifier) {
        Staff staff = findActiveStaff(staffIdentifier);
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.VIEW_STAFF);
        User user = userRepository.findById(staff.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToResponse(staff, user);
    }

    @Transactional(readOnly = true)
    public StaffDetailResponse getStaffDetail(String staffIdentifier) {
        Staff staff = findActiveStaff(staffIdentifier);
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.VIEW_STAFF);
        User user = userRepository.findById(staff.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Business business = businessRepository.findById(staff.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        return mapToDetailResponse(staff, user, business);
    }

    /**
     * FIX CVH-009 / N+1: batch-loads all users in a single findAllById call.
     */
    @Transactional(readOnly = true)
    public Page<StaffResponse> getBusinessStaff(Long businessId, String search, String status, Pageable pageable) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_STAFF);

        Page<Staff> page;
        if (search != null && !search.isBlank()) {
            page = staffRepository.searchStaff(businessId, search.trim(), pageable);
        } else if (status != null && !status.isBlank()) {
            try {
                Staff.StaffStatus parsedStatus = Staff.StaffStatus.valueOf(status.trim().toUpperCase());
                page = staffRepository.findByBusinessIdAndStatus(businessId, parsedStatus, pageable);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid status filter: " + status);
            }
        } else {
            page = staffRepository.findActiveByBusinessId(businessId, pageable);
        }

        Map<Long, User> userMap = batchLoadUsers(page.getContent());
        return page.map(s -> mapToResponse(s, userMap.get(s.getUserId())));
    }

    /**
     * FIX CVH-009 / N+1: batch-loads all users in a single findAllById call.
     */
    @Transactional(readOnly = true)
    public List<StaffResponse> getBusinessStaffList(Long businessId) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_STAFF);

        List<Staff> staffList = staffRepository.findActiveByBusinessId(businessId);
        Map<Long, User> userMap = batchLoadUsers(staffList);

        return staffList.stream()
                .map(s -> mapToResponse(s, userMap.get(s.getUserId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> searchStaff(Long businessId, String query, Pageable pageable) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_STAFF);
        Page<Staff> page = staffRepository.searchStaff(businessId, query, pageable);
        Map<Long, User> userMap = batchLoadUsers(page.getContent());
        return page.map(s -> mapToResponse(s, userMap.get(s.getUserId())));
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> getStaffByStatus(Long businessId,
                                                Staff.StaffStatus status,
                                                Pageable pageable) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_STAFF);
        Page<Staff> page = staffRepository.findByBusinessIdAndStatus(businessId, status, pageable);
        Map<Long, User> userMap = batchLoadUsers(page.getContent());
        return page.map(s -> mapToResponse(s, userMap.get(s.getUserId())));
    }

    // ── Updates ───────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse updateStaff(String staffIdentifier, UpdateStaffRequest request) {
        Staff staff = findActiveStaff(staffIdentifier);
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.EDIT_STAFF);

        if (request.getRole() != null) {
            if (!rbacService.hasRole(Role.ADMIN) && !rbacService.hasRole(Role.SUPER_ADMIN)) {
                throw new AccessDeniedException("Only administrators can change staff roles");
            }
            staff.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            staff.setStatus(request.getStatus());
            if (request.getStatus() == Staff.StaffStatus.TERMINATED) {
                staff.setCanLogin(false);
                staff.setTerminationDate(LocalDate.now());
            }
        }
        if (request.getDepartment()      != null) staff.setDepartment(request.getDepartment());
        if (request.getDesignation()     != null) staff.setDesignation(request.getDesignation());
        if (request.getSalary()          != null) staff.setSalary(request.getSalary());
        if (request.getPhone()           != null) staff.setPhone(request.getPhone());
        if (request.getEmail()           != null) staff.setEmail(request.getEmail());
        if (request.getAddress()         != null) staff.setAddress(request.getAddress());
        if (request.getNotes()           != null) staff.setNotes(request.getNotes());
        if (request.getEmergencyContact() != null) staff.setEmergencyContact(request.getEmergencyContact());
        if (request.getCanLogin()              != null) staff.setCanLogin(request.getCanLogin());
        if (request.getCanManageMembers()      != null) staff.setCanManageMembers(request.getCanManageMembers());
        if (request.getCanManagePayments()     != null) staff.setCanManagePayments(request.getCanManagePayments());
        if (request.getCanManageSubscriptions() != null) staff.setCanManageSubscriptions(request.getCanManageSubscriptions());
        if (request.getCanViewReports()        != null) staff.setCanViewReports(request.getCanViewReports());

        staff = staffRepository.save(staff);

        if (request.getSalary() != null) {
            staffSalaryService.ensureMonthlyLedger(staff.getId(), LocalDate.now());
        }

        auditLogService.logAction(staff.getBusinessId(), "STAFF_UPDATED", "STAFF",
                staff.getId(), "Staff information updated");

        User user = userRepository.findById(staff.getUserId()).orElse(null);
        return mapToResponse(staff, user);
    }

    @Transactional
    public void terminateStaff(String staffIdentifier, LocalDate terminationDate) {
        Staff staff = findActiveStaff(staffIdentifier);
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.REMOVE_STAFF);

        staff.terminate(terminationDate != null ? terminationDate : LocalDate.now());
        staffRepository.save(staff);

        businessRepository.findById(staff.getBusinessId()).ifPresent(b -> {
            b.setStaffCount(Math.max(0, b.getStaffCount() - 1));
            businessRepository.save(b);
        });

        auditLogService.logAction(staff.getBusinessId(), "STAFF_TERMINATED",
                "STAFF", staff.getId(), "Staff terminated");
        log.info("Staff terminated: id={}", staff.getId());
    }

    @Transactional
    public void suspendStaff(String staffIdentifier) {
        Staff staff = findActiveStaff(staffIdentifier);
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.EDIT_STAFF);
        staff.suspend();
        staffRepository.save(staff);
        auditLogService.logAction(staff.getBusinessId(), "STAFF_SUSPENDED",
                "STAFF", staff.getId(), "Staff suspended");
    }

    @Transactional
    public void activateStaff(String staffIdentifier) {
        Staff staff = findActiveStaff(staffIdentifier);
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.EDIT_STAFF);
        staff.activate();
        staffRepository.save(staff);
        auditLogService.logAction(staff.getBusinessId(), "STAFF_ACTIVATED",
                "STAFF", staff.getId(), "Staff activated");
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    @Transactional
    public void grantPermission(String staffIdentifier, StaffRole.Permission permission) {
        Staff staff = findActiveStaff(staffIdentifier);
        Long staffId = staff.getId();
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.EDIT_STAFF);

        if (permissionRepository.existsByStaffIdAndPermission(staffId, permission)) {
            throw new ConflictException("Permission " + permission + " is already granted");
        }

        StaffPermission sp = StaffPermission.builder()
                .staffId(staffId)
                .permission(permission)
                .granted(true)
                .grantedBy(rbacService.getCurrentUserId())
                .build();
        permissionRepository.save(sp);

        auditLogService.logAction(staff.getBusinessId(), "PERMISSION_GRANTED",
                "STAFF_PERMISSION", sp.getId(),
                String.format("Permission %s granted to staff %d", permission, staffId));
    }

    @Transactional
    public void revokePermission(String staffIdentifier, StaffRole.Permission permission) {
        Staff staff = findActiveStaff(staffIdentifier);
        Long staffId = staff.getId();
        businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.EDIT_STAFF);

        permissionRepository.findByStaffIdAndPermission(staffId, permission).ifPresent(perm -> {
            permissionRepository.delete(perm);
            auditLogService.logAction(staff.getBusinessId(), "PERMISSION_REVOKED",
                    "STAFF_PERMISSION", perm.getId(),
                    String.format("Permission %s revoked from staff %d", permission, staffId));
        });
    }

    @Transactional(readOnly = true)
    public Set<StaffRole.Permission> getEffectivePermissions(String staffIdentifier) {
        Staff staff = findActiveStaff(staffIdentifier);
        Long staffId = staff.getId();
        if (rbacService.isAuthenticated()) {
            businessService.requireBusinessPermission(staff.getBusinessId(), StaffRole.Permission.VIEW_STAFF);
        }
        Set<StaffRole.Permission> permissions = new HashSet<>(staff.getRole().getPermissions());

        permissionRepository.findGrantedPermissions(staffId).stream()
                .map(StaffPermission::getPermission)
                .forEach(permissions::add);

        permissionRepository.findRevokedPermissions(staffId).stream()
                .map(StaffPermission::getPermission)
                .forEach(permissions::remove);

        return permissions;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(String staffIdentifier, StaffRole.Permission permission) {
        Set<StaffRole.Permission> effective = getEffectivePermissions(staffIdentifier);
        return effective.contains(StaffRole.Permission.ALL_PERMISSIONS)
                || effective.contains(permission);
    }

    @Transactional(readOnly = true)
    public Long countActiveStaff(Long businessId) {
        businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_STAFF);
        return staffRepository.countActiveStaff(businessId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Staff findActiveStaff(String staffIdentifier) {
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

    /**
     * Batch-loads all User records for a list of Staff in a single DB call.
     * Prevents N+1 in list endpoints.
     */
    private Map<Long, User> batchLoadUsers(List<Staff> staffList) {
        Set<Long> userIds = staffList.stream()
                .map(Staff::getUserId)
                .collect(Collectors.toSet());
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private StaffResponse mapToResponse(Staff staff, User user) {
        String businessPublicId = businessRepository.findById(staff.getBusinessId())
            .map(Business::getPublicId)
            .orElse(null);

        return StaffResponse.builder()
                .id(staff.getId())
            .publicId(staff.getPublicId())
                .businessId(staff.getBusinessId())
            .businessPublicId(businessPublicId)
                .userId(staff.getUserId())
            .userPublicId(user != null ? user.getPublicId() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userName(user != null ? user.getFullName() : null)
                .role(staff.getRole())
                .roleDisplay(staff.getRole().getDisplayName())
                .status(staff.getStatus())
                .statusDisplay(staff.getStatus().name())
                .hireDate(staff.getHireDate())
                .terminationDate(staff.getTerminationDate())
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .salary(staff.getSalary())
                .employeeId(staff.getEmployeeId())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .address(staff.getAddress())
                .notes(staff.getNotes())
                .canLogin(staff.getCanLogin())
                .canManageMembers(staff.getCanManageMembers())
                .canManagePayments(staff.getCanManagePayments())
                .canManageSubscriptions(staff.getCanManageSubscriptions())
                .canViewReports(staff.getCanViewReports())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }

    private StaffDetailResponse mapToDetailResponse(Staff staff, User user, Business business) {
        Set<StaffRole.Permission> effective = getEffectivePermissions(staff.getPublicId());
        List<StaffPermission> granted = permissionRepository.findGrantedPermissions(staff.getId());
        List<StaffPermission> revoked = permissionRepository.findRevokedPermissions(staff.getId());

        return StaffDetailResponse.builder()
                .id(staff.getId())
            .publicId(staff.getPublicId())
                .businessId(staff.getBusinessId())
            .businessPublicId(business.getPublicId())
                .businessName(business.getName())
                .userId(staff.getUserId())
            .userPublicId(user.getPublicId())
                .userEmail(user.getEmail())
                .userName(user.getFullName())
                .role(staff.getRole())
                .roleDisplay(staff.getRole().getDisplayName())
                .status(staff.getStatus())
                .statusDisplay(staff.getStatus().name())
                .hireDate(staff.getHireDate())
                .terminationDate(staff.getTerminationDate())
                .department(staff.getDepartment())
                .designation(staff.getDesignation())
                .salary(staff.getSalary())
                .employeeId(staff.getEmployeeId())
                .phone(staff.getPhone())
                .email(staff.getEmail())
                .address(staff.getAddress())
                .emergencyContact(staff.getEmergencyContact())
                .rolePermissions(staff.getRole().getPermissions())
                .grantedPermissions(granted.stream()
                        .map(StaffPermission::getPermission).collect(Collectors.toSet()))
                .revokedPermissions(revoked.stream()
                        .map(StaffPermission::getPermission).collect(Collectors.toSet()))
                .effectivePermissions(effective)
                .canLogin(staff.getCanLogin())
                .canManageMembers(staff.getCanManageMembers())
                .canManagePayments(staff.getCanManagePayments())
                .canManageSubscriptions(staff.getCanManageSubscriptions())
                .canViewReports(staff.getCanViewReports())
                .notes(staff.getNotes())
                .createdAt(staff.getCreatedAt())
                .updatedAt(staff.getUpdatedAt())
                .build();
    }
}