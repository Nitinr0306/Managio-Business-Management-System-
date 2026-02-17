package com.nitin.saas.member.service;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.repository.BusinessRepository;
import com.nitin.saas.business.service.BusinessService;
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

        private final MemberRepository memberRepository;
        private final BusinessService businessService;
        private final BusinessRepository businessRepository;
        private final com.nitin.saas.audit.service.AuditLogService auditLogService;

        @Transactional
        public MemberResponse createMember(Long businessId, CreateMemberRequest request) {
                businessService.requireAccess(businessId);

                Member member = Member.builder()
                        .businessId(businessId)
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .phone(request.getPhone())
                        .email(request.getEmail())
                        .dateOfBirth(request.getDateOfBirth())
                        .gender(request.getGender())
                        .address(request.getAddress())
                        .notes(request.getNotes())
                        .status("ACTIVE")
                        .build();

                member = memberRepository.save(member);

                businessRepository.findById(businessId).ifPresent(business -> {
                        business.incrementMemberCount();
                        businessRepository.save(business);
                });

                log.info("Member created: {} for business: {}", member.getId(), businessId);
                return mapToResponse(member);
        }

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

        @Transactional
        public MemberResponse updateMember(Long id, CreateMemberRequest request) {
                Member member = findActiveMemberById(id);
                businessService.requireAccess(member.getBusinessId());

                member.setFirstName(request.getFirstName());
                member.setLastName(request.getLastName());
                member.setPhone(request.getPhone());
                member.setEmail(request.getEmail());
                member.setDateOfBirth(request.getDateOfBirth());
                member.setGender(request.getGender());
                member.setAddress(request.getAddress());
                member.setNotes(request.getNotes());

                member = memberRepository.save(member);
                log.info("Member updated: {}", id);

                return mapToResponse(member);
        }

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

                // Audit log
                auditLogService.logMemberDeactivation(member.getBusinessId(), member.getId(), member.getFullName());

                log.info("Member deactivated: {}", id);
        }

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

        private Member findActiveMemberById(Long id) {
                return memberRepository.findActiveById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + id));
        }

        private MemberResponse mapToResponse(Member member) {
                return MemberResponse.builder()
                        .id(member.getId())
                        .businessId(member.getBusinessId())
                        .firstName(member.getFirstName())
                        .lastName(member.getLastName())
                        .fullName(member.getFullName())
                        .phone(member.getPhone())
                        .email(member.getEmail())
                        .dateOfBirth(member.getDateOfBirth())
                        .gender(member.getGender())
                        .address(member.getAddress())
                        .status(member.getStatus())
                        .notes(member.getNotes())
                        .createdAt(member.getCreatedAt())
                        .updatedAt(member.getUpdatedAt())
                        .build();
        }
}