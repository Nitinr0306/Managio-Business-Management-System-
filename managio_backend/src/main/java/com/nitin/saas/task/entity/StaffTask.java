package com.nitin.saas.task.entity;

import com.nitin.saas.common.utils.PublicIdGenerator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_tasks", indexes = {
        @Index(name = "idx_staff_tasks_business", columnList = "businessId"),
        @Index(name = "idx_staff_tasks_status", columnList = "status"),
        @Index(name = "idx_staff_tasks_priority", columnList = "priority"),
        @Index(name = "idx_staff_tasks_assignee", columnList = "assignedStaffId"),
        @Index(name = "idx_staff_tasks_due", columnList = "dueDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffTask {

    public enum Status {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    public enum Priority {
        LOW,
        MEDIUM,
        HIGH,
        URGENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String publicId;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    private LocalDate dueDate;

    private Long assignedStaffId;

    @Column(nullable = false)
    private Long createdByUserId;

    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @PrePersist
    private void assignPublicIdIfMissing() {
        if (publicId == null || publicId.isBlank()) {
            publicId = PublicIdGenerator.generate("TSK", 8);
        }
    }
}
