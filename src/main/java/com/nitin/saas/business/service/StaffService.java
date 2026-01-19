package com.nitin.saas.business.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.entity.BusinessMembership;
import com.nitin.saas.business.enums.BusinessRole;
import com.nitin.saas.business.repository.BusinessMembershipRepository;
import com.nitin.saas.common.audit.service.AuditService;
import org.springframework.stereotype.Service;

@Service
public class StaffService {

    private final BusinessMembershipRepository membershipRepository;
    private final BusinessAuthorizationService authorizationService;
    private final AuditService auditService ;

    public StaffService(
            BusinessMembershipRepository membershipRepository,
            BusinessAuthorizationService authorizationService,
            AuditService auditService
    ) {
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public void addStaff(
            Business business,
            User requester,
            User staffUser
    ) {

        authorizationService.authorizeOwnerOnly(business, requester);

        if (membershipRepository.existsByBusinessAndUser(business, staffUser)) {
            throw new IllegalStateException("User already part of business");
        }

        BusinessMembership staffMembership =
                new BusinessMembership(
                        business,
                        staffUser,
                        BusinessRole.STAFF
                );

        membershipRepository.save(staffMembership);
    }

    public void removeStaff(
            Business business,
            User requester,
            Long staffUserId
    ) {
        authorizationService.authorizeOwnerOnly(business, requester);

        BusinessMembership membership =
                membershipRepository.findByBusinessAndUserId(business, staffUserId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Staff not found")
                        );

        membership.deactivate();
        membershipRepository.save(membership);
        auditService.log(
                "REMOVE_STAFF",
                "BUSINESS_MEMBERSHIP",
                membership.getId(),
                requester.getId(),
                "Staff removed by owner"
        );
    }
}

