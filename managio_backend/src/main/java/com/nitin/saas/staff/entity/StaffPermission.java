package com.nitin.saas.staff.entity;

import com.nitin.saas.staff.enums.StaffRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_permissions", indexes = {
        @Index(name = "idx_staff_perm_staff", columnList = "staffId"),
        @Index(name = "idx_staff_perm_permission", columnList = "permission")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_staff_permission", columnNames = {"staffId", "permission"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long staffId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private StaffRole.Permission permission;

    @Column(nullable = false)
    @Builder.Default
    private Boolean granted = true;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private Long grantedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;
}