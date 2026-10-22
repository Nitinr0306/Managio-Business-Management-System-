package com.managio.modules.team.repository;

import com.managio.modules.team.domain.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    Optional<TeamMember> findByIdAndBusinessId(long id, long businessId);

    Optional<TeamMember> findByBusinessIdAndUserId(long businessId, long userId);
}
