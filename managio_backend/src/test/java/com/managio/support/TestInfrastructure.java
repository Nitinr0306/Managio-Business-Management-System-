package com.managio.support;

import com.managio.platform.mail.Mailer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared beans for integration tests: one PostgreSQL container, a controllable clock and a recording mailer. */
@TestConfiguration(proxyBeanMethods = false)
public class TestInfrastructure {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:17-alpine");
    }

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-10-20T04:30:00Z"));
    }

    @Bean
    @Primary
    RecordingMailer recordingMailer() {
        return new RecordingMailer();
    }

    /** A clock tests can move forward. Starts at 10:00 IST on Tuesday 20 October 2026. */
    public static class MutableClock extends Clock {

        private final Instant start;
        private final AtomicReference<Instant> now;

        MutableClock(Instant start) {
            this.start = start;
            this.now = new AtomicReference<>(start);
        }

        public void advance(java.time.Duration duration) {
            now.updateAndGet(i -> i.plus(duration));
        }

        public void set(Instant instant) {
            now.set(instant);
        }

        public void reset() {
            now.set(start);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            MutableClock self = this;
            return new Clock() {
                @Override
                public ZoneId getZone() {
                    return zone;
                }

                @Override
                public Clock withZone(ZoneId other) {
                    return self.withZone(other);
                }

                @Override
                public Instant instant() {
                    return self.instant();
                }
            };
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    /** Keeps every email the application sends so tests can follow its links. */
    public static class RecordingMailer implements Mailer {

        private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");
        private final List<MailMessage> sent = new CopyOnWriteArrayList<>();

        @Override
        public void send(MailMessage message) {
            sent.add(message);
        }

        public List<MailMessage> sentTo(String email) {
            return sent.stream().filter(m -> m.to().equals(email)).toList();
        }

        public MailMessage lastTo(String email) {
            List<MailMessage> messages = sentTo(email);
            if (messages.isEmpty()) {
                throw new AssertionError("No email was sent to " + email);
            }
            return messages.getLast();
        }

        /** The token from the link in the latest email to {@code email}. */
        public String lastTokenFor(String email) {
            Matcher matcher = TOKEN.matcher(lastTo(email).text());
            if (!matcher.find()) {
                throw new AssertionError("The latest email to " + email + " has no token link");
            }
            return matcher.group(1);
        }

        public void clear() {
            sent.clear();
        }
    }
}
