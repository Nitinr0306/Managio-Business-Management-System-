package com.nitin.saas.business.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.exception.businessException.BusinessAlreadyExistsException;
import com.nitin.saas.business.repository.BusinessRepository;
import org.springframework.stereotype.Service;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;

    public BusinessService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public Business createBusiness(String name, User owner) {

        String slug = generateSlug(name);

        if (businessRepository.existsBySlug(slug)) {
            throw new BusinessAlreadyExistsException(slug);
        }

        Business business = new Business(name, slug, owner);
        return businessRepository.save(business);
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
