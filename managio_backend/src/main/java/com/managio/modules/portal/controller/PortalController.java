package com.managio.modules.portal.controller;

import com.managio.modules.portal.service.PortalAuthService;
import com.managio.modules.portal.service.PortalService;
import com.managio.modules.billing.dto.BillingDtos.InvoiceRow;
import com.managio.modules.billing.dto.BillingDtos.PaymentResponse;
import com.managio.platform.security.Principals.CustomerPrincipal;
import com.managio.platform.security.RefreshCookies;
import com.managio.platform.web.PageResponse;
import com.managio.modules.portal.service.PortalAuthService.SignedIn;
import com.managio.modules.portal.dto.PortalDtos.ChangePasswordBody;
import com.managio.modules.portal.dto.PortalDtos.PortalMembership;
import com.managio.modules.portal.dto.PortalDtos.PortalSession;
import com.managio.modules.portal.dto.PortalDtos.Profile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * What a signed-in customer sees (POR-3). Every query is scoped by the business and customer ID in the access
 * token; no endpoint takes a customer or business ID from the request.
 */
@RestController
@RequestMapping("/api/v1/portal")
@RequiredArgsConstructor
class PortalController {

    private final PortalService portal;
    private final PortalAuthService auth;
    private final RefreshCookies cookies;

    @GetMapping("/me")
    Profile me(@AuthenticationPrincipal CustomerPrincipal who) {
        return portal.profile(who);
    }

    @GetMapping("/memberships")
    List<PortalMembership> memberships(@AuthenticationPrincipal CustomerPrincipal who) {
        return portal.memberships(who);
    }

    @GetMapping("/invoices")
    PageResponse<InvoiceRow> invoices(@AuthenticationPrincipal CustomerPrincipal who,
                                      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return portal.invoices(who, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @GetMapping("/payments")
    PageResponse<PaymentResponse> payments(@AuthenticationPrincipal CustomerPrincipal who,
                                           @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return portal.payments(who, Math.max(page, 0), Math.clamp(size, 1, PageResponse.MAX_PAGE_SIZE));
    }

    @PostMapping("/me/password")
    ResponseEntity<PortalSession> changePassword(@AuthenticationPrincipal CustomerPrincipal who,
                                                 @Valid @RequestBody ChangePasswordBody body, HttpServletRequest http) {
        SignedIn signedIn = auth.changePassword(who.id(), body, http);
        return PortalAuthController.session(HttpStatus.OK, signedIn, cookies);
    }
}
