package com.managio.modules.auth.repository;

import com.managio.modules.auth.domain.SignInEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignInEventRepository extends JpaRepository<SignInEvent, Long> {
}
