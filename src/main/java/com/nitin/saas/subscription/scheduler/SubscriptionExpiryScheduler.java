package com.nitin.saas.subscription.scheduler;

import com.nitin.saas.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionExpiryScheduler {

    private final SubscriptionService subscriptionService;

    @Scheduled(cron = "0 0 0 * * *")
    public void expireSubscriptions() {
        log.info("Running subscription expiry check");
        subscriptionService.expireSubscriptions();
    }
}