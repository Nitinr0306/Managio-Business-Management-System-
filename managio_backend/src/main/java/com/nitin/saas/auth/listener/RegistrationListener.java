package com.nitin.saas.auth.listener;

import com.nitin.saas.auth.event.UserRegisteredEvent;
import com.nitin.saas.common.email.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
@RequiredArgsConstructor
public class RegistrationListener {

    private final EmailNotificationService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRegistered(UserRegisteredEvent event) {

        emailService.sendVerificationEmail(
                event.getUser().getEmail(),
                event.getToken().getToken()
        );
    }
}