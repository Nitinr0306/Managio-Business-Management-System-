package com.managio.modules.team.repository;

import com.managio.modules.team.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByBusinessIdOrderByIdAsc(long businessId);

    Optional<Role> findByIdAndBusinessId(long id, long businessId);

    Optional<Role> findByBusinessIdAndSystemKey(long businessId, String systemKey);

    @Query("select count(r) > 0 from Role r where r.businessId = :businessId and lower(r.name) = lower(:name) "
            + "and (:excludeId is null or r.id <> :excludeId)")
    boolean nameTaken(long businessId, String name, Long excludeId);
}
