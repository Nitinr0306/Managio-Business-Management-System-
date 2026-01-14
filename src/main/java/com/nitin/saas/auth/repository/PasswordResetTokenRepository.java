package com.nitin.saas.auth.repository;

import com.nitin.saas.auth.entity.PasswordResetToken;
import com.nitin.saas.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken>findByUser(User user);
}
