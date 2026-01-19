package com.nitin.saas.business.service;


import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.entity.BusinessMembership;
import com.nitin.saas.business.enums.BusinessRole;
import com.nitin.saas.business.repository.BusinessMembershipRepository;
import org.springframework.stereotype.Service;

@Service
public class BusinessAuthorizationService {

    private final BusinessMembershipRepository membershipRepository;

    public BusinessAuthorizationService(
            BusinessMembershipRepository membershipRepository
    ) {
        this.membershipRepository = membershipRepository;
    }

    public void authorizeOwnerOrStaff(
            Business business,
            User user
    ) {
        BusinessMembership membership =
                membershipRepository.findByBusinessAndUser(business, user)
                        .orElseThrow(() ->
                                new IllegalStateException("User not part of business")
                        );

        if (membership.getRole() != BusinessRole.OWNER &&
                membership.getRole() != BusinessRole.STAFF) {
            throw new IllegalStateException("Not authorized for this action");
        }
    }

    public void authorizeOwnerOnly(
            Business business,
            User user
    ) {
        BusinessMembership membership =
                membershipRepository.findByBusinessAndUser(business, user)
                        .orElseThrow(() ->
                                new IllegalStateException("User not part of business")
                        );

        if (membership.getRole() != BusinessRole.OWNER) {
            throw new IllegalStateException("Only owner can perform this action");
        }
    }
}

