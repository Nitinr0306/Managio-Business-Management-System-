package com.nitin.saas.staff.entity;

import com.nitin.saas.staff.enums.StaffRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff", indexes = {
        @Index(name = "idx_staff_business", columnList = "businessId"),
        @Index(name = "idx_staff_user", columnList = "userId"),
        @Index(name = "idx_staff_status", columnList = "status"),
        @Index(name = "idx_staff_role", columnList = "role")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_business_user", columnNames = {"businessId", "userId"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Staff {
  // Work on staff salary data
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StaffRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate hireDate;

    private LocalDate terminationDate;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String designation;

    @Column(precision = 10, scale = 2)
    private BigDecimal salary;

    @Column(length = 20)
    private String employeeId;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String emergencyContact;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canLogin = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canManageMembers = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canManagePayments = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canManageSubscriptions = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canViewReports = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Long version;

    public enum StaffStatus {
        ACTIVE,
        ON_LEAVE,
        SUSPENDED,
        TERMINATED
    }

    public void terminate(LocalDate terminationDate) {
        this.status = StaffStatus.TERMINATED;
        this.terminationDate = terminationDate;
        this.canLogin = false;
    }

    public void suspend() {
        this.status = StaffStatus.SUSPENDED;
        this.canLogin = false;
    }

    public void activate() {
        this.status = StaffStatus.ACTIVE;
        this.canLogin = true;
    }

    public boolean isActive() {
        return status == StaffStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = StaffStatus.TERMINATED;
        this.canLogin = false;
    }
}