package com.nitin.saas.member.repository;

import com.nitin.saas.member.entity.MemberEmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberEmailVerificationTokenRepository
        extends JpaRepository<MemberEmailVerificationToken, Long> {

    Optional<MemberEmailVerificationToken> findByToken(String token);

    void deleteAllByMemberId(Long memberId);
}