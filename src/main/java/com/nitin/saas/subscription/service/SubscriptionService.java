package com.nitin.saas.subscription.service;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.*;
import com.nitin.saas.business.enums.BusinessRole;
import com.nitin.saas.business.service.BusinessAuthorizationService;
import com.nitin.saas.common.audit.service.AuditService;
import com.nitin.saas.subscription.enums.SubscriptionStatus;
import com.nitin.saas.business.repository.BusinessMembershipRepository;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import com.nitin.saas.subscription.repository.SubscriptionPlanRepository;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.subscription.entity.MemberSubscription;
import com.nitin.saas.subscription.entity.SubscriptionPlan;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;
    private final BusinessMembershipRepository membershipRepository;
    private final BusinessAuthorizationService authorizationService;
    private final AuditService auditService;

    public SubscriptionService(
            MemberSubscriptionRepository subscriptionRepository,
            SubscriptionPlanRepository planRepository,
            BusinessMembershipRepository membershipRepository,
            BusinessAuthorizationService authorizationService,
            AuditService auditService
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.membershipRepository = membershipRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    public MemberSubscription assignSubscription(
            Business business,
            User requester,
            Member member,
            String planCode
    ) {

        BusinessMembership membership =
                membershipRepository.findByBusinessAndUser(business, requester)
                        .orElseThrow(() ->
                                new IllegalStateException("Not part of business")
                        );

        if (membership.getRole() != BusinessRole.OWNER &&
                membership.getRole() != BusinessRole.STAFF) {
            throw new IllegalStateException("Not allowed to assign subscription");
        }

        if (!member.getBusiness().equals(business)) {
            throw new IllegalStateException("Member does not belong to business");
        }

        boolean hasActive =
                subscriptionRepository.existsByMemberAndStatus(
                        member,
                        SubscriptionStatus.ACTIVE
                );

        if (hasActive) {
            throw new IllegalStateException("Member already has active subscription");
        }

        SubscriptionPlan plan =
                planRepository.findByBusinessAndCode(business,planCode)
                        .orElseThrow(() ->
                                new IllegalStateException("Subscription plan not found")
                        );

        MemberSubscription subscription =
                new MemberSubscription(
                        member,
                        plan,
                        LocalDate.now()
                );

        return subscriptionRepository.save(subscription);
    }

    public void expireSubscriptions() {

        List<MemberSubscription> expiredSubscriptions =
                subscriptionRepository.findAllByStatusAndEndDateBefore(
                        SubscriptionStatus.ACTIVE,
                        LocalDate.now()
                );

        for (MemberSubscription subscription : expiredSubscriptions) {
            subscription.markExpired();
        }

        subscriptionRepository.saveAll(expiredSubscriptions);
    }
    public void cancelSubscription(
            Business business,
            User requester,
            MemberSubscription subscription,
            String reason
    ) {
        authorizationService.authorizeOwnerOrStaff(business, requester);

        subscription.cancel(LocalDate.now(),reason);
        subscriptionRepository.save(subscription);
        auditService.log(
                "CANCEL_SUBSCRIPTION",
                "SUBSCRIPTION",
                subscription.getId(),
                requester.getId(),
                reason
        );
    }

    @Transactional
    public void activateSubscriptionAfterPayment(
            MemberSubscription subscription
    ) {
        boolean hasActive =
                subscriptionRepository.existsByMemberAndStatus(
                        subscription.getMember(),
                        SubscriptionStatus.ACTIVE
                );

        if (hasActive) {
            throw new IllegalStateException(
                    "Member already has an active subscription"
            );
        }

        subscription.activate();
        subscriptionRepository.save(subscription);
    }


}