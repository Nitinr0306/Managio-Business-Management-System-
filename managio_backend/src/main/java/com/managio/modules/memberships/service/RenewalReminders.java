package com.managio.modules.memberships.service;

import com.managio.platform.mail.MailTemplates;
import com.managio.platform.mail.Mailer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Emails customers whose membership ends within {@link #DAYS_AHEAD} days and who have no later membership
 * (MEM-9). Each membership is reminded once. A membership is only marked reminded after the email was handed
 * to the mail service, so a failed send is retried on the next run.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RenewalReminders {

    static final int DAYS_AHEAD = 3;
    private static final int BATCH = 500;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    private final JdbcClient jdbc;
    private final Mailer mailer;
    private final Clock clock;

    private record Due(long membershipId, String customerName, String email, String planName, LocalDate endsOn,
                        String businessName) {
    }

    @Scheduled(cron = "${managio.jobs.renewal-reminders-cron:0 0 9 * * *}", zone = "Asia/Kolkata")
    void scheduled() {
        int sent = run();
        log.info("Renewal reminders sent: {}", sent);
    }

    /** Sends the reminders that are due now and returns how many went out. */
    public int run() {
        Timestamp now = Timestamp.from(clock.instant());
        List<Due> due = jdbc.sql("""
                        SELECT m.id, c.full_name, c.email, o.name AS plan_name, m.ends_on, b.name AS business_name
                        FROM memberships m
                        JOIN customers c ON c.id = m.customer_id
                        JOIN offerings o ON o.id = m.offering_id
                        JOIN businesses b ON b.id = m.business_id
                        WHERE m.status = 'ACTIVE' AND m.reminder_sent_at IS NULL
                          AND c.status = 'ACTIVE' AND c.email IS NOT NULL
                          AND b.archived_at IS NULL AND b.renewal_reminders AND 'MEMBERSHIPS' = ANY (b.modules)
                          AND m.ends_on BETWEEN (CAST(:now AS timestamptz) AT TIME ZONE b.timezone)::date
                                            AND (CAST(:now AS timestamptz) AT TIME ZONE b.timezone)::date + :daysAhead
                          AND NOT EXISTS (SELECT 1 FROM memberships later
                                          WHERE later.customer_id = m.customer_id AND later.status = 'ACTIVE'
                                            AND later.ends_on > m.ends_on)
                        ORDER BY m.id LIMIT :batch""")
                .param("now", now).param("daysAhead", DAYS_AHEAD).param("batch", BATCH)
                .query((rs, i) -> new Due(rs.getLong("id"), rs.getString("full_name"), rs.getString("email"),
                        rs.getString("plan_name"), rs.getObject("ends_on", LocalDate.class), rs.getString("business_name")))
                .list();
        int sent = 0;
        for (Due d : due) {
            try {
                mailer.send(MailTemplates.renewalReminder(d.email(), d.customerName(), d.businessName(), d.planName(),
                        DATE.format(d.endsOn())));
                jdbc.sql("UPDATE memberships SET reminder_sent_at = :now WHERE id = :id").param("now", now)
                        .param("id", d.membershipId()).update();
                sent++;
            } catch (RuntimeException e) {
                log.warn("Renewal reminder for membership {} failed; it will be retried", d.membershipId(), e);
            }
        }
        return sent;
    }
}
