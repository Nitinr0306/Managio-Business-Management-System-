package com.nitin.saas.staff.entity;

import com.nitin.saas.staff.enums.StaffRole;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "staff_invitations", indexes = {
        @Index(name = "idx_invitation_business", columnList = "businessId"),
        @Index(name = "idx_invitation_email", columnList = "email"),
        @Index(name = "idx_invitation_token", columnList = "token"),
        @Index(name = "idx_invitation_expires", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long businessId;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StaffRole role;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    private LocalDateTime usedAt;

    private Long acceptedByUserId;

    @Column(nullable = false)
    private Long invitedBy;

    @Column(length = 500)
    private String message;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String designation;

    @Column(length = 45)
    private String inviterIpAddress;

    @Column(length = 255)
    private String inviterUserAgent;

    @Column(length = 45)
    private String acceptorIpAddress;

    @Column(length = 255)
    private String acceptorUserAgent;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !used && !isExpired();
    }

    public void markAsUsed(Long userId, String ipAddress, String userAgent) {
        this.used = true;
        this.usedAt = LocalDateTime.now();
        this.acceptedByUserId = userId;
        this.acceptorIpAddress = ipAddress;
        this.acceptorUserAgent = userAgent;
    }
}