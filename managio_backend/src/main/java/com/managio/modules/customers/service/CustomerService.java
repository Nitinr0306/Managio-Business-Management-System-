package com.managio.modules.customers.service;

import com.managio.modules.customers.domain.Customer;
import com.managio.modules.customers.repository.CustomerQueries;
import com.managio.modules.customers.repository.CustomerRepository;
import com.managio.modules.business.service.BusinessSequences;
import com.managio.modules.business.service.BusinessSequences.Sequence;
import com.managio.modules.customers.domain.Customer.Details;
import com.managio.modules.customers.repository.CustomerQueries.Counts;
import com.managio.modules.customers.repository.CustomerQueries.CustomerRow;
import com.managio.modules.customers.repository.CustomerQueries.Sort;
import com.managio.modules.customers.repository.CustomerQueries.Standing;
import com.managio.modules.customers.repository.CustomerQueries.View;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Customers of a business (CUS-1 to CUS-4). */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;
    private final CustomerQueries queries;
    private final BusinessSequences sequences;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final Clock clock;

    public record CustomerResponse(long id, int number, String fullName, @Nullable String phone, @Nullable String email,
                                   @Nullable LocalDate dateOfBirth, @Nullable String gender, @Nullable String address,
                                   @Nullable String guardianName, @Nullable String guardianPhone,
                                   @Nullable String notes, String status, LocalDate joinedOn, boolean portalEnabled,
                                   String membershipState, @Nullable LocalDate coveredUntil, BigDecimal balanceDue,
                                   BigDecimal upcoming, List<GroupRef> groups, Instant createdAt) {
    }

    public record GroupRef(long id, String name) {
    }

    /** The editable fields plus whether the caller accepts a phone number another customer already has. */
    public record Input(Details details, LocalDate joinedOn, boolean confirmDuplicate) {
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerRow> list(BusinessContext ctx, View view, String search, Long groupId, Sort sort,
                                          int page, int size) {
        return queries.list(ctx.businessId(), ctx.today(clock), view, search, groupId, sort, page, size);
    }

    @Transactional(readOnly = true)
    public Counts counts(BusinessContext ctx) {
        return queries.counts(ctx.businessId(), ctx.today(clock));
    }

    @Transactional(readOnly = true)
    public CustomerResponse get(BusinessContext ctx, long customerId) {
        return describe(ctx, load(ctx.businessId(), customerId));
    }

    @Transactional
    public CustomerResponse create(BusinessContext ctx, Input input) {
        Details details = clean(input.details());
        guardDuplicatePhone(ctx.businessId(), details.phone(), null, input.confirmDuplicate());
        int number = sequences.next(ctx.businessId(), Sequence.CUSTOMER);
        LocalDate joinedOn = input.joinedOn() != null ? input.joinedOn() : ctx.today(clock);
        Customer customer = customers.save(Customer.create(ctx.businessId(), number, details, joinedOn, ctx.userId()));
        activity.record(ctx, "CUSTOMER_ADDED", "CUSTOMER", customer.getId(), "Added " + customer.getFullName() + " (#" + number + ")");
        return describe(ctx, customer);
    }

    @Transactional
    public CustomerResponse update(BusinessContext ctx, long customerId, Input input) {
        Customer customer = load(ctx.businessId(), customerId);
        Details details = clean(input.details());
        guardDuplicatePhone(ctx.businessId(), details.phone(), customerId, input.confirmDuplicate());
        if (customer.isPortalEnabled()) {
            requirePortalLoginFree(ctx.businessId(), details.email(), details.phone(), customerId);
        }
        if (customer.isPortalEnabled() && details.email() == null && details.phone() == null) {
            throw ApiException.invalidField("email", "Keep an email or phone number: this customer signs in with it");
        }
        customer.apply(details);
        if (input.joinedOn() != null) {
            customer.setJoinedOn(input.joinedOn());
        }
        activity.record(ctx, "CUSTOMER_UPDATED", "CUSTOMER", customerId, "Updated " + customer.getFullName());
        return describe(ctx, customer);
    }

    /**
     * POR-2: staff turn on portal access. CUS-3: the customer needs an email or phone that no other portal customer in
     * the business uses. An email is needed to send the invitation.
     */
    @Transactional
    public Customer enablePortal(BusinessContext ctx, long customerId) {
        Customer customer = requireActive(ctx.businessId(), customerId);
        if (customer.getEmail() == null) {
            throw ApiException.rule("portal.email_required", "Add an email address for " + customer.getFullName() + " to send the invitation.");
        }
        requirePortalLoginFree(ctx.businessId(), customer.getEmail(), customer.getPhone(), customerId);
        if (!customer.isPortalEnabled()) {
            customer.enablePortal();
            activity.record(ctx, "PORTAL_ENABLED", "CUSTOMER", customerId, "Gave " + customer.getFullName() + " portal access");
        }
        return customer;
    }

    @Transactional
    public Customer disablePortal(BusinessContext ctx, long customerId) {
        Customer customer = load(ctx.businessId(), customerId);
        if (customer.isPortalEnabled()) {
            customer.disablePortal();
            activity.record(ctx, "PORTAL_DISABLED", "CUSTOMER", customerId, "Removed portal access for " + customer.getFullName());
        }
        return customer;
    }

    /**
     * POR-2: self sign-up creates a new customer with portal access. It never attaches to an existing customer,
     * because nobody has proven that the email or phone belongs to the person signing up.
     */
    @Transactional
    public Customer registerSelf(long businessId, Details details, String passwordHash) {
        Details clean = clean(details);
        if (clean.email() == null && clean.phone() == null) {
            throw ApiException.invalidField("email", "Enter an email or a phone number to sign in with");
        }
        requirePortalLoginFree(businessId, clean.email(), clean.phone(), -1);
        int number = sequences.next(businessId, Sequence.CUSTOMER);
        Customer customer = Customer.create(businessId, number, clean, LocalDate.now(clock), null);
        customer.enablePortal();
        customer.setPortalPassword(passwordHash, clock.instant());
        customers.save(customer);
        activity.recordCustomer(businessId, customer.getId(), customer.getFullName(), "CUSTOMER_SIGNED_UP",
                customer.getFullName() + " (#" + number + ") signed up through the portal");
        return customer;
    }

    /** CUS-3: no other portal-enabled customer in the business may use the same email or phone to sign in. */
    void requirePortalLoginFree(long businessId, String email, String phone, long excludeId) {
        if (customers.portalLoginTaken(businessId, email, phone, excludeId)) {
            throw ApiException.conflict("customer.portal_login_taken",
                    "Another customer with portal access already uses this email or phone.");
        }
    }

    /** CUS-2: archived customers drop out of lists and lose portal access; their history stays. */
    @Transactional
    public CustomerResponse archive(BusinessContext ctx, long customerId) {
        Customer customer = load(ctx.businessId(), customerId);
        customer.archive();
        activity.record(ctx, "CUSTOMER_ARCHIVED", "CUSTOMER", customerId, "Archived " + customer.getFullName());
        return describe(ctx, customer);
    }

    @Transactional
    public CustomerResponse restore(BusinessContext ctx, long customerId) {
        Customer customer = load(ctx.businessId(), customerId);
        customer.restore();
        activity.record(ctx, "CUSTOMER_RESTORED", "CUSTOMER", customerId, "Restored " + customer.getFullName());
        return describe(ctx, customer);
    }

    /** CUS-2: other features call this before attaching memberships or invoices to a customer. */
    @Transactional(readOnly = true)
    public Customer requireActive(long businessId, long customerId) {
        Customer customer = load(businessId, customerId);
        if (customer.isArchived()) {
            throw ApiException.rule("customer.archived", customer.getFullName() + " is archived. Restore them first.");
        }
        return customer;
    }

    @Transactional(readOnly = true)
    public Customer load(long businessId, long customerId) {
        return customers.findByIdAndBusinessId(customerId, businessId).orElseThrow(() -> ApiException.notFound("Customer"));
    }

    /** CUS-4: a phone number already used by an active customer needs explicit confirmation. */
    void guardDuplicatePhone(long businessId, String phone, Long excludeId, boolean confirmed) {
        if (phone == null || confirmed) {
            return;
        }
        customers.findActiveByPhone(businessId, phone, excludeId).stream().findFirst().ifPresent(existing -> {
            throw ApiException.conflict("customer.duplicate_phone", existing.getFullName() + " (#" + existing.getNumber()
                    + ") already has this phone number. Add anyway if they share a phone.");
        });
    }

    static Details clean(Details d) {
        String name = Normalize.text(d.fullName());
        if (name == null) {
            throw ApiException.invalidField("fullName", "Enter a name");
        }
        return new Details(name, Normalize.phone(d.phone()), Normalize.email(d.email()), d.dateOfBirth(),
                Normalize.text(d.gender()), Normalize.text(d.address()), Normalize.text(d.guardianName()),
                Normalize.phone(d.guardianPhone()), Normalize.text(d.notes()));
    }

    private CustomerResponse describe(BusinessContext ctx, Customer c) {
        Standing standing = queries.standing(ctx.businessId(), c.getId(), ctx.today(clock));
        List<GroupRef> groups = jdbc.sql("""
                        SELECT g.id, g.name FROM customer_groups g
                        JOIN customer_group_members gm ON gm.group_id = g.id
                        WHERE gm.customer_id = :customerId ORDER BY lower(g.name)""")
                .param("customerId", c.getId())
                .query((rs, i) -> new GroupRef(rs.getLong("id"), rs.getString("name")))
                .list();
        return new CustomerResponse(c.getId(), c.getNumber(), c.getFullName(), c.getPhone(), c.getEmail(),
                c.getDateOfBirth(), c.getGender(), c.getAddress(), c.getGuardianName(), c.getGuardianPhone(),
                c.getNotes(), c.getStatus().name(), c.getJoinedOn(), c.isPortalEnabled(),
                standing.membershipState().name(), standing.coveredUntil(), standing.balanceDue(), standing.upcoming(),
                groups, c.getCreatedAt());
    }
}
