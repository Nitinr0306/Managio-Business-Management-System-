package com.nitin.saas.member.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberEmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String token;

    private Long memberId;

    private LocalDateTime expiresAt;

    private boolean used;

    public boolean isValid() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }

    public void markAsUsed() {
        this.used = true;
    }
}