package com.nitin.saas.business.controller;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.business.dto.BusinessResponse;
import com.nitin.saas.business.dto.CreateBusinessRequest;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/businesses")
public class BusinessController {

    private final BusinessService businessService;
    private final UserRepository userRepository;

    public BusinessController(
            BusinessService businessService,
            UserRepository userRepository
    ) {
        this.businessService = businessService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<BusinessResponse> createBusiness(
            @RequestBody CreateBusinessRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        User owner = userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Business business = businessService.createBusiness(
                request.getName(),
                owner
        );

        return ResponseEntity.ok(BusinessResponse.from(business));
    }
}
