package com.nitin.saas.dashboard.service;


import com.nitin.saas.business.entity.Business;
import com.nitin.saas.dashboard.dto.StaffDashboardResponse;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.enums.PaymentStatus;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class StaffDashboardService {

    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    public StaffDashboardService(
            MemberRepository memberRepository,
            MemberSubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository
    ) {
        this.memberRepository = memberRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    public StaffDashboardResponse getDashboard(Business business) {

        StaffDashboardResponse r = new StaffDashboardResponse();

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        r.membersAddedToday =
                memberRepository.countByBusinessAndCreatedAtAfter(
                        business,
                        startOfDay
                );

        r.activeMembers =
                memberRepository.countByBusinessAndActiveTrue(business);

        r.expiringSubscriptionsNext7Days =
                subscriptionRepository.countExpiringSoon(
                        business,
                        LocalDate.now().plusDays(7)
                );

        r.successfulPaymentsToday =
                paymentRepository.countByBusinessAndStatusAndCreatedAtAfter(
                        business,
                        PaymentStatus.SUCCESS,
                        startOfDay
                );

        r.failedPaymentsToday =
                paymentRepository.countByBusinessAndStatusAndCreatedAtAfter(
                        business,
                        PaymentStatus.FAILED,
                        startOfDay
                );

        r.recentPayments =
                paymentRepository.findRecentPayments(
                        business,
                        PageRequest.of(0, 10)
                );

        return r;
    }
}

