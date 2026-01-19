package com.nitin.saas.business.repository;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.business.entity.Business;
import com.nitin.saas.business.entity.BusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessMembershipRepository extends JpaRepository <BusinessMembership,Long>{
    Optional<BusinessMembership> findByBusinessAndUser(Business business, User user);
    boolean existsByBusinessAndUser(Business business, User user);
    Optional<BusinessMembership> findByBusinessAndUserId(
            Business business,
            Long userId
    );

}
