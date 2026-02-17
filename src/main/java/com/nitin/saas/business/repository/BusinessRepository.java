package com.nitin.saas.business.repository;

import com.nitin.saas.business.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessRepository extends JpaRepository<Business, Long> {

    List<Business> findByOwnerId(Long ownerId);

    @Query("SELECT b FROM Business b WHERE b.ownerId = :ownerId AND b.deletedAt IS NULL")
    List<Business> findActiveByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT b FROM Business b WHERE b.id = :id AND b.deletedAt IS NULL")
    Optional<Business> findActiveById(@Param("id") Long id);

    @Query("SELECT COUNT(b) FROM Business b WHERE b.ownerId = :ownerId AND b.deletedAt IS NULL")
    Long countActiveByOwnerId(@Param("ownerId") Long ownerId);

    Optional<Business> findByName(String name);
}