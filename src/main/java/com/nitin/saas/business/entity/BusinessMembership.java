package com.nitin.saas.business.entity;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.enums.BusinessRole;
import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name="Business_memberships",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"business_id", "user_id"})
        }
)
public class BusinessMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Business business;

    @ManyToOne(optional = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BusinessRole role;

    @Column(nullable = false)
    private boolean active = true;

    protected BusinessMembership() {}

    public BusinessMembership(Business business, User user, BusinessRole role) {
        this.business = business;
        this.user = user;
        this.role = role;
    }

    public BusinessRole getRole() {
        return role;
    }

    public User getUser() {
        return user;
    }
    public void deactivate() {
        this.active = false;
    }
}
