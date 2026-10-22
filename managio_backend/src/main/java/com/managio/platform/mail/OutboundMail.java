package com.managio.platform.mail;

import com.managio.platform.mail.Mailer.MailMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sends email only once the surrounding transaction has committed, so a rolled-back action never emails
 * anyone and a link in an email always points at committed data.
 */
@Component
@RequiredArgsConstructor
public class OutboundMail {

    private final Mailer mailer;

    public void send(MailMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    mailer.send(message);
                }
            });
        } else {
            mailer.send(message);
        }
    }
}
