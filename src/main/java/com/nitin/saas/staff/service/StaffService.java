package com.nitin.saas.staff.service;

import com.nitin.saas.auth.entity.User;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final StaffRepository staffRepository;
    private final StaffPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessService businessService;
    private final RBACService rbacService;
    private final com.nitin.saas.audit.service.AuditLogService auditLogService;

    @Transactional
    public StaffResponse addStaff(Long businessId, CreateStaffRequest request) {
        businessService.requireAccess(businessId);

        // Validate user exists
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        // Check if user is already staff in this business
        if (staffRepository.existsByBusinessIdAndUserId(businessId, request.getUserId())) {
            throw new ConflictException("User is already staff in this business");
        }

        // Check if employee ID is unique
        if (request.getEmployeeId() != null &&
                staffRepository.existsByBusinessIdAndEmployeeId(businessId, request.getEmployeeId())) {
            throw new ConflictException("Employee ID already exists in this business");
        }

        Staff staff = Staff.builder()
                .businessId(businessId)
                .userId(request.getUserId())
                .role(request.getRole())
                .status(Staff.StaffStatus.ACTIVE)
                .hireDate(request.getHireDate())
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

        // Update business staff count
        businessRepository.findById(businessId).ifPresent(business -> {
            business.setStaffCount(business.getStaffCount() + 1);
            businessRepository.save(business);
        });

        // Audit log
        auditLogService.logAction(businessId, "STAFF_ADDED", "STAFF", staff.getId(),
                String.format("Staff added: %s as %s", user.getEmail(), request.getRole()));

        log.info("Staff added: id={}, businessId={}, userId={}, role={}",
                staff.getId(), businessId, request.getUserId(), request.getRole());

        return mapToResponse(staff, user);
    }

    @Transactional(readOnly = true)
    public StaffResponse getStaffById(Long id) {
        Staff staff = findActiveStaffById(id);
        businessService.requireAccess(staff.getBusinessId());

        User user = userRepository.findById(staff.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return mapToResponse(staff, user);
    }

    @Transactional(readOnly = true)
    public StaffDetailResponse getStaffDetail(Long id) {
        Staff staff = findActiveStaffById(id);
        businessService.requireAccess(staff.getBusinessId());

        User user = userRepository.findById(staff.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Business business = businessRepository.findById(staff.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return mapToDetailResponse(staff, user, business);
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> getBusinessStaff(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);

        return staffRepository.findActiveByBusinessId(businessId, pageable)
                .map(staff -> {
                    User user = userRepository.findById(staff.getUserId()).orElse(null);
                    return mapToResponse(staff, user);
                });
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getBusinessStaffList(Long businessId) {
        businessService.requireAccess(businessId);

        return staffRepository.findActiveByBusinessId(businessId).stream()
                .map(staff -> {
                    User user = userRepository.findById(staff.getUserId()).orElse(null);
                    return mapToResponse(staff, user);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> searchStaff(Long businessId, String query, Pageable pageable) {
        businessService.requireAccess(businessId);

        return staffRepository.searchStaff(businessId, query, pageable)
                .map(staff -> {
                    User user = userRepository.findById(staff.getUserId()).orElse(null);
                    return mapToResponse(staff, user);
                });
    }

    @Transactional(readOnly = true)
    public Page<StaffResponse> getStaffByStatus(Long businessId, Staff.StaffStatus status, Pageable pageable) {
        businessService.requireAccess(businessId);

        return staffRepository.findByBusinessIdAndStatus(businessId, status, pageable)
                .map(staff -> {
                    User user = userRepository.findById(staff.getUserId()).orElse(null);
                    return mapToResponse(staff, user);
                });
    }

    @Transactional
    public StaffResponse updateStaff(Long id, UpdateStaffRequest request) {
        Staff staff = findActiveStaffById(id);
        businessService.requireAccess(staff.getBusinessId());

        if (request.getRole() != null) {
            staff.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            staff.setStatus(request.getStatus());
            if (request.getStatus() == Staff.StaffStatus.TERMINATED) {
                staff.setCanLogin(false);
                staff.setTerminationDate(LocalDate.now());
            }
        }
        if (request.getDepartment() != null) {
            staff.setDepartment(request.getDepartment());
        }
        if (request.getDesignation() != null) {
            staff.setDesignation(request.getDesignation());
        }
        if (request.getSalary() != null) {
            staff.setSalary(request.getSalary());
        }
        if (request.getPhone() != null) {
            staff.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            staff.setEmail(request.getEmail());
        }
        if (request.getAddress() != null) {
            staff.setAddress(request.getAddress());
        }
        if (request.getNotes() != null) {
            staff.setNotes(request.getNotes());
        }
        if (request.getEmergencyContact() != null) {
            staff.setEmergencyContact(request.getEmergencyContact());
        }
        if (request.getCanLogin() != null) {
            staff.setCanLogin(request.getCanLogin());
        }
        if (request.getCanManageMembers() != null) {
            staff.setCanManageMembers(request.getCanManageMembers());
        }
        if (request.getCanManagePayments() != null) {
            staff.setCanManagePayments(request.getCanManagePayments());
        }
        if (request.getCanManageSubscriptions() != null) {
            staff.setCanManageSubscriptions(request.getCanManageSubscriptions());
        }
        if (request.getCanViewReports() != null) {
            staff.setCanViewReports(request.getCanViewReports());
        }

        staff = staffRepository.save(staff);

        auditLogService.logAction(staff.getBusinessId(), "STAFF_UPDATED", "STAFF", staff.getId(),
                "Staff information updated");

        log.info("Staff updated: id={}", id);

        User user = userRepository.findById(staff.getUserId()).orElse(null);
        return mapToResponse(staff, user);
    }

    @Transactional
    public void terminateStaff(Long id, LocalDate terminationDate) {
        Staff staff = findActiveStaffById(id);
        businessService.requireAccess(staff.getBusinessId());

        staff.terminate(terminationDate != null ? terminationDate : LocalDate.now());
        staffRepository.save(staff);

        // Update business staff count
        businessRepository.findById(staff.getBusinessId()).ifPresent(business -> {
            business.setStaffCount(Math.max(0, business.getStaffCount() - 1));
            businessRepository.save(business);
        });

        auditLogService.logAction(staff.getBusinessId(), "STAFF_TERMINATED", "STAFF", staff.getId(),
                "Staff terminated");

        log.info("Staff terminated: id={}", id);
    }

    @Transactional
    public void suspendStaff(Long id) {
        Staff staff = findActiveStaffById(id);
        businessService.requireAccess(staff.getBusinessId());

        staff.suspend();
        staffRepository.save(staff);

        auditLogService.logAction(staff.getBusinessId(), "STAFF_SUSPENDED", "STAFF", staff.getId(),
                "Staff suspended");

        log.info("Staff suspended: id={}", id);
    }

    @Transactional
    public void activateStaff(Long id) {
        Staff staff = findActiveStaffById(id);
        businessService.requireAccess(staff.getBusinessId());

        staff.activate();
        staffRepository.save(staff);

        auditLogService.logAction(staff.getBusinessId(), "STAFF_ACTIVATED", "STAFF", staff.getId(),
                "Staff activated");

        log.info("Staff activated: id={}", id);
    }

    @Transactional
    public void grantPermission(Long staffId, StaffRole.Permission permission) {
        Staff staff = findActiveStaffById(staffId);
        businessService.requireAccess(staff.getBusinessId());

        if (permissionRepository.existsByStaffIdAndPermission(staffId, permission)) {
            throw new ConflictException("Permission already granted");
        }

        Long grantedBy = rbacService.getCurrentUserId();

        StaffPermission staffPermission = StaffPermission.builder()
                .staffId(staffId)
                .permission(permission)
                .granted(true)
                .grantedBy(grantedBy)
                .build();

        permissionRepository.save(staffPermission);

        auditLogService.logAction(staff.getBusinessId(), "PERMISSION_GRANTED", "STAFF_PERMISSION",
                staffPermission.getId(), String.format("Permission %s granted to staff %d", permission, staffId));

        log.info("Permission granted: staffId={}, permission={}", staffId, permission);
    }

    @Transactional
    public void revokePermission(Long staffId, StaffRole.Permission permission) {
        Staff staff = findActiveStaffById(staffId);
        businessService.requireAccess(staff.getBusinessId());

        permissionRepository.findByStaffIdAndPermission(staffId, permission)
                .ifPresent(perm -> {
                    permissionRepository.delete(perm);
                    auditLogService.logAction(staff.getBusinessId(), "PERMISSION_REVOKED", "STAFF_PERMISSION",
                            perm.getId(), String.format("Permission %s revoked from staff %d", permission, staffId));
                });

        log.info("Permission revoked: staffId={}, permission={}", staffId, permission);
    }

    @Transactional(readOnly = true)
    public Set<StaffRole.Permission> getEffectivePermissions(Long staffId) {
        Staff staff = findActiveStaffById(staffId);

        Set<StaffRole.Permission> permissions = new HashSet<>(staff.getRole().getPermissions());

        // Add granted permissions
        List<StaffPermission> grantedPerms = permissionRepository.findGrantedPermissions(staffId);
        permissions.addAll(grantedPerms.stream()
                .map(StaffPermission::getPermission)
                .collect(Collectors.toSet()));

        // Remove revoked permissions
        List<StaffPermission> revokedPerms = permissionRepository.findRevokedPermissions(staffId);
        revokedPerms.forEach(perm -> permissions.remove(perm.getPermission()));

        return permissions;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Long staffId, StaffRole.Permission permission) {
        Set<StaffRole.Permission> effectivePermissions = getEffectivePermissions(staffId);
        return effectivePermissions.contains(StaffRole.Permission.ALL_PERMISSIONS) ||
                effectivePermissions.contains(permission);
    }

    @Transactional(readOnly = true)
    public Long countActiveStaff(Long businessId) {
        businessService.requireAccess(businessId);
        return staffRepository.countActiveStaff(businessId);
    }

    public void requireStaffAccess(Long businessId) {
        Long userId = rbacService.getCurrentUserId();
        staffRepository.findByBusinessIdAndUserId(businessId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "User is not staff in this business"));
    }

    public void requireStaffPermission(Long businessId, StaffRole.Permission permission) {
        Long userId = rbacService.getCurrentUserId();
        Staff staff = staffRepository.findByBusinessIdAndUserId(businessId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "User is not staff in this business"));

        if (!hasPermission(staff.getId(), permission)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Insufficient permissions: " + permission);
        }
    }

    private Staff findActiveStaffById(Long id) {
        return staffRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + id));
    }

    private StaffResponse mapToResponse(Staff staff, User user) {
        return StaffResponse.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .userId(staff.getUserId())
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
        Set<StaffRole.Permission> rolePermissions = staff.getRole().getPermissions();
        List<StaffPermission> grantedPerms = permissionRepository.findGrantedPermissions(staff.getId());
        List<StaffPermission> revokedPerms = permissionRepository.findRevokedPermissions(staff.getId());
        Set<StaffRole.Permission> effectivePermissions = getEffectivePermissions(staff.getId());

        return StaffDetailResponse.builder()
                .id(staff.getId())
                .businessId(staff.getBusinessId())
                .businessName(business.getName())
                .userId(staff.getUserId())
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
                .rolePermissions(rolePermissions)
                .grantedPermissions(grantedPerms.stream()
                        .map(StaffPermission::getPermission)
                        .collect(Collectors.toSet()))
                .revokedPermissions(revokedPerms.stream()
                        .map(StaffPermission::getPermission)
                        .collect(Collectors.toSet()))
                .effectivePermissions(effectivePermissions)
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