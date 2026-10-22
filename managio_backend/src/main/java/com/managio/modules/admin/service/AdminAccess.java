package com.managio.modules.admin.service;

import com.managio.modules.auth.repository.UserRepository;
import com.managio.platform.config.ManagioProperties;
import com.managio.platform.error.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides who may use the platform console (D-028): an active account whose email is on the configured list and
 * has been verified. Verification matters: without it, anyone could sign up with a listed address they don't own.
 * Checked on every request, so removing an address or disabling the account takes effect immediately.
 */
@Service
@RequiredArgsConstructor
public class AdminAccess {

    private final UserRepository users;
    private final ManagioProperties properties;

    @Transactional(readOnly = true)
    public boolean isAdmin(long userId) {
        return users.findById(userId)
                .filter(user -> user.isActive() && user.isEmailVerified() && properties.admin().includes(user.getEmail()))
                .isPresent();
    }

    public void require(long userId) {
        if (!isAdmin(userId)) {
            throw ApiException.forbidden("The platform console is only for Managio's administrators.");
        }
    }
}
