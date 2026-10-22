package com.managio.modules.tasks.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/** A piece of work for one team member, optionally about one customer and optionally repeating. */
@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task {

    public enum Status { OPEN, IN_PROGRESS, DONE, CANCELLED }

    public enum Priority { LOW, NORMAL, HIGH, URGENT }

    public enum Repeat { NONE, DAILY, WEEKLY, MONTHLY }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;
    private String title;
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status = Status.OPEN;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.NORMAL;

    private LocalDate dueOn;

    @Enumerated(EnumType.STRING)
    private Repeat repeat = Repeat.NONE;

    private Long assigneeId;
    private Long customerId;
    private Long createdByUserId;
    private Instant completedAt;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    /** The editable fields. */
    public record Details(String title, String description, Priority priority, LocalDate dueOn, Repeat repeat,
                          Long assigneeId, Long customerId) {
    }

    public static Task create(long businessId, Details d, long createdByUserId) {
        Task task = new Task();
        task.businessId = businessId;
        task.createdByUserId = createdByUserId;
        task.apply(d);
        return task;
    }

    public void apply(Details d) {
        this.title = d.title();
        this.description = d.description();
        this.priority = d.priority() == null ? Priority.NORMAL : d.priority();
        this.dueOn = d.dueOn();
        this.repeat = d.repeat() == null ? Repeat.NONE : d.repeat();
        this.assigneeId = d.assigneeId();
        this.customerId = d.customerId();
    }

    public boolean isOpen() {
        return status == Status.OPEN || status == Status.IN_PROGRESS;
    }

    public void setStatus(Status status, Instant now) {
        this.status = status;
        this.completedAt = status == Status.DONE ? now : null;
    }

    /** TSK-2: the next due date of a repeating task, counted from its due date (or today if it had none). */
    public LocalDate nextDue(LocalDate today) {
        LocalDate base = dueOn != null ? dueOn : today;
        return switch (repeat) {
            case DAILY -> base.plusDays(1);
            case WEEKLY -> base.plusWeeks(1);
            case MONTHLY -> base.plusMonths(1);
            case NONE -> throw new IllegalStateException("Task does not repeat");
        };
    }
}
