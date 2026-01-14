package com.nitin.saas.auth.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity@Table(name = "refresh_tokens")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryTime;

    public RefreshToken() {}

    public RefreshToken(String token, User user, LocalDateTime expiryTime) {
        this.token = token;
        this.user = user;
        this.expiryTime = expiryTime;
    }

    public String getToken() { return token; }
    public User getUser() { return user; }
    public LocalDateTime getExpiryTime() { return expiryTime; }
}
