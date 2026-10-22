package com.managio.modules.team.service;

import com.managio.modules.team.domain.Invitation;
import com.managio.modules.team.domain.TeamMember;
import com.managio.modules.team.repository.InvitationRepository;
import com.managio.modules.business.domain.Business;
import com.managio.modules.business.service.BusinessService;
import com.managio.modules.team.domain.Role;

import com.managio.modules.auth.service.AuthService;
import com.managio.modules.auth.service.AuthService.SignedIn;
import com.managio.modules.auth.service.GoogleIdentityVerifier;
import com.managio.modules.auth.domain.User;
import com.managio.modules.auth.repository.UserRepository;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.config.ManagioProperties;
import com.managio.platform.mail.MailTemplates;
import com.managio.platform.mail.OutboundMail;
import com.managio.platform.security.Principals.UserPrincipal;
import com.managio.platform.security.SecureTokens;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Invitations to join a business (TEAM-1, TEAM-2, TEAM-7, TEAM-8, IDN-8). */
@Service
@RequiredArgsConstructor
public class InvitationService {

    static final Duration LIFETIME = Duration.ofDays(7);

    private final InvitationRepository invitations;
    private final TeamService team;
    private final RoleService roles;
    private final BusinessService businesses;
    private final UserRepository users;
    private final AuthService auth;
    private final GoogleIdentityVerifier google;
    private final OutboundMail mail;
    private final ManagioProperties properties;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final Clock clock;

    public record InvitationResponse(long id, String email, long roleId, String roleName, @Nullable String title,
                                     @Nullable String invitedBy, Instant expiresAt, Instant createdAt) {
    }

    public record InvitationPreview(String businessName, String roleName, String email, String inviterName,
                                    boolean accountExists, Instant expiresAt) {
    }

    /** The result of accepting: the business joined and, for a new or Google sign-in, the new session. */
    public record Accepted(long businessId, SignedIn signedIn) {
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> pending(BusinessContext ctx) {
        return jdbc.sql("""
                        SELECT i.id, i.email, i.role_id, r.name AS role_name, i.title, u.full_name AS invited_by,
                               i.expires_at, i.created_at
                        FROM invitations i
                        JOIN roles r ON r.id = i.role_id
                        LEFT JOIN users u ON u.id = i.invited_by_user_id
                        WHERE i.business_id = :businessId AND i.accepted_at IS NULL AND i.revoked_at IS NULL
                          AND i.expires_at > :now
                        ORDER BY i.created_at DESC""")
                .param("businessId", ctx.businessId())
                .param("now", java.sql.Timestamp.from(clock.instant()))
                .query((rs, i) -> new InvitationResponse(rs.getLong("id"), rs.getString("email"), rs.getLong("role_id"),
                        rs.getString("role_name"), rs.getString("title"), rs.getString("invited_by"),
                        rs.getTimestamp("expires_at").toInstant(), rs.getTimestamp("created_at").toInstant()))
                .list();
    }

    @Transactional
    public InvitationResponse invite(BusinessContext ctx, String rawEmail, long roleId, String title) {
        User inviter = users.findById(ctx.userId()).orElseThrow(() -> ApiException.notFound("Account"));
        if (!inviter.isEmailVerified()) {
            throw ApiException.rule("invitation.unverified_sender",
                    "Confirm your email address before inviting people. We sent you a link when you signed up.");
        }
        Role role = roles.assignable(ctx, roleId);
        String email = Normalize.email(rawEmail);
        boolean alreadyMember = jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM team_members tm JOIN users u ON u.id = tm.user_id
                                       WHERE tm.business_id = :businessId AND u.email = :email AND tm.status = 'ACTIVE')""")
                .param("businessId", ctx.businessId()).param("email", email).query(Boolean.class).single();
        if (alreadyMember) {
            throw ApiException.conflict("invitation.already_member", email + " is already on the team.");
        }
        Instant now = clock.instant();
        invitations.findPendingForEmail(ctx.businessId(), email, now).forEach(old -> old.revoke(now));

        String token = SecureTokens.generate();
        Invitation invitation = invitations.save(Invitation.create(ctx.businessId(), email, role.getId(),
                Normalize.text(title), SecureTokens.hash(token), ctx.userId(), now, now.plus(LIFETIME)));
        sendEmail(invitation, token, inviter, role);
        activity.record(ctx, "INVITATION_SENT", "INVITATION", invitation.getId(),
                "Invited " + email + " as " + role.getName());
        return new InvitationResponse(invitation.getId(), email, role.getId(), role.getName(), invitation.getTitle(),
                inviter.getFullName(), invitation.getExpiresAt(), invitation.getCreatedAt());
    }

    /** TEAM-1: a resend creates a new link and the old one stops working. */
    @Transactional
    public void resend(BusinessContext ctx, long invitationId) {
        Invitation invitation = pendingInvitation(ctx, invitationId);
        Role role = roles.assignable(ctx, invitation.getRoleId());
        String token = SecureTokens.generate();
        invitation.renew(SecureTokens.hash(token), clock.instant().plus(LIFETIME));
        User inviter = users.findById(ctx.userId()).orElseThrow(() -> ApiException.notFound("Account"));
        sendEmail(invitation, token, inviter, role);
        activity.record(ctx, "INVITATION_RESENT", "INVITATION", invitationId, "Resent invitation to " + invitation.getEmail());
    }

    @Transactional
    public void revoke(BusinessContext ctx, long invitationId) {
        Invitation invitation = pendingInvitation(ctx, invitationId);
        invitation.revoke(clock.instant());
        activity.record(ctx, "INVITATION_REVOKED", "INVITATION", invitationId, "Cancelled invitation to " + invitation.getEmail());
    }

    @Transactional(readOnly = true)
    public InvitationPreview preview(String token) {
        Invitation invitation = usable(token);
        Business business = businesses.get(invitation.getBusinessId());
        Role role = roles.get(invitation.getBusinessId(), invitation.getRoleId());
        String inviter = invitation.getInvitedByUserId() == null ? null
                : users.findById(invitation.getInvitedByUserId()).map(User::getFullName).orElse(null);
        return new InvitationPreview(business.getName(), role.getName(), invitation.getEmail(), inviter,
                users.existsByEmail(invitation.getEmail()), invitation.getExpiresAt());
    }

    /**
     * TEAM-2, IDN-8: a signed-in invitee must be signed in with the invited email; otherwise a new account is
     * created with a password or Google. Google must report the same email as the invitation.
     */
    @Transactional
    public Accepted accept(String token, UserPrincipal principal, String fullName, String password,
                           String googleCredential, HttpServletRequest http) {
        Invitation invitation = usable(token);
        SignedIn signedIn = null;
        long userId;
        if (principal != null) {
            User user = users.findById(principal.id()).orElseThrow(() -> ApiException.notFound("Account"));
            requireSameEmail(invitation, user.getEmail());
            userId = user.getId();
        } else if (googleCredential != null && !googleCredential.isBlank()) {
            requireSameEmail(invitation, Normalize.email(google.verify(googleCredential).email()));
            signedIn = auth.signInWithGoogle(googleCredential, http);
            userId = signedIn.user().getId();
        } else {
            if (Normalize.text(fullName) == null) {
                throw ApiException.invalidField("fullName", "Enter your name");
            }
            if (password == null || password.length() < 8) {
                throw ApiException.invalidField("password", "Use at least 8 characters");
            }
            signedIn = auth.registerInvited(invitation.getEmail(), fullName, password, http);
            userId = signedIn.user().getId();
        }

        Business business = businesses.get(invitation.getBusinessId());
        LocalDate today = LocalDate.now(clock.withZone(business.zone()));
        TeamMember member = team.join(invitation.getBusinessId(), userId, invitation.getRoleId(), invitation.getTitle(), today);
        invitation.accept(userId, clock.instant());
        activity.recordUser(invitation.getBusinessId(), userId, null, "INVITATION_ACCEPTED", "TEAM_MEMBER",
                member.getId(), invitation.getEmail() + " joined the team");
        return new Accepted(invitation.getBusinessId(), signedIn);
    }

    private Invitation usable(String token) {
        return invitations.findByTokenHash(SecureTokens.hash(token == null ? "" : token))
                .filter(i -> i.isPending(clock.instant()))
                .orElseThrow(() -> ApiException.rule("invitation.invalid",
                        "This invitation has expired or was already used. Ask for a new one."));
    }

    private Invitation pendingInvitation(BusinessContext ctx, long invitationId) {
        Invitation invitation = invitations.findByIdAndBusinessId(invitationId, ctx.businessId())
                .orElseThrow(() -> ApiException.notFound("Invitation"));
        if (!invitation.isPending(clock.instant())) {
            throw ApiException.rule("invitation.closed", "This invitation was already accepted, cancelled or has expired.");
        }
        return invitation;
    }

    private static void requireSameEmail(Invitation invitation, String email) {
        if (!invitation.getEmail().equals(email)) {
            throw ApiException.rule("invitation.wrong_account",
                    "This invitation is for " + invitation.getEmail() + ". Sign in with that email to accept it.");
        }
    }

    private void sendEmail(Invitation invitation, String token, User inviter, Role role) {
        Business business = businesses.get(invitation.getBusinessId());
        mail.send(MailTemplates.invitation(invitation.getEmail(), business.getName(), inviter.getFullName(), role.getName(),
                properties.appUrl() + "/invite?token=" + token));
    }
}
