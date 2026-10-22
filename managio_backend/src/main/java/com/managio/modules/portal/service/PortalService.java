package com.managio.modules.portal.service;

import com.managio.modules.billing.dto.BillingDtos.InvoiceRow;
import com.managio.modules.billing.dto.BillingDtos.PaymentResponse;
import com.managio.modules.billing.repository.BillingQueries;
import com.managio.modules.billing.repository.BillingQueries.InvoiceView;
import com.managio.modules.customers.domain.Customer;
import com.managio.modules.customers.repository.CustomerQueries;
import com.managio.modules.customers.repository.CustomerQueries.Standing;
import com.managio.modules.customers.repository.CustomerRepository;
import com.managio.modules.memberships.domain.Membership;
import com.managio.modules.memberships.repository.MembershipQueries;
import com.managio.platform.security.Principals.CustomerPrincipal;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.PageResponse;
import com.managio.modules.portal.dto.PortalDtos.BusinessPreview;
import com.managio.modules.portal.dto.PortalDtos.CustomerSummary;
import com.managio.modules.portal.dto.PortalDtos.PortalMembership;
import com.managio.modules.portal.dto.PortalDtos.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/** Read models for the signed-in customer, always limited to their own records (POR-3). */
@Service
@RequiredArgsConstructor
public class PortalService {

    private final CustomerRepository customers;
    private final CustomerQueries customerQueries;
    private final BillingQueries billing;
    private final JdbcClient jdbc;
    private final Clock clock;

    /** Today's date in the business's time zone. */
    public LocalDate today(CustomerPrincipal who) {
        String zone = jdbc.sql("SELECT timezone FROM businesses WHERE id = :id").param("id", who.businessId())
                .query(String.class).single();
        return LocalDate.now(clock.withZone(ZoneId.of(zone)));
    }

    /** The customer's own invoices; voided ones are left out. */
    @Transactional(readOnly = true)
    public PageResponse<InvoiceRow> invoices(CustomerPrincipal who, int page, int size) {
        return billing.invoices(who.businessId(), today(who), InvoiceView.NOT_VOID, who.id(), null, page, size);
    }

    /** The customer's own payments and refunds; voided ones are left out. */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> payments(CustomerPrincipal who, int page, int size) {
        return billing.payments(who.businessId(), null, null, null, who.id(), false, page, size);
    }

    @Transactional(readOnly = true)
    public Profile profile(CustomerPrincipal who) {
        Customer customer = customers.findByIdAndBusinessId(who.id(), who.businessId())
                .orElseThrow(() -> ApiException.notFound("Account"));
        BusinessPreview business = jdbc.sql("SELECT code, name, type, allow_customer_signup FROM businesses WHERE id = :id")
                .param("id", who.businessId())
                .query((rs, i) -> new BusinessPreview(rs.getString("code"), rs.getString("name"), rs.getString("type"),
                        rs.getBoolean("allow_customer_signup")))
                .single();
        Standing standing = customerQueries.standing(who.businessId(), who.id(), today(who));
        return new Profile(new CustomerSummary(customer.getId(), customer.getNumber(), customer.getFullName(),
                customer.getEmail(), customer.getPhone()), business, standing.membershipState().name(),
                standing.coveredUntil(), standing.balanceDue(), standing.upcoming());
    }

    @Transactional(readOnly = true)
    public List<PortalMembership> memberships(CustomerPrincipal who) {
        LocalDate today = today(who);
        return jdbc.sql("""
                        SELECT m.id, o.name AS plan_name, m.starts_on, m.ends_on, m.price, m.status
                        FROM memberships m JOIN offerings o ON o.id = m.offering_id
                        WHERE m.business_id = :businessId AND m.customer_id = :customerId
                        ORDER BY m.ends_on DESC, m.id DESC""")
                .param("businessId", who.businessId()).param("customerId", who.id())
                .query((rs, i) -> {
                    LocalDate startsOn = rs.getObject("starts_on", LocalDate.class);
                    LocalDate endsOn = rs.getObject("ends_on", LocalDate.class);
                    Membership.State state = MembershipQueries.stateOf(
                            Membership.Status.valueOf(rs.getString("status")), startsOn, endsOn, today);
                    return new PortalMembership(rs.getLong("id"), rs.getString("plan_name"), startsOn, endsOn,
                            rs.getBigDecimal("price"), state.name());
                }).list();
    }
}
