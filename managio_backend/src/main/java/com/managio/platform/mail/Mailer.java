package com.managio.platform.mail;

/** Delivers one email. Implementations must not throw for delivery failures; they log them. */
public interface Mailer {

    void send(MailMessage message);

    record MailMessage(String to, String subject, String html, String text) {
    }
}
