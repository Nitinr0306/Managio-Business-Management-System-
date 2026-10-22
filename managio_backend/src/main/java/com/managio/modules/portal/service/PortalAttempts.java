package com.managio.modules.portal.service;

import com.managio.modules.customers.repository.CustomerRepository;
import com.managio.modules.auth.service.SignInAttempts;
import com.managio.modules.auth.domain.SubjectType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Counts failed portal sign-ins in their own transaction, so the count and lockout survive the failed request. */
@Component
@RequiredArgsConstructor
class PortalAttempts {

    private final CustomerRepository customers;
    private final SignInAttempts attempts;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void failed(Long customerId, String identifier, String detail, HttpServletRequest request) {
        if (customerId != null) {
            customers.findById(customerId).ifPresent(c ->
                    c.recordFailedPortalSignIn(clock.instant(), SignInAttempts.MAX_FAILED_ATTEMPTS, SignInAttempts.LOCKOUT));
        }
        attempts.record(SubjectType.CUSTOMER, customerId, identifier, "FAILURE", detail, request);
    }
}
