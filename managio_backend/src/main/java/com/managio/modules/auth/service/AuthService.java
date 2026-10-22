package com.managio.modules.auth.service;

import com.managio.modules.auth.domain.SubjectType;
import com.managio.modules.auth.domain.User;
import com.managio.modules.auth.repository.UserRepository;
import com.managio.modules.auth.dto.AuthDtos.ChangePasswordRequest;
import com.managio.modules.auth.dto.AuthDtos.SignInRequest;
import com.managio.modules.auth.dto.AuthDtos.SignUpRequest;
import com.managio.modules.auth.service.GoogleIdentityVerifier.GoogleIdentity;
import com.managio.modules.auth.domain.TokenPurpose;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Account sign-up, sign-in and recovery for owners and team members (docs/PRODUCT.md, rules IDN-1 to IDN-7). */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Compared against when the email is unknown, so both failure paths take the same time. */
    private static final String DUMMY_HASH = new BCryptPasswordEncoder(11).encode(SecureTokens.generate());

    private final UserRepository users;
    private final Sessions sessions;
    private final OneTimeTokens oneTimeTokens;
    private final SignInAttempts attempts;
    private final GoogleIdentityVerifier google;
    private final AccessTokens accessTokens;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;
    private final OutboundMail mail;
    private final ManagioProperties properties;
    private final Clock clock;

    /** A successful sign-in: the account, its access token and the raw refresh token for the cookie. */
    public record SignedIn(User user, IssuedToken accessToken, String refreshToken) {
    }

    @Transactional
    public SignedIn signUp(SignUpRequest request, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.SIGN_UP, http);
        String email = Normalize.email(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("auth.email_taken",
                    "An account with this email already exists. Sign in, or reset your password if you forgot it.");
        }
        User user = users.save(User.withPassword(email, request.fullName().trim(), hashPassword(request.password())));
        sendVerificationEmail(user);
        attempts.record(SubjectType.USER, user.getId(), email, "SUCCESS", "sign-up", http);
        return startSession(user, http);
    }

    /** IDN-8: an invitee without an account gets one with a verified email, because the invitation link proves it. */
    @Transactional
    public SignedIn registerInvited(String email, String fullName, String password, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.SIGN_UP, http);
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("invitation.sign_in_required",
                    "You already have a Managio account. Sign in with " + email + " to accept this invitation.");
        }
        User user = users.save(User.withPassword(email, fullName.trim(), hashPassword(password)));
        user.verifyEmail(clock.instant());
        attempts.record(SubjectType.USER, user.getId(), email, "SUCCESS", "invitation sign-up", http);
        return startSession(user, http);
    }

    /** IDN-2: one message for unknown email and wrong password; 5 failures lock the account for 15 minutes. */
    @Transactional
    public SignedIn signIn(SignInRequest request, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.SIGN_IN, http);
        String email = Normalize.email(request.email());
        User user = users.findByEmail(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            attempts.failedUserSignIn(null, email, "unknown email", http);
            throw invalidCredentials();
        }
        requireCanSignIn(user, email, clock.instant(), http);
        if (!user.hasPassword() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            attempts.failedUserSignIn(user.getId(), email, user.hasPassword() ? "wrong password" : "no password set", http);
            throw invalidCredentials();
        }
        attempts.record(SubjectType.USER, user.getId(), email, "SUCCESS", "password", http);
        return startSession(user, http);
    }

    /**
     * IDN-3: a verified Google email signs in the matching account, linking Google to it on first use,
     * or creates a new verified account. The same account rules apply as for a password: a disabled or locked
     * account can't sign in this way either.
     *
     * <p>Linking to an account whose email was never verified removes its password and ends its sessions: whoever
     * chose that password never proved they own the address, and could otherwise keep a way into the account that
     * the address's real owner just signed in to (account pre-hijacking).
     */
    @Transactional
    public SignedIn signInWithGoogle(String credential, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.SIGN_IN, http);
        GoogleIdentity identity = google.verify(credential);
        String email = Normalize.email(identity.email());
        if (email == null || !identity.emailVerified()) {
            throw ApiException.rule("google.email_unverified",
                    "Your Google account's email isn't verified. Verify it with Google or sign up with email and password.");
        }
        Instant now = clock.instant();
        User user = users.findByGoogleSubject(identity.subject()).orElse(null);
        if (user == null) {
            user = users.findByEmail(email).orElse(null);
            if (user != null && user.getGoogleSubject() != null) {
                attempts.record(SubjectType.USER, user.getId(), email, "FAILURE", "linked to another Google account", http);
                throw ApiException.conflict("google.other_account", "This email is linked to a different Google account.");
            }
        }
        if (user == null) {
            user = users.save(User.withGoogle(email, displayName(identity), identity.subject(), now));
        } else {
            requireCanSignIn(user, email, now, http);
            if (user.getGoogleSubject() == null) {
                if (!user.isEmailVerified()) {
                    user.removePassword();
                    sessions.endAll(SubjectType.USER, user.getId());
                }
                user.linkGoogle(identity.subject());
                user.verifyEmail(now);
            }
        }
        attempts.record(SubjectType.USER, user.getId(), email, "SUCCESS", "google", http);
        return startSession(user, http);
    }

    /** Rotates the refresh token and issues a new access token (IDN-5). */
    @Transactional(noRollbackFor = ApiException.class)
    public SignedIn refresh(String refreshToken, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.REFRESH, http);
        Sessions.Refreshed refreshed = sessions.refresh(SubjectType.USER, refreshToken, http);
        User user = users.findById(refreshed.subjectId()).filter(User::isActive).orElseThrow(() -> {
            sessions.end(refreshed.refreshToken());
            return ApiException.unauthorized("auth.session_expired", "Your session has ended. Sign in again.");
        });
        return new SignedIn(user, accessTokens.forUser(user.getId(), user.getEmail()), refreshed.refreshToken());
    }

    @Transactional
    public void signOut(String refreshToken) {
        if (refreshToken != null) {
            sessions.end(refreshToken);
        }
    }

    @Transactional
    public User verifyEmail(String token) {
        long userId = oneTimeTokens.redeem(TokenPurpose.EMAIL_VERIFICATION, SubjectType.USER, token);
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Account"));
        user.verifyEmail(clock.instant());
        return user;
    }

    @Transactional
    public void resendVerificationEmail(long userId, HttpServletRequest http) {
        rateLimiter.check(Policy.EMAIL, "verify:" + userId);
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Account"));
        if (!user.isEmailVerified()) {
            sendVerificationEmail(user);
        }
    }

    /**
     * IDN-4: always succeeds from the caller's point of view, so it can't be used to discover accounts. Limited per
     * client address and per recipient, so nobody can flood an inbox with reset emails from many addresses.
     */
    @Transactional
    public void requestPasswordReset(String rawEmail, HttpServletRequest http) {
        rateLimiter.checkClient(Policy.EMAIL, http);
        String email = Normalize.email(rawEmail);
        rateLimiter.check(Policy.EMAIL_PER_RECIPIENT, "reset:" + email);
        users.findByEmail(email).filter(User::isActive).ifPresent(user -> {
            String token = oneTimeTokens.issue(TokenPurpose.PASSWORD_RESET, SubjectType.USER, user.getId());
            mail.send(MailTemplates.resetPassword(user.getEmail(), user.getFullName(),
                    properties.appUrl() + "/reset-password?token=" + token));
        });
    }

    /** IDN-4: sets the new password, confirms the email and signs the account out everywhere. */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        String hash = hashPassword(newPassword);
        long userId = oneTimeTokens.redeem(TokenPurpose.PASSWORD_RESET, SubjectType.USER, token);
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Account"));
        user.setPassword(hash);
        user.verifyEmail(clock.instant());
        sessions.endAll(SubjectType.USER, userId);
    }

    /** IDN-6: needs the current password if one is set; ends every other session and starts a fresh one. */
    @Transactional
    public SignedIn changePassword(long userId, ChangePasswordRequest request, HttpServletRequest http) {
        rateLimiter.check(Policy.PASSWORD_CHECK, "user:" + userId);
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Account"));
        if (user.hasPassword() && (request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))) {
            throw ApiException.invalidField("currentPassword", "This isn't your current password");
        }
        user.setPassword(hashPassword(request.newPassword()));
        sessions.endAll(SubjectType.USER, userId);
        return startSession(user, http);
    }

    @Transactional
    public User updateProfile(long userId, String fullName, String phone) {
        User user = users.findById(userId).orElseThrow(() -> ApiException.notFound("Account"));
        user.updateProfile(fullName.trim(), Normalize.phone(phone));
        return user;
    }

    @Transactional(readOnly = true)
    public User get(long userId) {
        return users.findById(userId).orElseThrow(() -> ApiException.notFound("Account"));
    }

    /** Refuses a sign-in to a disabled or locked account, whichever way the person proved who they are. */
    private void requireCanSignIn(User user, String identifier, Instant now, HttpServletRequest http) {
        if (!user.isActive()) {
            attempts.record(SubjectType.USER, user.getId(), identifier, "FAILURE", "account disabled", http);
            throw disabled();
        }
        if (user.isLocked(now)) {
            attempts.record(SubjectType.USER, user.getId(), identifier, "FAILURE", "account locked", http);
            long minutes = Math.max(1, now.until(user.getLockedUntil(), ChronoUnit.MINUTES) + 1);
            throw new ApiException(HttpStatus.LOCKED, "auth.locked",
                    "Too many failed attempts. Try again in " + minutes + " minutes, or reset your password.");
        }
    }

    private SignedIn startSession(User user, HttpServletRequest http) {
        user.recordSignIn(clock.instant());
        String refreshToken = sessions.start(SubjectType.USER, user.getId(), http);
        return new SignedIn(user, accessTokens.forUser(user.getId(), user.getEmail()), refreshToken);
    }

    private void sendVerificationEmail(User user) {
        String token = oneTimeTokens.issue(TokenPurpose.EMAIL_VERIFICATION, SubjectType.USER, user.getId());
        mail.send(MailTemplates.verifyEmail(user.getEmail(), user.getFullName(),
                properties.appUrl() + "/verify-email?token=" + token));
    }

    /** bcrypt only uses the first 72 bytes, so longer passwords would silently be truncated. */
    private String hashPassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw ApiException.invalidField("password", "Use a shorter password (at most 72 bytes)");
        }
        if (password.isBlank()) {
            throw ApiException.invalidField("password", "Choose a password");
        }
        return passwordEncoder.encode(password);
    }

    private static String displayName(GoogleIdentity identity) {
        String name = Normalize.text(identity.name());
        return name != null ? name : identity.email().substring(0, identity.email().indexOf('@'));
    }

    private static ApiException invalidCredentials() {
        return ApiException.unauthorized("auth.invalid_credentials", "That email and password don't match. Try again.");
    }

    private static ApiException disabled() {
        return new ApiException(HttpStatus.FORBIDDEN, "auth.disabled", "This account is disabled. Contact support.");
    }
}
