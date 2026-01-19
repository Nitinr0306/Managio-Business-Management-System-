package com.nitin.saas.dashboard.service;

import com.nitin.saas.dashboard.dto.MemberDashboardResponse;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.enums.SubscriptionStatus;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import org.springframework.stereotype.Service;

@Service
public class MemberDashboardService {

    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    public MemberDashboardService(
            MemberSubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    public MemberDashboardResponse getDashboard(Member member) {

        MemberDashboardResponse r = new MemberDashboardResponse();

        subscriptionRepository
                .findByMemberAndStatus(member, SubscriptionStatus.ACTIVE)
                .ifPresent(sub -> {
                    r.planName = sub.getPlan().getName();
                    r.subscriptionStatus = sub.getStatus().name();
                    r.startDate = sub.getStartDate();
                    r.endDate = sub.getEndDate();
                });

        r.payments =
                paymentRepository.findPaymentsForMember(member);

        return r;
    }
}
