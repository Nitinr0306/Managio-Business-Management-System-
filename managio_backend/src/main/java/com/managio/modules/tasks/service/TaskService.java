package com.managio.modules.tasks.service;

import com.managio.modules.tasks.domain.Task;
import com.managio.modules.tasks.repository.TaskRepository;
import com.managio.modules.customers.service.CustomerService;
import com.managio.modules.tasks.domain.Task.Details;
import com.managio.modules.tasks.domain.Task.Status;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Tasks (TSK-1 to TSK-3). With tasks.manage you see and edit everyone's tasks. Without it you see tasks
 * assigned to you or created by you, create tasks for yourself, and move your own tasks along.
 */
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository tasks;
    private final CustomerService customers;
    private final ActivityLog activity;
    private final JdbcClient jdbc;
    private final Persistence persistence;
    private final Clock clock;

    public record PersonRef(long id, String name) {
    }

    public record TaskResponse(long id, String title, @Nullable String description, Status status,
                               Task.Priority priority, @Nullable LocalDate dueOn, Task.Repeat repeat,
                               @Nullable PersonRef assignee, @Nullable PersonRef customer, @Nullable String createdBy,
                               @Nullable Instant completedAt, boolean overdue) {
    }

    /** Which tasks a list shows. */
    public enum View { OPEN, DONE, ALL }

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(BusinessContext ctx, View view, Long assigneeId, boolean mine, int page, int size) {
        boolean seeAll = ctx.can(Permission.TASKS_MANAGE);
        String where = " WHERE t.business_id = :businessId"
                + (view == View.OPEN ? " AND t.status IN ('OPEN', 'IN_PROGRESS')" : view == View.DONE ? " AND t.status IN ('DONE', 'CANCELLED')" : "")
                + (seeAll ? "" : " AND (t.assignee_id = :memberId OR t.created_by_user_id = :userId)")
                + (mine ? " AND t.assignee_id = :memberId" : "")
                + (assigneeId == null ? "" : " AND t.assignee_id = :assigneeId");
        long total = bind(jdbc.sql("SELECT count(*) FROM tasks t" + where), ctx, assigneeId).query(Long.class).single();
        List<TaskResponse> rows = bind(jdbc.sql(SELECT + where + ORDER_AND_PAGE), ctx, assigneeId)
                .param("limit", size).param("offset", page * size)
                .query((rs, i) -> map(rs, ctx.today(clock))).list();
        return PageResponse.of(rows, page, size, total);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(BusinessContext ctx, long taskId) {
        Task task = load(ctx, taskId);
        requireVisible(ctx, task);
        return describe(ctx, task.getId());
    }

    /** TSK-1: without tasks.manage a task can only be for yourself. */
    @Transactional
    public TaskResponse create(BusinessContext ctx, Details details) {
        Details clean = validate(ctx, details, ctx.can(Permission.TASKS_MANAGE) ? details.assigneeId() : (Long) ctx.memberId());
        if (!ctx.can(Permission.TASKS_MANAGE) && details.assigneeId() != null && details.assigneeId() != ctx.memberId()) {
            throw ApiException.forbidden("You can only create tasks for yourself.");
        }
        Task task = tasks.save(Task.create(ctx.businessId(), clean, ctx.userId()));
        activity.record(ctx, "TASK_CREATED", "TASK", task.getId(), "Created task \"" + task.getTitle() + "\"");
        return describe(ctx, task.getId());
    }

    @Transactional
    public TaskResponse update(BusinessContext ctx, long taskId, Details details) {
        ctx.require(Permission.TASKS_MANAGE);
        Task task = load(ctx, taskId);
        task.apply(validate(ctx, details, details.assigneeId()));
        activity.record(ctx, "TASK_UPDATED", "TASK", taskId, "Updated task \"" + task.getTitle() + "\"");
        return describe(ctx, taskId);
    }

    /** TSK-1: the assignee or someone with tasks.manage changes the status. TSK-2: finishing a repeating task creates the next one. */
    @Transactional
    public TaskResponse setStatus(BusinessContext ctx, long taskId, Status status) {
        Task task = load(ctx, taskId);
        boolean assignee = task.getAssigneeId() != null && task.getAssigneeId() == ctx.memberId();
        if (!assignee && !ctx.can(Permission.TASKS_MANAGE)) {
            throw ApiException.forbidden("Only the person it's assigned to can update this task.");
        }
        boolean finishing = status == Status.DONE && task.isOpen();
        task.setStatus(status, clock.instant());
        if (finishing && task.getRepeat() != Task.Repeat.NONE) {
            Task next = Task.create(ctx.businessId(), new Details(task.getTitle(), task.getDescription(), task.getPriority(),
                    task.nextDue(ctx.today(clock)), task.getRepeat(), task.getAssigneeId(), task.getCustomerId()),
                    task.getCreatedByUserId() == null ? ctx.userId() : task.getCreatedByUserId());
            tasks.save(next);
        }
        activity.record(ctx, "TASK_STATUS_CHANGED", "TASK", taskId, "Marked \"" + task.getTitle() + "\" as "
                + status.name().toLowerCase().replace('_', ' '));
        return describe(ctx, taskId);
    }

    @Transactional
    public void delete(BusinessContext ctx, long taskId) {
        ctx.require(Permission.TASKS_MANAGE);
        Task task = load(ctx, taskId);
        tasks.delete(task);
        activity.record(ctx, "TASK_DELETED", "TASK", taskId, "Deleted task \"" + task.getTitle() + "\"");
    }

    /** Open tasks assigned to the caller, for the Today view. */
    @Transactional(readOnly = true)
    public List<TaskResponse> myOpen(BusinessContext ctx, int limit) {
        return jdbc.sql(SELECT + " WHERE t.business_id = :businessId AND t.assignee_id = :memberId AND t.status IN ('OPEN', 'IN_PROGRESS')"
                        + " ORDER BY t.due_on NULLS LAST, t.id LIMIT :limit")
                .param("businessId", ctx.businessId()).param("memberId", ctx.memberId()).param("limit", limit)
                .query((rs, i) -> map(rs, ctx.today(clock))).list();
    }

    private Task load(BusinessContext ctx, long taskId) {
        return tasks.findByIdAndBusinessId(taskId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Task"));
    }

    private void requireVisible(BusinessContext ctx, Task task) {
        boolean involved = (task.getAssigneeId() != null && task.getAssigneeId() == ctx.memberId())
                || (task.getCreatedByUserId() != null && task.getCreatedByUserId() == ctx.userId());
        if (!involved && !ctx.can(Permission.TASKS_MANAGE)) {
            throw ApiException.notFound("Task");
        }
    }

    private Details validate(BusinessContext ctx, Details d, Long assigneeId) {
        String title = Normalize.text(d.title());
        if (title == null) {
            throw ApiException.invalidField("title", "Enter what needs doing");
        }
        if (assigneeId != null) {
            boolean active = jdbc.sql("SELECT EXISTS (SELECT 1 FROM team_members WHERE id = :id AND business_id = :businessId AND status = 'ACTIVE')")
                    .param("id", assigneeId).param("businessId", ctx.businessId()).query(Boolean.class).single();
            if (!active) {
                throw ApiException.notFound("Team member");
            }
        }
        if (d.customerId() != null) {
            customers.load(ctx.businessId(), d.customerId());
        }
        return new Details(title, Normalize.text(d.description()), d.priority(), d.dueOn(), d.repeat(), assigneeId, d.customerId());
    }

    private TaskResponse describe(BusinessContext ctx, long taskId) {
        persistence.flush();
        return jdbc.sql(SELECT + " WHERE t.business_id = :businessId AND t.id = :id")
                .param("businessId", ctx.businessId()).param("id", taskId)
                .query((rs, i) -> map(rs, ctx.today(clock))).single();
    }

    private static JdbcClient.StatementSpec bind(JdbcClient.StatementSpec spec, BusinessContext ctx, Long assigneeId) {
        spec = spec.param("businessId", ctx.businessId()).param("memberId", ctx.memberId()).param("userId", ctx.userId());
        return assigneeId == null ? spec : spec.param("assigneeId", assigneeId);
    }

    private static final String ORDER_AND_PAGE = " ORDER BY (t.status IN ('DONE', 'CANCELLED')), t.due_on NULLS LAST,"
            + " CASE t.priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 ELSE 3 END, t.id"
            + " LIMIT :limit OFFSET :offset";

    private static final String SELECT = """
            SELECT t.id, t.title, t.description, t.status, t.priority, t.due_on, t.repeat, t.assignee_id,
                   au.full_name AS assignee_name, t.customer_id, c.full_name AS customer_name,
                   cu.full_name AS created_by, t.completed_at
            FROM tasks t
            LEFT JOIN team_members tm ON tm.id = t.assignee_id
            LEFT JOIN users au ON au.id = tm.user_id
            LEFT JOIN customers c ON c.id = t.customer_id
            LEFT JOIN users cu ON cu.id = t.created_by_user_id""";

    private static TaskResponse map(ResultSet rs, LocalDate today) throws SQLException {
        Status status = Status.valueOf(rs.getString("status"));
        LocalDate dueOn = rs.getObject("due_on", LocalDate.class);
        Long assigneeId = (Long) rs.getObject("assignee_id");
        Long customerId = (Long) rs.getObject("customer_id");
        return new TaskResponse(rs.getLong("id"), rs.getString("title"), rs.getString("description"), status,
                Task.Priority.valueOf(rs.getString("priority")), dueOn, Task.Repeat.valueOf(rs.getString("repeat")),
                assigneeId == null ? null : new PersonRef(assigneeId, rs.getString("assignee_name")),
                customerId == null ? null : new PersonRef(customerId, rs.getString("customer_name")),
                rs.getString("created_by"), rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                dueOn != null && dueOn.isBefore(today) && (status == Status.OPEN || status == Status.IN_PROGRESS));
    }
}
