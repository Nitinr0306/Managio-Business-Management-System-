package com.managio.modules.requests.service;

import com.managio.modules.notifications.service.Notifications;
import com.managio.modules.requests.domain.ApprovalRequest;
import com.managio.modules.requests.repository.ApprovalRequestRepository;
import com.managio.modules.billing.domain.Invoice;
import com.managio.modules.billing.service.InvoiceService;
import com.managio.modules.billing.domain.PaymentMethod;
import com.managio.modules.expenses.service.ExpenseCategoryService;
import com.managio.modules.expenses.service.ExpenseService;
import com.managio.modules.expenses.service.ExpenseService.ExpenseInput;
import com.managio.modules.requests.domain.ApprovalRequest.Details;
import com.managio.modules.requests.domain.ApprovalRequest.Status;
import com.managio.modules.requests.domain.ApprovalRequest.Type;
import com.managio.platform.util.Money;
import com.managio.platform.persistence.Persistence;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Requests and approvals (REQ-1 to REQ-4). Anyone on the team can ask; people with requests.approve decide,
 * but never their own request. Approving an expense claim records the expense, and approving a discount
 * applies it to the invoice.
 */
@Service
@RequiredArgsConstructor
public class RequestService {

    private final ApprovalRequestRepository requests;
    private final ExpenseService expenses;
    private final ExpenseCategoryService categories;
    private final InvoiceService invoices;
    private final Notifications notifications;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final Persistence persistence;
    private final Clock clock;

    public record RequestResponse(long id, Type type, String title, @Nullable String details,
                                  @Nullable BigDecimal amount, @Nullable Long expenseCategoryId,
                                  @Nullable LocalDate startsOn, @Nullable LocalDate endsOn, @Nullable Long invoiceId,
                                  Status status, long requestedByMemberId, String requestedBy,
                                  @Nullable String decidedBy, @Nullable Instant decidedAt,
                                  @Nullable String decisionNote, Instant createdAt, @Nullable Long expenseId) {
    }

    @Transactional(readOnly = true)
    public PageResponse<RequestResponse> list(BusinessContext ctx, Status status, boolean mine, int page, int size) {
        boolean seeAll = ctx.can(Permission.REQUESTS_APPROVE) && !mine;
        String where = " WHERE r.business_id = :businessId" + (status == null ? "" : " AND r.status = :status")
                + (seeAll ? "" : " AND r.requested_by_member_id = :memberId");
        var count = jdbc.sql("SELECT count(*) FROM requests r" + where).param("businessId", ctx.businessId());
        var rows = jdbc.sql(SELECT + where + " ORDER BY (r.status = 'PENDING') DESC, r.created_at DESC, r.id DESC LIMIT :limit OFFSET :offset")
                .param("businessId", ctx.businessId());
        if (status != null) {
            count = count.param("status", status.name());
            rows = rows.param("status", status.name());
        }
        if (!seeAll) {
            count = count.param("memberId", ctx.memberId());
            rows = rows.param("memberId", ctx.memberId());
        }
        long total = count.query(Long.class).single();
        List<RequestResponse> items = rows.param("limit", size).param("offset", page * size).query((rs, i) -> map(rs)).list();
        return PageResponse.of(items, page, size, total);
    }

    @Transactional(readOnly = true)
    public RequestResponse get(BusinessContext ctx, long requestId) {
        RequestResponse response = describe(ctx.businessId(), requestId);
        if (response.requestedByMemberId() != ctx.memberId() && !ctx.can(Permission.REQUESTS_APPROVE)) {
            throw ApiException.notFound("Request");
        }
        return response;
    }

    @Transactional
    public RequestResponse create(BusinessContext ctx, Details input) {
        Details details = validate(ctx, input);
        ApprovalRequest request = requests.save(ApprovalRequest.create(ctx.businessId(), ctx.memberId(), details));
        String requester = userName(ctx.userId());
        notifications.notifyApprovers(ctx.businessId(), ctx.userId(), "REQUEST_NEW", requester + " asked for approval",
                summary(details), "/app/" + ctx.businessId() + "/requests/" + request.getId());
        activity.record(ctx, "REQUEST_CREATED", "REQUEST", request.getId(), requester + " requested: " + details.title());
        return describe(ctx.businessId(), request.getId());
    }

    /** REQ-1: only the requester cancels, and only while it is pending. */
    @Transactional
    public RequestResponse cancel(BusinessContext ctx, long requestId) {
        ApprovalRequest request = requests.findForUpdate(requestId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Request"));
        if (request.getRequestedByMemberId() != ctx.memberId()) {
            throw ApiException.notFound("Request");
        }
        requirePending(request);
        request.cancel(clock.instant());
        activity.record(ctx, "REQUEST_CANCELLED", "REQUEST", requestId, "Cancelled request: " + request.getTitle());
        return describe(ctx.businessId(), requestId);
    }

    /** REQ-2, REQ-3: {@code method} is how an approved expense was paid and is required for expense claims. */
    @Transactional
    public RequestResponse approve(BusinessContext ctx, long requestId, String note, PaymentMethod method) {
        ApprovalRequest request = decidable(ctx, requestId);
        switch (request.getType()) {
            case EXPENSE -> {
                if (method == null) {
                    throw ApiException.invalidField("method", "Choose how the expense was paid");
                }
                expenses.create(ctx, new ExpenseInput(request.getExpenseCategoryId(), request.getAmount(), ctx.today(clock),
                        method, userNameOfMember(request.getRequestedByMemberId()), request.getTitle(), null), requestId, null);
            }
            case DISCOUNT -> invoices.addApprovedDiscount(ctx, request.getInvoiceId(), request.getAmount());
            default -> {
                // Purchases, leave and other requests are approval-only.
            }
        }
        request.decide(Status.APPROVED, ctx.userId(), clock.instant(), Normalize.text(note));
        notifyRequester(ctx, request, "approved");
        activity.record(ctx, "REQUEST_APPROVED", "REQUEST", requestId, "Approved request: " + request.getTitle());
        return describe(ctx.businessId(), requestId);
    }

    @Transactional
    public RequestResponse reject(BusinessContext ctx, long requestId, String note) {
        ApprovalRequest request = decidable(ctx, requestId);
        String clean = Normalize.text(note);
        if (clean == null) {
            throw ApiException.invalidField("note", "Say why, so they know what to change");
        }
        request.decide(Status.REJECTED, ctx.userId(), clock.instant(), clean);
        notifyRequester(ctx, request, "rejected");
        activity.record(ctx, "REQUEST_REJECTED", "REQUEST", requestId, "Rejected request: " + request.getTitle() + " (" + clean + ")");
        return describe(ctx.businessId(), requestId);
    }

    /** Pending requests waiting for the caller, for the Today view. */
    @Transactional(readOnly = true)
    public long pendingCount(BusinessContext ctx) {
        return jdbc.sql("SELECT count(*) FROM requests WHERE business_id = :businessId AND status = 'PENDING' AND requested_by_member_id <> :memberId")
                .param("businessId", ctx.businessId()).param("memberId", ctx.memberId()).query(Long.class).single();
    }

    private ApprovalRequest decidable(BusinessContext ctx, long requestId) {
        ctx.require(Permission.REQUESTS_APPROVE);
        ApprovalRequest request = requests.findForUpdate(requestId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Request"));
        if (request.getRequestedByMemberId() == ctx.memberId()) {
            throw ApiException.rule("request.own", "You can't decide your own request. Ask another approver.");
        }
        requirePending(request);
        return request;
    }

    private static void requirePending(ApprovalRequest request) {
        if (!request.isPending()) {
            throw ApiException.rule("request.closed", "This request was already " + request.getStatus().name().toLowerCase() + ".");
        }
    }

    private void notifyRequester(BusinessContext ctx, ApprovalRequest request, String outcome) {
        Long userId = jdbc.sql("SELECT user_id FROM team_members WHERE id = :id").param("id", request.getRequestedByMemberId())
                .query(Long.class).optional().orElse(null);
        if (userId != null) {
            notifications.notifyUser(userId, ctx.businessId(), "REQUEST_DECIDED", "Your request was " + outcome,
                    request.getTitle() + (request.getDecisionNote() == null ? "" : ": " + request.getDecisionNote()),
                    "/app/" + ctx.businessId() + "/requests/" + request.getId());
        }
    }

    /** Each type needs different fields; the checks name the field so the form can highlight it. */
    private Details validate(BusinessContext ctx, Details d) {
        String title = Normalize.text(d.title());
        if (title == null) {
            throw ApiException.invalidField("title", "Enter a short title");
        }
        BigDecimal amount = d.amount() == null ? null : Money.of(d.amount());
        switch (d.type()) {
            case EXPENSE, PURCHASE -> {
                if (amount == null || amount.signum() <= 0) {
                    throw ApiException.invalidField("amount", "Enter the amount");
                }
            }
            case DISCOUNT -> {
                if (amount == null || amount.signum() <= 0) {
                    throw ApiException.invalidField("amount", "Enter the discount amount");
                }
                if (d.invoiceId() == null) {
                    throw ApiException.invalidField("invoiceId", "Choose the invoice");
                }
                Invoice invoice = invoices.load(ctx.businessId(), d.invoiceId());
                if (invoice.isVoid() || invoice.getDiscount().add(amount).compareTo(invoice.getSubtotal()) > 0) {
                    throw ApiException.invalidField("amount", "The discount can't be more than the invoice amount");
                }
            }
            case LEAVE -> {
                if (d.startsOn() == null) {
                    throw ApiException.invalidField("startsOn", "Choose the first day");
                }
                if (d.endsOn() != null && d.endsOn().isBefore(d.startsOn())) {
                    throw ApiException.invalidField("endsOn", "The last day can't be before the first day");
                }
            }
            case OTHER -> {
                // Free text only.
            }
        }
        Long categoryId = null;
        if (d.type() == Type.EXPENSE) {
            if (d.expenseCategoryId() == null) {
                throw ApiException.invalidField("expenseCategoryId", "Choose a category");
            }
            categoryId = categories.get(ctx.businessId(), d.expenseCategoryId()).getId();
        }
        LocalDate endsOn = d.type() == Type.LEAVE && d.endsOn() == null ? d.startsOn() : d.endsOn();
        return new Details(d.type(), title, Normalize.text(d.details()), amount, categoryId, d.startsOn(), endsOn,
                d.type() == Type.DISCOUNT ? d.invoiceId() : null);
    }

    private static String summary(Details d) {
        return switch (d.type()) {
            case EXPENSE, PURCHASE, DISCOUNT -> d.title() + " (" + Money.inr(d.amount()) + ")";
            case LEAVE -> d.title() + " (" + d.startsOn() + (d.endsOn().equals(d.startsOn()) ? "" : " to " + d.endsOn()) + ")";
            case OTHER -> d.title();
        };
    }

    private String userName(long userId) {
        return jdbc.sql("SELECT full_name FROM users WHERE id = :id").param("id", userId).query(String.class).single();
    }

    private String userNameOfMember(long memberId) {
        return jdbc.sql("SELECT u.full_name FROM team_members tm JOIN users u ON u.id = tm.user_id WHERE tm.id = :id")
                .param("id", memberId).query(String.class).single();
    }

    private RequestResponse describe(long businessId, long requestId) {
        persistence.flush();
        return jdbc.sql(SELECT + " WHERE r.business_id = :businessId AND r.id = :id")
                .param("businessId", businessId).param("id", requestId).query((rs, i) -> map(rs)).optional()
                .orElseThrow(() -> ApiException.notFound("Request"));
    }

    private static final String SELECT = """
            SELECT r.id, r.type, r.title, r.details, r.amount, r.expense_category_id, r.starts_on, r.ends_on, r.invoice_id,
                   r.status, r.requested_by_member_id, ru.full_name AS requested_by, du.full_name AS decided_by,
                   r.decided_at, r.decision_note, r.created_at,
                   (SELECT e.id FROM expenses e WHERE e.request_id = r.id AND e.voided_at IS NULL LIMIT 1) AS expense_id
            FROM requests r
            JOIN team_members rm ON rm.id = r.requested_by_member_id
            JOIN users ru ON ru.id = rm.user_id
            LEFT JOIN users du ON du.id = r.decided_by_user_id""";

    private static RequestResponse map(ResultSet rs) throws SQLException {
        return new RequestResponse(rs.getLong("id"), Type.valueOf(rs.getString("type")), rs.getString("title"),
                rs.getString("details"), rs.getBigDecimal("amount"), (Long) rs.getObject("expense_category_id"),
                rs.getObject("starts_on", LocalDate.class), rs.getObject("ends_on", LocalDate.class),
                (Long) rs.getObject("invoice_id"), Status.valueOf(rs.getString("status")),
                rs.getLong("requested_by_member_id"), rs.getString("requested_by"), rs.getString("decided_by"),
                rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getString("decision_note"), rs.getTimestamp("created_at").toInstant(), (Long) rs.getObject("expense_id"));
    }
}
