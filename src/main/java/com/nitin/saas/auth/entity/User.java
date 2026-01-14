package com.nitin.saas.auth.entity;

import com.nitin.saas.auth.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public Role role;

    @Column(nullable = false)
    private String provider;

    @Column(unique = true)
    private String providerId;

    private int failedLoginAttempts ;
    private LocalDateTime accountLockedUntil;

    private LocalDateTime createdAt = LocalDateTime.now();
}
