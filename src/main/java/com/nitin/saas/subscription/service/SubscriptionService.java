package com.nitin.saas.subscription.service;

import com.nitin.saas.business.service.BusinessService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.exception.ResourceNotFoundException;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.subscription.dto.AssignSubscriptionRequest;
import com.nitin.saas.subscription.dto.CreateSubscriptionPlanRequest;
import com.nitin.saas.subscription.dto.SubscriptionPlanResponse;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

        private final SubscriptionPlanRepository planRepository;
        private final MemberSubscriptionRepository subscriptionRepository;
        private final MemberRepository memberRepository;
        private final BusinessService businessService;

        @Transactional
        public SubscriptionPlanResponse createPlan(Long businessId, CreateSubscriptionPlanRequest request) {
                businessService.requireAccess(businessId);

                SubscriptionPlan plan = SubscriptionPlan.builder()
                        .businessId(businessId)
                        .name(request.getName())
                        .description(request.getDescription())
                        .price(request.getPrice())
                        .durationDays(request.getDurationDays())
                        .isActive(true)
                        .build();

                plan = planRepository.save(plan);
                log.info("Subscription plan created: {}", plan.getId());

                return mapPlanToResponse(plan);
        }

        @Transactional(readOnly = true)
        public List<SubscriptionPlanResponse> getActivePlans(Long businessId) {
                businessService.requireAccess(businessId);
                return planRepository.findActiveByBusinessId(businessId).stream()
                        .map(this::mapPlanToResponse)
                        .collect(Collectors.toList());
        }

        @Transactional
        public void assignSubscription(Long businessId, AssignSubscriptionRequest request) {
                businessService.requireAccess(businessId);

                Member member = memberRepository.findById(request.getMemberId())
                        .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

                if (!member.getBusinessId().equals(businessId)) {
                        throw new BadRequestException("Member does not belong to this business");
                }

                subscriptionRepository.findActiveSubscriptionByMemberId(member.getId())
                        .ifPresent(existingSub -> {
                                throw new BadRequestException("Member already has an active subscription");
                        });

                SubscriptionPlan plan = planRepository.findById(request.getPlanId())
                        .orElseThrow(() -> new ResourceNotFoundException("Plan not found"));

                if (!plan.getBusinessId().equals(businessId)) {
                        throw new BadRequestException("Plan does not belong to this business");
                }

                LocalDate startDate = LocalDate.now();
                LocalDate endDate = startDate.plusDays(plan.getDurationDays());

                MemberSubscription subscription = MemberSubscription.builder()
                        .memberId(member.getId())
                        .planId(plan.getId())
                        .startDate(startDate)
                        .endDate(endDate)
                        .status("ACTIVE")
                        .amount(plan.getPrice())
                        .build();

                subscriptionRepository.save(subscription);
                log.info("Subscription assigned: member={}, plan={}", member.getId(), plan.getId());
        }

        @Transactional
        public void expireSubscriptions() {
                List<MemberSubscription> expiredSubs = subscriptionRepository
                        .findExpiredSubscriptions(LocalDate.now());

                expiredSubs.forEach(sub -> {
                        sub.expire();
                        subscriptionRepository.save(sub);
                });

                log.info("Expired {} subscriptions", expiredSubs.size());
        }

        @Transactional(readOnly = true)
        public List<MemberSubscription> getExpiringSubscriptions(Long businessId, int days) {
                businessService.requireAccess(businessId);
                LocalDate start = LocalDate.now();
                LocalDate end = start.plusDays(days);
                return subscriptionRepository.findExpiringSubscriptions(start, end);
        }

        @Transactional(readOnly = true)
        public Long countActiveSubscriptions(Long businessId) {
                businessService.requireAccess(businessId);
                return subscriptionRepository.countActiveByBusinessId(businessId);
        }

        private SubscriptionPlanResponse mapPlanToResponse(SubscriptionPlan plan) {
                return SubscriptionPlanResponse.builder()
                        .id(plan.getId())
                        .businessId(plan.getBusinessId())
                        .name(plan.getName())
                        .description(plan.getDescription())
                        .price(plan.getPrice())
                        .durationDays(plan.getDurationDays())
                        .isActive(plan.getIsActive())
                        .createdAt(plan.getCreatedAt())
                        .build();
        }
}