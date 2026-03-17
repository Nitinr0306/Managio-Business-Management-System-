package com.nitin.saas.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * L1 FIX: the class was accidentally package-private (missing {@code public}).
 * Jackson's serialiser and SpringDoc/Swagger both silently skip non-public
 * classes, so any endpoint returning this type would produce either an empty
 * response or a serialisation error at runtime.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSubscriptionResponse {
    private Long          id;
    private Long          memberId;
    private String        memberName;
    private String        memberEmail;
    private String        memberPhone;
    private Long          planId;
    private String        planName;
    private LocalDate     startDate;
    private LocalDate     endDate;
    private String        status;
    private BigDecimal    amount;
    private Integer       daysRemaining;
    private LocalDateTime createdAt;
}