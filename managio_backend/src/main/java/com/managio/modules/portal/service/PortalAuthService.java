package com.managio.modules.portal.service;

import com.managio.modules.customers.domain.Customer;
import com.managio.modules.customers.repository.CustomerRepository;
import com.managio.modules.customers.service.CustomerService;
import com.managio.modules.auth.service.OneTimeTokens;
import com.managio.modules.auth.domain.TokenPurpose;
import com.managio.modules.auth.service.Sessions;
import com.managio.modules.auth.service.SignInAttempts;
import com.managio.modules.auth.domain.SubjectType;
import com.managio.platform.config.ManagioProperties;
import com.managio.platform.mail.MailTemplates;
import com.managio.platform.mail.OutboundMail;
import com.managio.platform.security.AccessTokens;
import com.managio.platform.security.AccessTokens.IssuedToken;
import com.managio.platform.security.RateLimiter;
import com.managio.platform.security.RateLimiter.Policy;
import com.managio.platform.security.SecureTokens;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.modules.portal.dto.PortalDtos.BusinessPreview;
import com.managio.modules.portal.dto.PortalDtos.ChangePasswordBody;
import com.managio.modules.portal.dto.PortalDtos.CustomerSummary;
import com.managio.modules.portal.dto.PortalDtos.SignInBody;
import com.managio.modules.portal.dto.PortalDtos.SignUpBody;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Customer sign-in and recovery for one business's portal (POR-1, POR-2). It mirrors the staff flow in
 * {@code AuthService}, with the same lockout and rate limits, but its sessions are customer sessions and its
 * tokens can only open {@code /api/v1/portal}.
 */
@Service
@RequiredArgsConstructor
public class PortalAuthService {

    /** Compared against when the login is unknown, so both failure paths take the same time. */
    private static final String DUMMY_HASH = new BCryptPasswordEncoder(11).encode(SecureTokens.generate());

    private final CustomerRepository customers;
    private final CustomerService customerService;
    private final Sessions sessions;
    private final OneTimeTokens oneTimeTokens;
    private final PortalAttempts failures;
    private final SignInAttempts attempts;
    private final AccessTokens accessTokens;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final OutboundMail mail;
    private final ManagioProperties properties;
    private final JdbcClient jdbc;
    private final Clock clock;

    /** A successful sign-in: who, the access token, and the raw refresh token for the cookie. */
    public record SignedIn(Customer customer, BusinessPreview business, IssuedToken accessToken, String refreshToken) {
        public CustomerSummary summary() {
            return new CustomerSummary(customer.getId(), customer.getNumber(), customer.getFullName(), customer.getEmail(),
                    customer.getPhone());
        }
    }

    @Transactional(readOnly = true)
    public BusinessPreview preview(String businessCode) {
        return business(businessCode).map(Biz::preview).orElseThrow(() -> ApiException.notFound("Business"));
    }

    /** POR-1: business code, then email or phone, then password. One message for every kind of mismatch. */
    @Transactional
    public SignedIn signIn(SignInBody body, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.SIGN_IN, http);
        String identifier = identifier(body.identifier());
        Biz business = business(body.businessCode()).orElse(null);
        Customer customer = business == null ? null
                : customers.findPortalAccount(business.id(), identifier).orElse(null);
        if (customer == null) {
            passwordEncoder.matches(body.password(), DUMMY_HASH);
            failures.failed(null, identifier, business == null ? "unknown business" : "unknown login", http);
            throw invalidCredentials();
        }
        Instant now = clock.instant();
        if (customer.isArchived()) {
            attempts.record(SubjectType.CUSTOMER, customer.getId(), identifier, "FAILURE", "archived", http);
            throw invalidCredentials();
        }
        if (customer.isPortalLocked(now)) {
            attempts.record(SubjectType.CUSTOMER, customer.getId(), identifier, "FAILURE", "locked", http);
            long minutes = Math.max(1, now.until(customer.getLockedUntil(), ChronoUnit.MINUTES) + 1);
            throw new ApiException(HttpStatus.LOCKED, "auth.locked",
                    "Too many failed attempts. Try again in " + minutes + " minutes, or reset your password.");
        }
        if (customer.getPortalPasswordHash() == null
                || !passwordEncoder.matches(body.password(), customer.getPortalPasswordHash())) {
            failures.failed(customer.getId(), identifier, "wrong password", http);
            throw invalidCredentials();
        }
        attempts.record(SubjectType.CUSTOMER, customer.getId(), identifier, "SUCCESS", "password", http);
        return start(customer, business.preview(), http);
    }

    /** POR-2: open only when the business allows it; always creates a new customer (see {@code CustomerService.registerSelf}). */
    @Transactional
    public SignedIn signUp(SignUpBody body, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.SIGN_UP, http);
        Biz business = business(body.businessCode()).orElseThrow(() -> ApiException.notFound("Business"));
        if (!business.preview().signUpOpen()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "portal.signup_closed",
                    business.preview().name() + " doesn't take sign-ups online. Ask at the front desk for an invitation.");
        }
        Customer customer = customerService.registerSelf(business.id(),
                new Customer.Details(body.fullName(), body.phone(), body.email(), null, null, null, null, null, null),
                hashPassword(body.password()));
        attempts.record(SubjectType.CUSTOMER, customer.getId(), customer.getEmail() != null ? customer.getEmail() : customer.getPhone(),
                "SUCCESS", "portal sign-up", http);
        return start(customer, business.preview(), http);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public SignedIn refresh(String refreshToken, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.REFRESH, http);
        Sessions.Refreshed refreshed = sessions.refresh(SubjectType.CUSTOMER, refreshToken, http);
        Customer customer = customers.findById(refreshed.subjectId())
                .filter(c -> c.isPortalEnabled() && !c.isArchived())
                .orElseThrow(() -> {
                    sessions.end(refreshed.refreshToken());
                    return ApiException.unauthorized("auth.session_expired", "Your session has ended. Sign in again.");
                });
        BusinessPreview business = businessById(customer.getBusinessId()).preview();
        return new SignedIn(customer, business, accessTokens.forCustomer(customer.getId(), customer.getBusinessId()),
                refreshed.refreshToken());
    }

    @Transactional
    public void signOut(String refreshToken) {
        if (refreshToken != null) {
            sessions.end(refreshToken);
        }
    }

    /** Sends a reset link when the login belongs to a portal customer with an email; says nothing either way. */
    @Transactional
    public void requestReset(String businessCode, String rawIdentifier, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.EMAIL, http);
        String identifier = identifier(rawIdentifier);
        rateLimiter.check(Policy.EMAIL_PER_RECIPIENT, "portal-reset:" + String.valueOf(businessCode).trim()
                .toUpperCase(Locale.ROOT) + ":" + identifier);
        Biz business = business(businessCode).orElse(null);
        if (business == null) {
            return;
        }
        customers.findPortalAccount(business.id(), identifier)
                .filter(c -> !c.isArchived() && c.getEmail() != null)
                .ifPresent(c -> {
                    String token = oneTimeTokens.issue(TokenPurpose.PASSWORD_RESET, SubjectType.CUSTOMER, c.getId());
                    mail.send(MailTemplates.portalPasswordReset(c.getEmail(), c.getFullName(), business.preview().name(),
                            link(business.preview().code(), token)));
                });
    }

    /** Staff invitation (POR-2): issues the seven-day link and emails it. */
    @Transactional
    public void sendInvitation(Customer customer) {
        BusinessPreview business = businessById(customer.getBusinessId()).preview();
        String token = oneTimeTokens.issue(TokenPurpose.PORTAL_INVITATION, SubjectType.CUSTOMER, customer.getId());
        mail.send(MailTemplates.portalAccess(customer.getEmail(), customer.getFullName(), business.name(),
                link(business.code(), token)));
    }

    /** Sets the first or a new password from an invitation or reset link, ends old sessions and signs in. */
    @Transactional
    public SignedIn setPassword(String token, String password, HttpServletRequest http) {
        String hash = hashPassword(password);
        long customerId = oneTimeTokens.redeemAny(Set.of(TokenPurpose.PORTAL_INVITATION, TokenPurpose.PASSWORD_RESET), SubjectType.CUSTOMER, token);
        Customer customer = customers.findById(customerId).filter(c -> c.isPortalEnabled() && !c.isArchived())
                .orElseThrow(() -> ApiException.rule("token.invalid", "This link has expired or was already used. Request a new one."));
        customer.setPortalPassword(hash, clock.instant());
        sessions.endAll(SubjectType.CUSTOMER, customerId);
        attempts.record(SubjectType.CUSTOMER, customerId, customer.getEmail(), "SUCCESS", "password set", http);
        return start(customer, businessById(customer.getBusinessId()).preview(), http);
    }

    /** Needs the current password; ends every other session and starts a fresh one. */
    @Transactional
    public SignedIn changePassword(long customerId, ChangePasswordBody body, HttpServletRequest http) {
        Customer customer = customers.findById(customerId).orElseThrow(() -> ApiException.notFound("Account"));
        if (customer.getPortalPasswordHash() == null
                || !passwordEncoder.matches(body.currentPassword(), customer.getPortalPasswordHash())) {
            throw ApiException.invalidField("currentPassword", "This isn't your current password");
        }
        customer.setPortalPassword(hashPassword(body.newPassword()), clock.instant());
        sessions.endAll(SubjectType.CUSTOMER, customerId);
        return start(customer, businessById(customer.getBusinessId()).preview(), http);
    }

    private SignedIn start(Customer customer, BusinessPreview business, HttpServletRequest http) {
        customer.recordPortalSignIn(clock.instant());
        String refreshToken = sessions.start(SubjectType.CUSTOMER, customer.getId(), http);
        return new SignedIn(customer, business, accessTokens.forCustomer(customer.getId(), customer.getBusinessId()), refreshToken);
    }

    /** A business with its ID, which the public preview leaves out. */
    private record Biz(long id, BusinessPreview preview) {
    }

    private static final String BUSINESS = "SELECT id, code, name, type, allow_customer_signup FROM businesses";

    private Optional<Biz> business(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim().toUpperCase(Locale.ROOT);
        return jdbc.sql(BUSINESS + " WHERE code = :code AND archived_at IS NULL").param("code", code)
                .query((rs, i) -> biz(rs)).optional();
    }

    private Biz businessById(long businessId) {
        return jdbc.sql(BUSINESS + " WHERE id = :id").param("id", businessId).query((rs, i) -> biz(rs)).optional()
                .orElseThrow(() -> ApiException.notFound("Business"));
    }

    private static Biz biz(ResultSet rs) throws SQLException {
        return new Biz(rs.getLong("id"), new BusinessPreview(rs.getString("code"), rs.getString("name"),
                rs.getString("type"), rs.getBoolean("allow_customer_signup")));
    }

    private String link(String businessCode, String token) {
        return properties.appUrl() + "/portal/" + businessCode + "/set-password?token=" + token;
    }

    /** Emails are matched lower-case and phones by their digits, the way they are stored. */
    private static String identifier(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.contains("@")) {
            return Normalize.email(value);
        }
        String phone = Normalize.phone(value);
        return phone == null ? value.toLowerCase(Locale.ROOT) : phone;
    }

    private String hashPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.invalidField("password", "Use a shorter password (at most 72 bytes)");
        }
        if (password.isBlank()) {
            throw ApiException.invalidField("password", "Choose a password");
        }
        return passwordEncoder.encode(password);
    }

    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("auth.invalid_credentials",
                "That business code, login and password don't match. Try again.");
    }
}
