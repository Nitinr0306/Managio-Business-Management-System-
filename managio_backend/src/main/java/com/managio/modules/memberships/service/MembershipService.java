package com.managio.modules.memberships.service;

import com.managio.modules.memberships.domain.Membership;
import com.managio.modules.memberships.repository.MembershipQueries;
import com.managio.modules.memberships.repository.MembershipQueries.MembershipRow;
import com.managio.modules.memberships.repository.MembershipQueries.View;
import com.managio.modules.memberships.repository.MembershipRepository;
import com.managio.modules.billing.dto.BillingDtos.InvoiceResponse;
import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.service.InvoiceService;
import com.managio.modules.billing.service.PaymentService;
import com.managio.modules.billing.service.PaymentService.PaymentInput;
import com.managio.modules.customers.domain.Customer;
import com.managio.modules.customers.service.CustomerService;
import com.managio.modules.memberships.domain.Membership.State;
import com.managio.modules.catalog.domain.Offering;
import com.managio.modules.catalog.service.OfferingService;
import com.managio.platform.util.Money;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Starting, renewing and cancelling memberships (MEM-1 to MEM-9). */
@Service
@RequiredArgsConstructor
public class MembershipService {

    private static final int MAX_DAYS_AHEAD = 366;

    private final MembershipRepository memberships;
    private final MembershipQueries queries;
    private final CustomerService customers;
    private final OfferingService offerings;
    private final InvoiceService invoices;
    private final PaymentService payments;
    private final ActivityLog activity;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<MembershipRow> list(BusinessContext ctx, View view, boolean needsRenewal, int page, int size) {
        return queries.list(ctx.businessId(), ctx.today(clock), view, needsRenewal, page, size);
    }

    /** {@code payNow} collects the first invoice immediately; it needs billing.collect. */
    public record StartRequest(long customerId, long offeringId, LocalDate startsOn, BigDecimal discount,
                               PaymentInput payNow) {
    }

    /** Renews with the same plan unless {@code offeringId} picks another. */
    public record RenewRequest(Long offeringId, BigDecimal discount, PaymentInput payNow) {
    }

    public record MembershipResponse(long id, long customerId, String customerName, long offeringId, String planName,
                                     LocalDate startsOn, LocalDate endsOn, BigDecimal price, String status, State state,
                                     @Nullable Long renewedFromId, @Nullable String cancelReason,
                                     List<InvoiceResponse> invoices) {
    }

    @Transactional
    public MembershipResponse start(BusinessContext ctx, StartRequest request) {
        Customer customer = customers.requireActive(ctx.businessId(), request.customerId());
        Offering plan = plan(ctx, request.offeringId());
        LocalDate startsOn = request.startsOn() != null ? request.startsOn() : ctx.today(clock);
        return create(ctx, customer, plan, startsOn, request.discount(), request.payNow(), null);
    }

    /** MEM-3: continues the day after the current end date, or starts today if it already ended. */
    @Transactional
    public MembershipResponse renew(BusinessContext ctx, long membershipId, RenewRequest request) {
        Membership previous = load(ctx.businessId(), membershipId);
        if (previous.getStatus() == Membership.Status.CANCELLED) {
            throw ApiException.rule("membership.cancelled", "This membership was cancelled. Start a new one instead.");
        }
        if (memberships.hasRenewal(membershipId)) {
            throw ApiException.rule("membership.already_renewed", "This membership has already been renewed.");
        }
        Customer customer = customers.requireActive(ctx.businessId(), previous.getCustomerId());
        Offering plan = plan(ctx, request.offeringId() != null ? request.offeringId() : previous.getOfferingId());
        LocalDate today = ctx.today(clock);
        LocalDate startsOn = previous.getEndsOn().isBefore(today) ? today : previous.getEndsOn().plusDays(1);
        return create(ctx, customer, plan, startsOn, request.discount(), request.payNow(), membershipId);
    }

    /** MEM-6: cancelling voids unpaid invoices that aren't due yet; money already paid stays as it is. */
    @Transactional
    public MembershipResponse cancel(BusinessContext ctx, long membershipId, String reason) {
        Membership membership = load(ctx.businessId(), membershipId);
        if (membership.getStatus() == Membership.Status.CANCELLED) {
            throw ApiException.rule("membership.cancelled", "This membership is already cancelled.");
        }
        membership.cancel(clock.instant(), Normalize.text(reason));
        int voided = invoices.voidUnpaidFuture(ctx, membershipId, "Membership cancelled");
        Customer customer = customers.load(ctx.businessId(), membership.getCustomerId());
        String voidedNote = voided == 0 ? "" : " and voided " + voided + " unpaid invoice" + (voided == 1 ? "" : "s");
        activity.record(ctx, "MEMBERSHIP_CANCELLED", "MEMBERSHIP", membershipId,
                "Cancelled " + customer.getFullName() + "'s membership" + voidedNote);
        return describe(ctx, membership);
    }

    @Transactional(readOnly = true)
    public MembershipResponse get(BusinessContext ctx, long membershipId) {
        return describe(ctx, load(ctx.businessId(), membershipId));
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> forCustomer(BusinessContext ctx, long customerId) {
        customers.load(ctx.businessId(), customerId);
        return memberships.findByBusinessIdAndCustomerIdOrderByEndsOnDescIdDesc(ctx.businessId(), customerId).stream()
                .map(m -> describe(ctx, m)).toList();
    }

    @Transactional(readOnly = true)
    public Membership load(long businessId, long membershipId) {
        return memberships.findByIdAndBusinessId(membershipId, businessId)
                .orElseThrow(() -> ApiException.notFound("Membership"));
    }

    private MembershipResponse create(BusinessContext ctx, Customer customer, Offering plan, LocalDate startsOn,
                                      BigDecimal discount, PaymentInput payNow, Long renewedFromId) {
        if (startsOn.isAfter(ctx.today(clock).plusDays(MAX_DAYS_AHEAD))) {
            throw ApiException.invalidField("startsOn", "Choose a start date within the next year");
        }
        LocalDate endsOn = plan.endDateFrom(startsOn);
        if (memberships.overlaps(ctx.businessId(), customer.getId(), plan.getId(), startsOn, endsOn)) {
            throw ApiException.conflict("membership.overlap", customer.getFullName() + " already has " + plan.getName()
                    + " during these dates. Renew it, or choose a different start date.");
        }
        if (payNow != null) {
            ctx.require(Permission.BILLING_COLLECT);
        }
        Membership membership = memberships.save(Membership.start(ctx.businessId(), customer.getId(), plan.getId(),
                startsOn, endsOn, plan.getPrice(), renewedFromId, ctx.userId()));
        List<Invoice> created = invoices.createForMembership(ctx, membership.getId(), customer.getId(), plan,
                plan.getPrice(), startsOn, endsOn, discount);
        if (payNow != null) {
            payments.record(ctx, created.getFirst().getId(), payNow);
        }
        String verb = renewedFromId == null ? "started" : "renewed";
        activity.record(ctx, renewedFromId == null ? "MEMBERSHIP_STARTED" : "MEMBERSHIP_RENEWED", "MEMBERSHIP",
                membership.getId(), customer.getFullName() + "'s " + plan.getName() + " " + verb + ", " + startsOn
                        + " to " + endsOn + " (" + Money.inr(plan.getPrice()) + ")");
        return describe(ctx, membership);
    }

    private Offering plan(BusinessContext ctx, long offeringId) {
        Offering offering = offerings.get(ctx.businessId(), offeringId);
        if (offering.getKind() != Offering.Kind.PLAN) {
            throw ApiException.rule("offering.not_plan", offering.getName() + " isn't a plan.");
        }
        if (!offering.isActive()) {
            throw ApiException.rule("offering.inactive", offering.getName() + " is no longer sold.");
        }
        return offering;
    }

    private MembershipResponse describe(BusinessContext ctx, Membership m) {
        Customer customer = customers.load(ctx.businessId(), m.getCustomerId());
        Offering plan = offerings.get(ctx.businessId(), m.getOfferingId());
        return new MembershipResponse(m.getId(), customer.getId(), customer.getFullName(), plan.getId(), plan.getName(),
                m.getStartsOn(), m.getEndsOn(), m.getPrice(), m.getStatus().name(), m.state(ctx.today(clock)),
                m.getRenewedFromId(), m.getCancelReason(), invoices.forMembership(ctx, m.getId()));
    }
}
