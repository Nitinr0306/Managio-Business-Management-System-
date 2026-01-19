package com.nitin.saas.dashboard.service;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.dashboard.dto.DailyRevenuePoint;
import com.nitin.saas.dashboard.dto.RevenueAnalyticsResponse;
import com.nitin.saas.payment.enums.PaymentStatus;
import com.nitin.saas.payment.repository.PaymentRepository;
import com.nitin.saas.subscription.repository.MemberSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RevenueAnalyticsService {

    private final PaymentRepository paymentRepository;
    private final MemberSubscriptionRepository subscriptionRepository;

    public RevenueAnalyticsService(
            PaymentRepository paymentRepository,
            MemberSubscriptionRepository subscriptionRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public RevenueAnalyticsResponse getRevenueAnalytics(Business business) {

        RevenueAnalyticsResponse r = new RevenueAnalyticsResponse();

        r.dailyRevenueLast30Days = queryDailyRevenue(business);
        r.revenueByPlan = queryRevenueByPlan(business);
        r.revenueByPaymentProvider = queryRevenueByProvider(business);

        r.successfulPayments = countSuccessfulPayments(business);
        r.failedPayments = countFailedPayments(business);

        return r;
    }


    private List<DailyRevenuePoint> queryDailyRevenue(Business business) {
        return paymentRepository.dailyRevenue(
                business,
                LocalDateTime.now().minusDays(30)
        );
    }

    private Map<String, Long> queryRevenueByPlan(Business business) {
        return toMap(subscriptionRepository.revenueByPlan(business));
    }

    private Map<String, Long> queryRevenueByProvider(Business business) {
        return toMap(paymentRepository.revenueByProvider(business));
    }

    private long countSuccessfulPayments(Business business) {
        return paymentRepository.countByBusinessAndStatus(
                business,
                PaymentStatus.SUCCESS
        );
    }

    private long countFailedPayments(Business business) {
        return paymentRepository.countByBusinessAndStatus(
                business,
                PaymentStatus.FAILED
        );
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }
}


