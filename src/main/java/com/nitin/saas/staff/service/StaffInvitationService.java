package com.nitin.saas.staff.service;

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
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class StaffInvitationService {

    private final StaffInvitationRepository invitationRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final BusinessService businessService;
    private final RBACService rbacService;
    private final EmailNotificationService emailService;
    private final PasswordEncoder passwordEncoder;
    private final com.nitin.saas.audit.service.AuditLogService auditLogService;

    @Value("${app.security.invitation-expiry-hours:72}")
    private Integer invitationExpiryHours;

    @Transactional
    public StaffInvitationResponse inviteStaff(Long businessId, InviteStaffRequest request,
                                               HttpServletRequest httpRequest) {
        businessService.requireAccess(businessId);

        // Check if email already exists as staff in this business
        if (staffRepository.existsByBusinessIdAndEmployeeId(businessId, request.getEmail())) {
            throw new ConflictException("User is already staff in this business");
        }

        // Check if there's a pending invitation
        if (invitationRepository.existsByBusinessIdAndEmailAndUsedFalse(businessId, request.getEmail())) {
            throw new ConflictException("A pending invitation already exists for this email");
        }

        Long invitedBy = rbacService.getCurrentUserId();
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(invitationExpiryHours);

        StaffInvitation invitation = StaffInvitation.builder()
                .businessId(businessId)
                .email(request.getEmail().toLowerCase())
                .role(request.getRole())
                .token(token)
                .expiresAt(expiresAt)
                .invitedBy(invitedBy)
                .message(request.getMessage())
                .department(request.getDepartment())
                .designation(request.getDesignation())
                .inviterIpAddress(getClientIp(httpRequest))
                .inviterUserAgent(getUserAgent(httpRequest))
                .build();

        invitation = invitationRepository.save(invitation);

        // Send invitation email
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        sendInvitationEmail(invitation, business);

        auditLogService.logAction(businessId, "STAFF_INVITED", "STAFF_INVITATION", invitation.getId(),
                String.format("Staff invitation sent to %s as %s", request.getEmail(), request.getRole()));

        log.info("Staff invitation created: id={}, businessId={}, email={}, role={}",
                invitation.getId(), businessId, request.getEmail(), request.getRole());

        return mapToResponse(invitation, business);
    }

    @Transactional
    public StaffInvitationResponse acceptInvitation(AcceptInvitationRequest request,
                                                    HttpServletRequest httpRequest) {
        StaffInvitation invitation = invitationRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid invitation token"));

        if (!invitation.isValid()) {
            throw new BadRequestException("Invitation has expired or already been used");
        }

        Business business = businessRepository.findById(invitation.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        // Create or get user account
        User user = userRepository.findByEmail(invitation.getEmail())
                .orElseGet(() -> createUserFromInvitation(invitation, request));

        // Update user details if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        user = userRepository.save(user);

        // Create staff record
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

        // Set permissions based on role
        setDefaultPermissions(staff, invitation.getRole());

        staffRepository.save(staff);

        // Mark invitation as used
        invitation.markAsUsed(user.getId(), getClientIp(httpRequest), getUserAgent(httpRequest));
        invitationRepository.save(invitation);

        // Update business staff count
        business.setStaffCount(business.getStaffCount() + 1);
        businessRepository.save(business);

        auditLogService.logAction(invitation.getBusinessId(), "STAFF_INVITATION_ACCEPTED",
                "STAFF_INVITATION", invitation.getId(),
                String.format("Staff invitation accepted by %s", user.getEmail()));

        log.info("Staff invitation accepted: invitationId={}, userId={}, businessId={}",
                invitation.getId(), user.getId(), invitation.getBusinessId());

        return mapToResponse(invitation, business);
    }

    @Transactional(readOnly = true)
    public StaffInvitationResponse getInvitationByToken(String token) {
        StaffInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        Business business = businessRepository.findById(invitation.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return mapToResponse(invitation, business);
    }

    @Transactional(readOnly = true)
    public Page<StaffInvitationResponse> getBusinessInvitations(Long businessId, Pageable pageable) {
        businessService.requireAccess(businessId);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return invitationRepository.findByBusinessId(businessId, pageable)
                .map(invitation -> mapToResponse(invitation, business));
    }

    @Transactional(readOnly = true)
    public List<StaffInvitationResponse> getPendingInvitations(Long businessId) {
        businessService.requireAccess(businessId);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        return invitationRepository.findPendingInvitations(businessId).stream()
                .map(invitation -> mapToResponse(invitation, business))
                .collect(Collectors.toList());
    }

    @Transactional
    public void resendInvitation(Long invitationId, HttpServletRequest httpRequest) {
        StaffInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        businessService.requireAccess(invitation.getBusinessId());

        if (invitation.getUsed()) {
            throw new BadRequestException("Invitation has already been accepted");
        }

        // Extend expiry
        invitation.setExpiresAt(LocalDateTime.now().plusHours(invitationExpiryHours));
        invitationRepository.save(invitation);

        // Resend email
        Business business = businessRepository.findById(invitation.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        sendInvitationEmail(invitation, business);

        auditLogService.logAction(invitation.getBusinessId(), "STAFF_INVITATION_RESENT",
                "STAFF_INVITATION", invitation.getId(),
                String.format("Staff invitation resent to %s", invitation.getEmail()));

        log.info("Staff invitation resent: id={}", invitationId);
    }

    @Transactional
    public void cancelInvitation(Long invitationId) {
        StaffInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        businessService.requireAccess(invitation.getBusinessId());

        if (invitation.getUsed()) {
            throw new BadRequestException("Cannot cancel an accepted invitation");
        }

        invitationRepository.delete(invitation);

        auditLogService.logAction(invitation.getBusinessId(), "STAFF_INVITATION_CANCELLED",
                "STAFF_INVITATION", invitation.getId(),
                String.format("Staff invitation cancelled for %s", invitation.getEmail()));

        log.info("Staff invitation cancelled: id={}", invitationId);
    }

    @Transactional
    public void cleanupExpiredInvitations() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        invitationRepository.deleteExpiredInvitations(cutoffDate);
        log.info("Cleaned up expired invitations older than {}", cutoffDate);
    }

    private User createUserFromInvitation(StaffInvitation invitation, AcceptInvitationRequest request) {
        User user = User.builder()
                .email(invitation.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .roles(Set.of(Role.USER))
                .emailVerified(true) // Auto-verify since they accepted invitation
                .enabled(true)
                .accountLocked(false)
                .accountStatus(User.AccountStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    private void setDefaultPermissions(Staff staff, StaffRole role) {
        // Set basic permissions based on role
        switch (role) {
            case OWNER:
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(true);
                break;
            case MANAGER:
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(true);
                break;
            case RECEPTIONIST:
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(false);
                break;
            case TRAINER:
                staff.setCanManageMembers(false);
                staff.setCanManagePayments(false);
                staff.setCanManageSubscriptions(false);
                staff.setCanViewReports(false);
                break;
            case ACCOUNTANT:
                staff.setCanManageMembers(false);
                staff.setCanManagePayments(true);
                staff.setCanManageSubscriptions(false);
                staff.setCanViewReports(true);
                break;
            case SALES:
                staff.setCanManageMembers(true);
                staff.setCanManagePayments(false);
                staff.setCanManageSubscriptions(true);
                staff.setCanViewReports(false);
                break;
        }
    }

    private void sendInvitationEmail(StaffInvitation invitation, Business business) {
        // TODO: Implement actual email sending
        log.info("Sending invitation email to: {} for business: {}", invitation.getEmail(), business.getName());
        log.info("Invitation token: {}", invitation.getToken());
        log.info("Accept link: /staff/accept-invitation?token={}", invitation.getToken());
    }

    private StaffInvitationResponse mapToResponse(StaffInvitation invitation, Business business) {
        User invitedByUser = userRepository.findById(invitation.getInvitedBy()).orElse(null);
        User acceptedByUser = invitation.getAcceptedByUserId() != null
                ? userRepository.findById(invitation.getAcceptedByUserId()).orElse(null)
                : null;

        return StaffInvitationResponse.builder()
                .id(invitation.getId())
                .businessId(invitation.getBusinessId())
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

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}