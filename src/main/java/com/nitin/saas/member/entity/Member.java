package com.nitin.saas.member.entity;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(optional = false)
    private Business business;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Member() {}

    public Member(User user,Business business, String name, String phone) {
        this.user = user;
        this.business = business;
        this.name = name;
        this.phone = phone;
    }
    public void updateDetails(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public Business getBusiness() {
        return business;
    }
    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
