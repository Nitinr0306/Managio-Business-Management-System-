package com.nitin.saas.member.service;

import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.email.EmailNotificationService;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.dto.CreateMemberRequest;
import com.nitin.saas.member.dto.MemberResponse;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberService {

        private final MemberRepository         memberRepository;
        private final BusinessService          businessService;
        private final BusinessRepository       businessRepository;
        private final AuditLogService          auditLogService;
        private final EmailNotificationService emailService;

        // ── Create ────────────────────────────────────────────────────────────────

        /**
         * Staff/owner adds a member manually (no password — member cannot log in via
         * the member portal until they use the self-registration flow or staff sets a password).
         *
         * A welcome email is sent if the member has a registered email address.
         */
        @Transactional
        public MemberResponse createMember(Long businessId, CreateMemberRequest request) {
                businessService.requireAccess(businessId);

                Member member = Member.builder()
                        .businessId(businessId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phone(request.getPhone())
                        .email(request.getEmail() != null ? request.getEmail().toLowerCase() : null)
                        .dateOfBirth(request.getDateOfBirth())
                        .gender(request.getGender())
                        .address(request.getAddress())
                        .notes(request.getNotes())
                        .status("ACTIVE")
                        .accountEnabled(true)
                        .emailVerified(false)
                        .build();

                member = memberRepository.save(member);

                // Update business member count
                businessRepository.findById(businessId).ifPresent(business -> {
                        business.incrementMemberCount();
                        businessRepository.save(business);
                });

                // Send welcome email if the member has an email address
                if (member.getEmail() != null) {
                        final Member savedMember = member;

                        businessRepository.findById(businessId).ifPresent(business ->
                                emailService.sendMemberWelcomeEmail(
                                        savedMember.getEmail(),
                                        businessId,                     // ADD
                                        savedMember.getFullName(),
                                        business.getName()
                                ));
                }

                log.info("Member created: id={}, businessId={}", member.getId(), businessId);
                return mapToResponse(member);
        }

        // ── Read ──────────────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public MemberResponse getMemberById(Long id) {
                Member member = findActiveMemberById(id);
                businessService.requireAccess(member.getBusinessId());
                return mapToResponse(member);
        }

        @Transactional(readOnly = true)
        public Page<MemberResponse> getMembers(Long businessId, Pageable pageable) {
                businessService.requireAccess(businessId);
                return memberRepository.findActiveByBusinessId(businessId, pageable)
                        .map(this::mapToResponse);
        }

        @Transactional(readOnly = true)
        public Page<MemberResponse> searchMembers(Long businessId, String query, Pageable pageable) {
                businessService.requireAccess(businessId);
                return memberRepository.searchMembers(businessId, query, pageable)
                        .map(this::mapToResponse);
        }

        @Transactional(readOnly = true)
        public Page<MemberResponse> getMembersByStatus(Long businessId, String status, Pageable pageable) {
                businessService.requireAccess(businessId);
                return memberRepository.findByBusinessIdAndStatus(businessId, status, pageable)
                        .map(this::mapToResponse);
        }

        // ── Update ────────────────────────────────────────────────────────────────

        @Transactional
        public MemberResponse updateMember(Long id, CreateMemberRequest request) {
                Member member = findActiveMemberById(id);
                businessService.requireAccess(member.getBusinessId());

                member.setFirstName(request.getFirstName());
                member.setLastName(request.getLastName());
                member.setPhone(request.getPhone());
                member.setEmail(request.getEmail() != null ? request.getEmail().toLowerCase() : null);
                member.setDateOfBirth(request.getDateOfBirth());
                member.setGender(request.getGender());
                member.setAddress(request.getAddress());
                member.setNotes(request.getNotes());

                member = memberRepository.save(member);
                log.info("Member updated: id={}", id);
                return mapToResponse(member);
        }

        // ── Deactivate ────────────────────────────────────────────────────────────

        @Transactional
        public void deactivateMember(Long id) {
                Member member = findActiveMemberById(id);
                businessService.requireAccess(member.getBusinessId());

                member.deactivate();
                memberRepository.save(member);

                businessRepository.findById(member.getBusinessId()).ifPresent(business -> {
                        business.decrementMemberCount();
                        businessRepository.save(business);
                });

                auditLogService.logMemberDeactivation(
                        member.getBusinessId(), member.getId(), member.getFullName());

                log.info("Member deactivated: id={}", id);
        }

        // ── Count helpers ─────────────────────────────────────────────────────────

        @Transactional(readOnly = true)
        public Long countMembers(Long businessId) {
                businessService.requireAccess(businessId);
                return memberRepository.countActiveByBusinessId(businessId);
        }

        @Transactional(readOnly = true)
        public Long countActiveMembers(Long businessId) {
                businessService.requireAccess(businessId);
                return memberRepository.countActiveMembers(businessId);
        }

        // ── Private helpers ───────────────────────────────────────────────────────

        private Member findActiveMemberById(Long id) {
                return memberRepository.findActiveById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + id));
        }

        private MemberResponse mapToResponse(Member m) {
                String businessPublicId = businessRepository.findById(m.getBusinessId())
                        .map(Business::getPublicId)
                        .orElse(null);

                return MemberResponse.builder()
                        .id(m.getId())
                        .publicId(m.getPublicId())
                        .businessId(m.getBusinessId())
                        .businessPublicId(businessPublicId)
                        .firstName(m.getFirstName())
                        .lastName(m.getLastName())
                        .fullName(m.getFullName())
                        .phone(m.getPhone())
                        .email(m.getEmail())
                        .dateOfBirth(m.getDateOfBirth())
                        .gender(m.getGender())
                        .address(m.getAddress())
                        .status(m.getStatus())
                        .notes(m.getNotes())
                        .createdAt(m.getCreatedAt())
                        .updatedAt(m.getUpdatedAt())
                        .build();
        }
}