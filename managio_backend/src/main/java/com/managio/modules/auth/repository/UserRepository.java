package com.managio.modules.auth.repository;

import com.managio.modules.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** Emails are stored lowercase; callers pass a normalised email. */
    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleSubject(String googleSubject);

    @Query("select count(u) > 0 from User u where u.email = :email")
    boolean existsByEmail(String email);
}
