package com.nitin.saas.business.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.entity.BusinessMembership;
import com.nitin.saas.business.enums.BusinessRole;
import com.nitin.saas.business.exception.businessException.BusinessAlreadyExistsException;
import com.nitin.saas.business.repository.BusinessMembershipRepository;
import com.nitin.saas.business.repository.BusinessRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository membershipRepository;

    public BusinessService(
            BusinessRepository businessRepository,
            BusinessMembershipRepository membershipRepository
    ) {
        this.businessRepository = businessRepository;
        this.membershipRepository = membershipRepository;
    }

    public Business createBusiness(String name, User owner) {

        String slug = generateSlug(name);
        String code = generateCode();

        if (businessRepository.existsBySlug(slug)) {
            throw new BusinessAlreadyExistsException(slug);
        }

        Business business = businessRepository.save(
                new Business(name, slug, code,owner)
        );

        BusinessMembership membership =
                new BusinessMembership(
                        business,
                        owner,
                        BusinessRole.OWNER
                );

        membershipRepository.save(membership);

        return business;
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
    private String generateCode() {
        return "BUS-" + UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
    }

}
