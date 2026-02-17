package com.nitin.saas.subscription.controller;

import com.nitin.saas.subscription.dto.AssignSubscriptionRequest;
import com.nitin.saas.subscription.dto.CreateSubscriptionPlanRequest;
import com.nitin.saas.subscription.dto.SubscriptionPlanResponse;
import com.nitin.saas.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/subscriptions")
@RequiredArgsConstructor
@Tag(name = "Subscriptions", description = "Subscription management")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/plans")
    @Operation(summary = "Create subscription plan")
    public ResponseEntity<SubscriptionPlanResponse> createPlan(
            @PathVariable Long businessId,
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse response = subscriptionService.createPlan(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/plans")
    @Operation(summary = "Get active plans")
    public ResponseEntity<List<SubscriptionPlanResponse>> getActivePlans(
            @PathVariable Long businessId) {
        List<SubscriptionPlanResponse> response = subscriptionService.getActivePlans(businessId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assign")
    @Operation(summary = "Assign subscription to member")
    public ResponseEntity<Void> assignSubscription(
            @PathVariable Long businessId,
            @Valid @RequestBody AssignSubscriptionRequest request) {
        subscriptionService.assignSubscription(businessId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count")
    @Operation(summary = "Count active subscriptions")
    public ResponseEntity<Long> countActiveSubscriptions(@PathVariable Long businessId) {
        Long count = subscriptionService.countActiveSubscriptions(businessId);
        return ResponseEntity.ok(count);
    }
}