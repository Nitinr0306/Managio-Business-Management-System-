package com.nitin.saas.subscription.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.audit.service.AuditLogService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.subscription.dto.AssignSubscriptionRequest;
import com.nitin.saas.subscription.dto.CreateSubscriptionPlanRequest;
import com.nitin.saas.subscription.dto.MemberSubscriptionResponse;
import com.nitin.saas.subscription.dto.SubscriptionPlanResponse;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import com.nitin.saas.staff.enums.StaffRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

        private final SubscriptionPlanRepository   planRepository;
        private final MemberSubscriptionRepository subscriptionRepository;
        private final MemberRepository             memberRepository;
        private final BusinessService              businessService;
        private final AuditLogService              auditLogService;

        @Transactional
        public SubscriptionPlanResponse createPlan(Long businessId, CreateSubscriptionPlanRequest request) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.MANAGE_SUBSCRIPTION_PLANS);
                SubscriptionPlan plan = SubscriptionPlan.builder()
                        .businessId(businessId)
                        .name(request.getName())
                        .description(request.getDescription())
                        .price(request.getPrice())
                        .durationDays(request.getDurationDays())
                        .isActive(true)
                        .build();
                plan = planRepository.save(plan);
                log.info("Subscription plan created: id={}, businessId={}", plan.getId(), businessId);
                return mapPlanToResponse(plan);
        }

        @Transactional(readOnly = true)
        public List<SubscriptionPlanResponse> getActivePlans(Long businessId) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_SUBSCRIPTIONS);
                return planRepository.findActiveByBusinessId(businessId).stream()
                        .map(this::mapPlanToResponse).collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public Page<MemberSubscriptionResponse> getBusinessSubscriptions(Long businessId, Pageable pageable) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_SUBSCRIPTIONS);
                Page<MemberSubscription> page = subscriptionRepository.findByBusinessId(businessId, pageable);
                List<MemberSubscription> subs = page.getContent();
                if (subs.isEmpty()) {
                        return new PageImpl<>(List.of(), pageable, page.getTotalElements());
                }

                Set<Long> memberIds = subs.stream()
                        .map(MemberSubscription::getMemberId)
                        .collect(Collectors.toSet());
                Set<Long> planIds = subs.stream()
                        .map(MemberSubscription::getPlanId)
                        .collect(Collectors.toSet());

                Map<Long, SubscriptionPlan> planById = planRepository.findAllById(planIds).stream()
                        .collect(Collectors.toMap(SubscriptionPlan::getId, Function.identity()));
                Map<Long, Member> memberById = memberRepository.findAllById(memberIds).stream()
                        .collect(Collectors.toMap(Member::getId, Function.identity()));

                List<MemberSubscriptionResponse> content = subs.stream()
                        .map(s -> mapMemberSubscriptionToResponse(
                                s,
                                memberById.get(s.getMemberId()),
                                planById.get(s.getPlanId())
                        ))
                        .collect(Collectors.toList());

                return new PageImpl<>(content, pageable, page.getTotalElements());
        }

        @Transactional
        public void assignSubscription(Long businessId, AssignSubscriptionRequest request) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.ASSIGN_SUBSCRIPTIONS);

                Member member = memberRepository.findById(request.getMemberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

                if (!member.getBusinessId().equals(businessId)) {
                        throw new BadRequestException("Member does not belong to this business");
                }

                subscriptionRepository.findActiveSubscriptionByMemberId(member.getId()).ifPresent(s -> {
                        throw new BadRequestException("Member already has an active subscription");
                });

                SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                        .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

                if (!plan.getBusinessId().equals(businessId)) {
                        throw new BadRequestException("Plan does not belong to this business");
                }

                LocalDate start = LocalDate.now();
                MemberSubscription sub = MemberSubscription.builder()
                        .memberId(member.getId())
                        .planId(plan.getId())
                        .startDate(start)
                        .endDate(start.plusDays(plan.getDurationDays()))
                        .status("ACTIVE")
                        .amount(plan.getPrice())
                        .build();

                subscriptionRepository.save(sub);
                log.info("Subscription assigned: memberId={}, planId={}", member.getId(), plan.getId());
        }

        @Transactional
        public void cancelSubscription(Long businessId, Long subscriptionId, String reason) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.CANCEL_SUBSCRIPTIONS);

                MemberSubscription sub = subscriptionRepository.findById(subscriptionId)
                        .orElseThrow(() -> new ResourceNotFoundException("Subscription not found"));

                Member member = memberRepository.findById(sub.getMemberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

                if (!member.getBusinessId().equals(businessId)) {
                        throw new BadRequestException("Subscription does not belong to this business");
                }

                if ("CANCELLED".equals(sub.getStatus())) {
                        return;
                }

                sub.setStatus("CANCELLED");
                subscriptionRepository.save(sub);

                auditLogService.logSubscriptionCancellation(businessId, subscriptionId, reason);
                log.info("Subscription cancelled: id={}, businessId={}, reason={}", subscriptionId, businessId, reason);
        }

        @Transactional
        public void expireSubscriptions() {
                List<MemberSubscription> expired = subscriptionRepository
                        .findExpiredSubscriptions(LocalDate.now());
                expired.forEach(s -> { s.expire(); subscriptionRepository.save(s); });
                if (!expired.isEmpty()) log.info("Expired {} subscriptions", expired.size());
        }

        @Transactional(readOnly = true)
        public List<MemberSubscription> getExpiringSubscriptions(Long businessId, int days) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_SUBSCRIPTIONS);
                LocalDate today = LocalDate.now();
                return subscriptionRepository.findExpiringByBusinessId(businessId, today, today.plusDays(days));
        }

        @Transactional(readOnly = true)
        public Long countActiveSubscriptions(Long businessId) {
                businessService.requireBusinessPermission(businessId, StaffRole.Permission.VIEW_SUBSCRIPTIONS);
                return subscriptionRepository.countActiveByBusinessId(businessId);
        }

        private SubscriptionPlanResponse mapPlanToResponse(SubscriptionPlan p) {
                return SubscriptionPlanResponse.builder()
                        .id(p.getId()).businessId(p.getBusinessId()).name(p.getName())
                        .description(p.getDescription()).price(p.getPrice())
                        .durationDays(p.getDurationDays()).isActive(p.getIsActive())
                        .createdAt(p.getCreatedAt()).build();
        }

        private MemberSubscriptionResponse mapMemberSubscriptionToResponse(
                MemberSubscription s,
                Member member,
                SubscriptionPlan plan
        ) {
                String memberName = member != null ? member.getFullName() : null;
                String memberEmail = member != null ? member.getEmail() : null;
                String memberPhone = member != null ? member.getPhone() : null;
                String planName = plan != null ? plan.getName() : "Plan #" + s.getPlanId();
                long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), s.getEndDate());
                return MemberSubscriptionResponse.builder()
                        .id(s.getId())
                        .memberId(s.getMemberId())
                        .memberName(memberName)
                        .memberEmail(memberEmail)
                        .memberPhone(memberPhone)
                        .planId(s.getPlanId())
                        .planName(planName)
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .status(s.getStatus())
                        .amount(s.getAmount())
                        .daysRemaining((int) daysRemaining)
                        .createdAt(s.getCreatedAt())
                        .build();
        }
}