package com.managio.modules.portal.controller;

import com.managio.modules.portal.service.PortalAuthService;
import com.managio.modules.customers.domain.Customer;
import com.managio.modules.customers.service.CustomerService;
import com.managio.modules.auth.service.Sessions;
import com.managio.modules.auth.domain.SubjectType;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Staff turning a customer's portal access on or off (POR-2). */
@RestController
@RequestMapping("/api/v1/businesses/{businessId}/customers/{customerId}/portal")
@RequiredArgsConstructor
class PortalAccessController {

    private final CustomerService customers;
    private final PortalAuthService auth;
    private final Sessions sessions;

    /** Turns access on and emails the invitation; calling it again sends a fresh link. */
    @PostMapping("/invite")
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    @Transactional
    @ResponseStatus(HttpStatus.ACCEPTED)
    Map<String, Object> invite(BusinessContext ctx, @PathVariable long customerId) {
        Customer customer = customers.enablePortal(ctx, customerId);
        auth.sendInvitation(customer);
        return Map.of("portalEnabled", true, "sentTo", customer.getEmail());
    }

    /** Turns access off and ends every portal session the customer has. */
    @PostMapping("/disable")
    @RequirePermission(Permission.CUSTOMERS_MANAGE)
    @Transactional
    Map<String, Object> disable(BusinessContext ctx, @PathVariable long customerId) {
        Customer customer = customers.disablePortal(ctx, customerId);
        sessions.endAll(SubjectType.CUSTOMER, customer.getId());
        return Map.of("portalEnabled", false);
    }
}
