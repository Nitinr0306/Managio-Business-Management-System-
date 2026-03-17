package com.nitin.saas.business.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "businesses", indexes = {
        @Index(name = "idx_business_owner", columnList = "ownerId"),
        @Index(name = "idx_business_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String name;

    // FIX 11: these five fields were missing from the entity entirely.
    // BusinessForm.tsx sends them; businesses/page.tsx renders them.
    // Without the columns the DB silently drops every value on every save.
    @Column(length = 50)
    private String type;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Builder.Default
    private Integer memberCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer staffCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Version
    private Long version;

    public void incrementMemberCount() {
        this.memberCount++;
    }

    public void decrementMemberCount() {
        if (this.memberCount > 0) {
            this.memberCount--;
        }
    }

    public void incrementStaffCount() {
        if (this.staffCount == null) {
            this.staffCount = 0;
        }
        this.staffCount++;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}