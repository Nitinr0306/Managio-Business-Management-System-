package com.nitin.saas.business.service;

import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.dto.BusinessResponse;
import com.nitin.saas.business.dto.CreateBusinessRequest;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.exception.BusinessAlreadyExistsException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final RBACService        rbacService;
    private final StaffRepository    staffRepository;

    @Transactional
    public BusinessResponse createBusiness(CreateBusinessRequest request) {
        Long userId = rbacService.getCurrentUserId();

        if (businessRepository.findByOwnerIdAndName(userId, request.getName()).isPresent()) {
            throw new BusinessAlreadyExistsException(
                    "You already have a business named: " + request.getName());
        }

        Business business = Business.builder()
                .ownerId(userId)
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status("ACTIVE")
                .memberCount(0)
                .staffCount(0)
                .build();

        business = businessRepository.save(business);
        log.info("Business created: id={}, owner={}", business.getId(), userId);
        return mapToResponse(business);
    }

    @Transactional(readOnly = true)
    public BusinessResponse getBusinessById(Long id) {
        requireAccess(id);
        return mapToResponse(findActiveById(id));
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> getMyBusinesses() {
        Long userId = rbacService.getCurrentUserId();
        return businessRepository.findActiveByOwnerId(userId).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public BusinessResponse updateBusiness(Long id, CreateBusinessRequest request) {
        Business business = findActiveById(id);
        requireOwner(business);
        Long userId = rbacService.getCurrentUserId();

        businessRepository.findByOwnerIdAndName(userId, request.getName())
                .filter(e -> !e.getId().equals(id))
                .ifPresent(d -> { throw new BusinessAlreadyExistsException(
                        "You already have another business named: " + request.getName()); });

        business.setName(request.getName());
        business.setType(request.getType());
        business.setDescription(request.getDescription());
        business.setAddress(request.getAddress());
        business.setCity(request.getCity());
        business.setState(request.getState());
        business.setCountry(request.getCountry());
        business.setPhone(request.getPhone());
        business.setEmail(request.getEmail());

        business = businessRepository.save(business);
        log.info("Business updated: {}", id);
        return mapToResponse(business);
    }

    @Transactional
    public void deleteBusiness(Long id) {
        Business business = findActiveById(id);
        requireOwner(business);
        business.setDeletedAt(LocalDateTime.now());
        business.setStatus("DELETED");
        businessRepository.save(business);
        log.info("Business soft-deleted: {}", id);
    }

    /**
     * Tenant gate — throws AccessDeniedException if the current principal is
     * neither the business owner nor an active staff member of this business.
     */
    public void requireAccess(Long businessId) {
        Business business = findActiveById(businessId);
        Long userId = rbacService.getCurrentUserId();

        if (business.getOwnerId().equals(userId)) return;

        boolean isActiveStaff = staffRepository
                .findByBusinessIdAndUserId(businessId, userId)
                .map(Staff::isActive)
                .orElse(false);

        if (isActiveStaff) return;

        throw new AccessDeniedException("Access denied to business: " + businessId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Business findActiveById(Long id) {
        return businessRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
    }

    private void requireOwner(Business b) {
        if (!b.getOwnerId().equals(rbacService.getCurrentUserId())) {
            throw new AccessDeniedException("Only the owner can perform this action");
        }
    }

    public BusinessResponse mapToResponse(Business b) {
        return BusinessResponse.builder()
                .id(b.getId()).ownerId(b.getOwnerId()).name(b.getName())
                .type(b.getType()).description(b.getDescription()).address(b.getAddress())
                .city(b.getCity()).state(b.getState()).country(b.getCountry())
                .phone(b.getPhone()).email(b.getEmail()).status(b.getStatus())
                .memberCount(b.getMemberCount()).staffCount(b.getStaffCount())
                .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt())
                .build();
    }
}