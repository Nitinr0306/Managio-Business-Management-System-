package com.nitin.saas.member.repository;

import com.nitin.saas.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    @Query("SELECT m FROM Member m WHERE m.businessId = :businessId AND m.deletedAt IS NULL")
    Page<Member> findActiveByBusinessId(@Param("businessId") Long businessId, Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Member> findActiveById(@Param("id") Long id);

    @Query("SELECT m FROM Member m WHERE m.businessId = :businessId AND " +
            "(LOWER(CONCAT(m.firstName, ' ', m.lastName)) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "m.phone LIKE CONCAT('%', :query, '%')) AND m.deletedAt IS NULL")
    Page<Member> searchMembers(@Param("businessId") Long businessId,
                               @Param("query") String query,
                               Pageable pageable);

    @Query("SELECT m FROM Member m WHERE m.businessId = :businessId AND m.status = :status AND m.deletedAt IS NULL")
    Page<Member> findByBusinessIdAndStatus(@Param("businessId") Long businessId,
                                           @Param("status") String status,
                                           Pageable pageable);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.businessId = :businessId AND m.deletedAt IS NULL")
    Long countActiveByBusinessId(@Param("businessId") Long businessId);

    @Query("SELECT COUNT(m) FROM Member m WHERE m.businessId = :businessId AND m.status = 'ACTIVE' AND m.deletedAt IS NULL")
    Long countActiveMembers(@Param("businessId") Long businessId);

    // ADD THESE QUERY METHODS TO MEMBER REPOSITORY

    @Query("SELECT m FROM Member m WHERE m.phone = :phone AND m.deletedAt IS NULL")
    Member findByPhone(@Param("phone") String phone);

    @Query("SELECT m FROM Member m WHERE m.email = :email AND m.deletedAt IS NULL")
    Member findByEmail(@Param("email") String email);

    @Query("SELECT m FROM Member m WHERE m.businessId = :businessId AND m.phone = :phone AND m.deletedAt IS NULL")
    Member findByBusinessIdAndPhone(@Param("businessId") Long businessId, @Param("phone") String phone);

    @Query("SELECT m FROM Member m WHERE m.businessId = :businessId AND m.email = :email AND m.deletedAt IS NULL")
    Member findByBusinessIdAndEmail(@Param("businessId") Long businessId, @Param("email") String email);

}