package com.nitin.saas.member.repository;

import com.nitin.saas.business.entity.Business;
import com.nitin.saas.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;


public interface MemberRepository extends JpaRepository <Member,Long>{
    Optional<Member> findByUserId(Long userId);
    boolean existsByBusinessAndPhone(Business business,String phone);
    long countByBusiness(Business business);
    long countByBusinessAndActiveTrue(Business business);
    long countByBusinessAndCreatedAtAfter(
            Business business,
            LocalDateTime startOfDay
    );



}
