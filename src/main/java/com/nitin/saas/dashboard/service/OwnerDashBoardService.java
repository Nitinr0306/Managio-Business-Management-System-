package com.nitin.saas.dashboard.service;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.dashboard.dto.OwnerDashboardResponse;
import com.nitin.saas.member.repository.MemberRepository;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.enums.SubscriptionStatus;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

public class OwnerDashBoardService {
    @Service
    public class OwnerDashboardService {

        private final MemberRepository memberRepository;
        private final MemberSubscriptionRepository subscriptionRepository;
        private final PaymentRepository paymentRepository;

        public OwnerDashboardService(
                MemberRepository memberRepository,
                MemberSubscriptionRepository subscriptionRepository,
                PaymentRepository paymentRepository
        ) {
            this.memberRepository = memberRepository;
            this.subscriptionRepository = subscriptionRepository;
            this.paymentRepository = paymentRepository;
        }

        public OwnerDashboardResponse getDashboard(Business business) {

            OwnerDashboardResponse r = new OwnerDashboardResponse();

            r.totalMembers =
                    memberRepository.countByBusiness(business);

            r.activeMembers =
                    memberRepository.countByBusinessAndActiveTrue(business);

            r.activeSubscriptions =
                    subscriptionRepository.countByBusinessAndStatus(
                            business,
                            SubscriptionStatus.ACTIVE
                    );

            r.monthlyRevenue =
                    paymentRepository.sumMonthlyRevenue(business);

            r.todayRevenue =
                    paymentRepository.sumTodayRevenue(business);

            r.expiringSubscriptionsNext7Days =
                    subscriptionRepository.countExpiringSoon(
                            business,
                            LocalDate.now().plusDays(7)
                    );

            return r;
        }
    }}
