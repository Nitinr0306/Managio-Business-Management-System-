package com.nitin.saas.business.service;

import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.dto.BusinessResponse;
import com.nitin.saas.business.dto.CreateBusinessRequest;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.common.exception.BusinessAlreadyExistsException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final RBACService rbacService;

    @Transactional
    public BusinessResponse createBusiness(CreateBusinessRequest request) {
        Long userId = rbacService.getCurrentUserId();

        if(businessRepository.findByName(request.getName()).isPresent()){
            throw new BusinessAlreadyExistsException("Business with name: " + request.getName() + " already exists");

        }
        Business business = Business.builder()
                .ownerId(userId)
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .status("ACTIVE")
                .memberCount(0)
                .staffCount(0)
                .build();

        business = businessRepository.save(business);
        log.info("Business created: {} by user: {}", business.getId(), userId);

        return mapToResponse(business);
    }

    @Transactional(readOnly = true)
    public BusinessResponse getBusinessById(Long id) {
        Business business = findActiveBusinessById(id);
        return mapToResponse(business);
    }

    @Transactional(readOnly = true)
    public List<BusinessResponse> getMyBusinesses() {
        Long userId = rbacService.getCurrentUserId();
        return businessRepository.findActiveByOwnerId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BusinessResponse updateBusiness(Long id, CreateBusinessRequest request) {
        Business business = findActiveBusinessById(id);
        requireOwner(business);

        business.setName(request.getName());
        business.setAddress(request.getAddress());
        business.setPhone(request.getPhone());
        business.setEmail(request.getEmail());

        business = businessRepository.save(business);
        log.info("Business updated: {}", id);

        return mapToResponse(business);
    }

    @Transactional
    public void deleteBusiness(Long id) {
        Business business = findActiveBusinessById(id);
        requireOwner(business);

        business.setDeletedAt(LocalDateTime.now());
        business.setStatus("DELETED");
        businessRepository.save(business);

        log.info("Business soft deleted: {}", id);
    }

    public void requireAccess(Long businessId) {
        Business business = findActiveBusinessById(businessId);
        Long userId = rbacService.getCurrentUserId();

        if (!business.getOwnerId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied to business: " + businessId);
        }
    }

    private Business findActiveBusinessById(Long id) {
        return businessRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found: " + id));
    }

    private void requireOwner(Business business) {
        Long userId = rbacService.getCurrentUserId();
        if (!business.getOwnerId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only owner can perform this action");
        }
    }

    private BusinessResponse mapToResponse(Business business) {
        return BusinessResponse.builder()
                .id(business.getId())
                .ownerId(business.getOwnerId())
                .name(business.getName())
                .address(business.getAddress())
                .phone(business.getPhone())
                .email(business.getEmail())
                .status(business.getStatus())
                .memberCount(business.getMemberCount())
                .staffCount(business.getStaffCount())
                .createdAt(business.getCreatedAt())
                .updatedAt(business.getUpdatedAt())
                .build();
    }
}