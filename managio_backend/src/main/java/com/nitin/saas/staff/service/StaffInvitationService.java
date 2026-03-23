package com.nitin.saas.staff.service;

import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.auth.service.RBACService;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ConflictException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.common.utils.IpAddressUtil;
import com.nitin.saas.staff.dto.AcceptInvitationRequest;
import com.nitin.saas.staff.dto.InviteStaffRequest;
import com.nitin.saas.staff.dto.StaffInvitationResponse;
import com.nitin.saas.staff.entity.Staff;
import com.nitin.saas.staff.entity.StaffInvitation;
import com.nitin.saas.staff.enums.StaffRole;
import com.nitin.saas.staff.repository.StaffInvitationRepository;
import com.nitin.saas.staff.repository.StaffRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffInvitationService {

    private final StaffInvitationRepository invitationRepository;
    private final StaffRepository           staffRepository;
    private final UserRepository            userRepository;
    private final BusinessRepository        businessRepository;
    private final BusinessService           businessService;
    private final RBACService               rbacService;
    private final EmailNotificationService  emailService;
    private final PasswordEncoder           passwordEncoder;
    private final AuditLogService           auditLogService;

    @Value("${app.security.invitation-expiry-hours:72}")
    private Integer invitationExpiryHours;

    // ── Invite ────────────────────────────────────────────────────────────────

    @Transactional
    public StaffInvitationResponse inviteStaff(Long businessId,
                                               InviteStaffRequest request,
                                               HttpServletRequest httpRequest) {
        businessService.requireAccess(businessId);

        String normalizedEmail = request.getEmail().toLowerCase();

        // Check if user is already staff
        userRepository.findByEmail(normalizedEmail).ifPresent(existingUser -> {
            if (staffRepository.existsByBusinessIdAndUserId(businessId, existingUser.getId())) {
                throw new ConflictException("This user is already a staff member in this business");
            }
        });

        // FIX H2: uses expiry-aware check — an expired invitation does NOT block a new one
        if (invitationRepository.existsActivePendingInvitation(
                businessId, normalizedEmail, LocalDateTime.now())) {
            throw new ConflictException(
                    "A pending invitation already exists for this email address. "
                            + "Cancel the existing one or wait for it to expire.");
        }

        Long   invitedBy = rbacService.getCurrentUserId();
        String token     = UUID.randomUUID().toString();

        StaffInvitation invitation = StaffInvitation.builder()
                .businessId(businessId)
                .email(normalizedEmail)
                .role(request.getRole())
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(invitationExpiryHours))
                .invitedBy(invitedBy)
                .message(request.getMessage())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .inviterIpAddress(IpAddressUtil.getClientIp(httpRequest))
                .inviterUserAgent(httpRequest.getHeader("User-Agent"))
                .build();

        invitation = invitationRepository.save(invitation);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Send invitation email
        sendInvitationEmail(invitation, business);

        auditLogService.logAction(
                businessId, "STAFF_INVITED", "STAFF_INVITATION", invitation.getId(),
                String.format("Staff invitation sent to %s as %s", normalizedEmail, request.getRole()));

        log.info("Staff invitation created: id={}, businessId={}, email={}, role={}",
                invitation.getId(), businessId, normalizedEmail, request.getRole());

        return mapToResponse(invitation, business, loadUsersForInvitations(List.of(invitation)));
    }

    // ── Accept ────────────────────────────────────────────────────────────────

    @Transactional
    public StaffInvitationResponse acceptInvitation(AcceptInvitationRequest request,
                                                    HttpServletRequest httpRequest) {
        log.info("ACCEPT_INVITATION: token={} from IP={} UA={}",
                request.getToken(), IpAddressUtil.getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        try {
            StaffInvitation invitation = invitationRepository.findByToken(request.getToken())
                    .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

            if (invitation.getUsed()) {
                throw new BadRequestException("This invitation has already been accepted");
            }
            if (invitation.isExpired()) {
                throw new BadRequestException(
                        "This invitation has expired. Please ask your administrator to resend it.");
            }

            Business business = businessRepository.findById(invitation.getBusinessId())
                    .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

            // Find existing user or create new one
            User user = userRepository.findByEmail(invitation.getEmail())
                    .orElseGet(() -> createUserFromInvitation(invitation, request));
            log.info("ACCEPT_INVITATION: user resolved id={} email={}", user.getId(), user.getEmail());

            // Update profile fields if provided
            boolean userModified = false;
            if (request.getFirstName() != null)  { user.setFirstName(request.getFirstName());   userModified = true; }
            if (request.getLastName()  != null)  { user.setLastName(request.getLastName());    userModified = true; }
            if (request.getPhoneNumber() != null){ user.setPhoneNumber(request.getPhoneNumber()); userModified = true; }
            if (userModified) {
                user = userRepository.save(user);
                log.info("ACCEPT_INVITATION: user profile updated id={}", user.getId());
            }

            // Create Staff record
            Staff staff = Staff.builder()
                    .businessId(invitation.getBusinessId())
                    .userId(user.getId())
                    .role(invitation.getRole())
                    .status(Staff.StaffStatus.ACTIVE)
                    .hireDate(request.getHireDate() != null ? request.getHireDate() : LocalDate.now())
                    .department(invitation.getDepartment())
                    .designation(invitation.getDesignation())
                    .phone(request.getPhoneNumber())
                    .email(invitation.getEmail())
                    .address(request.getAddress())
                    .emergencyContact(request.getEmergencyContact())
                    .canLogin(true)
                    .build();

            setDefaultPermissions(staff, invitation.getRole());
            staffRepository.save(staff);
            log.info("ACCEPT_INVITATION: staff created id={} businessId={} userId={}",
                    staff.getId(), staff.getBusinessId(), staff.getUserId());

            // Send welcome email (async — won't fail the transaction)
            emailService.sendStaffWelcomeEmail(
                    user.getEmail(),
                    invitation.getBusinessId(),
                    user.getFullName(),
                    business.getName(),
                    staff.getRole().getDisplayName());

            // Mark invitation as used
            invitation.markAsUsed(user.getId(),
                    IpAddressUtil.getClientIp(httpRequest),
                    httpRequest.getHeader("User-Agent"));
            invitationRepository.save(invitation);
            log.info("ACCEPT_INVITATION: invitation marked used id={}", invitation.getId());

            // Increment business staff count (null-safe for legacy rows)
            business.incrementStaffCount();
            businessRepository.save(business);
            log.info("ACCEPT_INVITATION: business staffCount incremented businessId={} newCount={}",
                    business.getId(), business.getStaffCount());

            auditLogService.logActionAsActor(
                    invitation.getBusinessId(),
                    user.getId(),
                    "USER",
                    user.getPublicId(),
                    "STAFF_INVITATION_ACCEPTED",
                    "STAFF_INVITATION",
                    invitation.getId(),
                    String.format("Invitation accepted by %s", user.getEmail())
            );

            log.info("ACCEPT_INVITATION: SUCCESS invitationId={} userId={} businessId={}",
                    invitation.getId(), user.getId(), invitation.getBusinessId());

            return mapToResponse(invitation, business, loadUsersForInvitations(List.of(invitation)));
        } catch (Exception ex) {
            log.error("ACCEPT_INVITATION: FAILED token={} reason={}", request.getToken(), ex.getMessage(), ex);
            throw ex;
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public StaffInvitationResponse getInvitationByToken(String token) {
        StaffInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or expired"));
        Business business = businessRepository.findById(invitation.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        return mapToResponse(invitation, business, loadUsersForInvitations(List.of(invitation)));
    }

    /**
     * FIX M3: batch-loads all referenced users in a single findAllById() call.
     */
    @Transactional(readOnly = true)
    public Page<StaffInvitationResponse> getBusinessInvitations(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Page<StaffInvitation> page = invitationRepository.findByBusinessId(businessId, pageable);
        Map<Long, User> userCache  = loadUsersForInvitations(page.getContent());

        return page.map(inv -> mapToResponse(inv, business, userCache));
    }

    @Transactional(readOnly = true)
    public List<StaffInvitationResponse> getPendingInvitations(Long businessId) {
        businessService.requireAccess(businessId);
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        List<StaffInvitation> invitations = invitationRepository.findPendingInvitations(businessId);
        Map<Long, User> userCache = loadUsersForInvitations(invitations);

        return invitations.stream()
                .map(inv -> mapToResponse(inv, business, userCache))
                .collect(Collectors.toList());
    }

    // ── Mutate ────────────────────────────────────────────────────────────────

    /**
     * FIX CVL-001: resend now actually sends the invitation email.
     * Also extends the expiry so the recipient gets a fresh 72-hour window.
     */
    @Transactional
    public void resendInvitation(Long invitationId, HttpServletRequest httpRequest) {
        StaffInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        businessService.requireAccess(invitation.getBusinessId());

        if (invitation.getUsed()) {
            throw new BadRequestException("This invitation has already been accepted and cannot be resent");
        }

        // Extend expiry
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationExpiryHours));
        invitationRepository.save(invitation);

        Business business = businessRepository.findById(invitation.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // FIX: actually send the email (was only logging before)
        sendInvitationEmail(invitation, business);

        auditLogService.logAction(
                invitation.getBusinessId(), "STAFF_INVITATION_RESENT",
                "STAFF_INVITATION", invitation.getId(),
                String.format("Invitation resent to %s", invitation.getEmail()));

        log.info("Staff invitation resent: id={}, email={}", invitationId, invitation.getEmail());
    }

    @Transactional
    public void cancelInvitation(Long invitationId) {
        StaffInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        businessService.requireAccess(invitation.getBusinessId());

        if (invitation.getUsed()) {
            throw new BadRequestException("Cannot cancel an invitation that has already been accepted");
        }

        invitationRepository.delete(invitation);

        auditLogService.logAction(
                invitation.getBusinessId(), "STAFF_INVITATION_CANCELLED",
                "STAFF_INVITATION", invitation.getId(),
                String.format("Invitation cancelled for %s", invitation.getEmail()));

        log.info("Staff invitation cancelled: id={}", invitationId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Central email dispatch — used for both initial invite and resend.
     * Delegates to async EmailNotificationService so it never blocks a transaction.
     */
    private void sendInvitationEmail(StaffInvitation invitation, Business business) {
        emailService.sendStaffInvitationEmail(
                invitation.getEmail(),
                invitation.getBusinessId(),      // ADD THIS
                business.getName(),
                invitation.getRole().getDisplayName(),
                invitation.getDepartment(),
                invitation.getDesignation(),
                invitation.getToken());
    }

    /**
     * FIX M3: collect all user IDs from a list of invitations in one batch query.
     */
    private Map<Long, User> loadUsersForInvitations(List<StaffInvitation> invitations) {
        Set<Long> userIds = new HashSet<>();
        for (StaffInvitation inv : invitations) {
            userIds.add(inv.getInvitedBy());
            if (inv.getAcceptedByUserId() != null) userIds.add(inv.getAcceptedByUserId());
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private User createUserFromInvitation(StaffInvitation invitation,
                                          AcceptInvitationRequest request) {
        User user = User.builder()
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                // Use a mutable Set so Hibernate can manage the collection
                .roles(new java.util.HashSet<>(java.util.Set.of(Role.USER)))
                .emailVerified(true)          // verified implicitly via token in email
                .enabled(true)
                .accountLocked(false)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private void setDefaultPermissions(Staff staff, StaffRole role) {
        switch (role) {
            case OWNER, MANAGER -> {
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(true);
            }
            case RECEPTIONIST -> {
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(false);
            }
            case ACCOUNTANT -> {
                staff.setCanManageMembers(false);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(false);
                staff.setCanViewReports(true);
            }
            case SALES -> {
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(false);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(false);
            }
            default -> {
                // TRAINER — view-only, no elevated flags
            }
        }
    }

    private StaffInvitationResponse mapToResponse(StaffInvitation invitation,
                                                  Business business,
                                                  Map<Long, User> userCache) {
        User invitedByUser  = userCache.get(invitation.getInvitedBy());
        User acceptedByUser = invitation.getAcceptedByUserId() != null
                ? userCache.get(invitation.getAcceptedByUserId()) : null;

        return StaffInvitationResponse.builder()
                .id(invitation.getId())
                .businessId(invitation.getBusinessId())
                .businessPublicId(business.getPublicId())
                .businessName(business.getName())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .roleDisplay(invitation.getRole().getDisplayName())
                .token(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .used(invitation.getUsed())
                .usedAt(invitation.getUsedAt())
                .acceptedByUserId(invitation.getAcceptedByUserId())
                .acceptedByUserEmail(acceptedByUser != null ? acceptedByUser.getEmail() : null)
                .invitedBy(invitation.getInvitedBy())
                .invitedByUserEmail(invitedByUser != null ? invitedByUser.getEmail() : null)
                .message(invitation.getMessage())
                .department(invitation.getDepartment())
                .designation(invitation.getDesignation())
                .createdAt(invitation.getCreatedAt())
                .expired(invitation.isExpired())
                .valid(invitation.isValid())
                .build();
    }
}