package com.managio.modules.memberships;

import com.managio.modules.memberships.service.RenewalReminders;
import com.managio.modules.auth.service.TokenCleanup;
import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Renewal reminders (MEM-9) and token cleanup. */
class ScheduledJobsTest extends IntegrationTest {

    private static final String B = "/api/v1/businesses/{b}";

    @Autowired
    private RenewalReminders reminders;
    @Autowired
    private TokenCleanup cleanup;

    private Session owner;
    private long business;

    private void setUp() {
        owner = verifiedUser("Ananya Rao", "ananya@example.com");
        business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
    }

    private long customer(String name, String email) {
        Map<String, Object> body = new HashMap<>(Map.of("fullName", name));
        if (email != null) {
            body.put("email", email);
        }
        return ok(post(owner.token(), B + "/customers", body, business), 201).path("id").asLong();
    }

    @Test
    void eachEndingMembershipIsRemindedOnceAndOnlyWhenNobodyHasRenewedIt() {
        setUp();
        long trial = createPlan(owner, business, "Trial", "300", 3);
        long fortnight = createPlan(owner, business, "Fortnight", "700", 14);
        long month = createPlan(owner, business, "Monthly", "1500", 30);

        long asha = customer("Asha Verma", "asha@example.com");
        long bela = customer("Bela Rao", null);
        long chitra = customer("Chitra Nair", "chitra@example.com");
        long dev = customer("Dev Patel", "dev@example.com");
        long esha = customer("Esha Iyer", "esha@example.com");
        long farid = customer("Farid Khan", "farid@example.com");

        startMembership(owner, business, asha, trial);          // ends in 2 days: reminded
        startMembership(owner, business, bela, trial);          // no email: nothing to send
        startMembership(owner, business, chitra, fortnight);    // ends in 13 days: too early
        long devMembership = startMembership(owner, business, dev, trial).path("id").asLong();
        ok(post(owner.token(), B + "/memberships/{m}/renew", Map.of(), business, devMembership), 201);   // renewed
        startMembership(owner, business, esha, trial);          // their next membership is already booked
        ok(post(owner.token(), B + "/memberships", Map.of("customerId", esha, "offeringId", month, "startsOn", "2026-10-23"), business), 201);
        JsonNode cancelled = startMembership(owner, business, farid, trial);
        ok(post(owner.token(), B + "/memberships/{m}/cancel", Map.of("reason", "Moved away"), business, cancelled.path("id").asLong()), 200);

        assertThat(reminders.run()).isEqualTo(1);
        assertThat(mailer.sentTo("asha@example.com")).hasSize(1);
        assertThat(mailer.sentTo("asha@example.com").get(0).subject()).isEqualTo("Your Trial ends on 22 Oct 2026");
        assertThat(mailer.sentTo("asha@example.com").get(0).text()).contains("Ironwood Fitness");
        for (String email : List.of("chitra@example.com", "dev@example.com", "esha@example.com", "farid@example.com")) {
            assertThat(mailer.sentTo(email)).as(email).isEmpty();
        }

        // Running again, even the next day, doesn't repeat the reminder.
        assertThat(reminders.run()).isZero();
        clock.advance(Duration.ofDays(1));
        assertThat(reminders.run()).isZero();
        assertThat(mailer.sentTo("asha@example.com")).hasSize(1);

        // Twelve days on, Chitra's fortnight is within three days of ending; nobody else has anything left to remind.
        clock.advance(Duration.ofDays(11));
        assertThat(reminders.run()).isEqualTo(1);
        assertThat(mailer.sentTo("chitra@example.com")).hasSize(1);
    }

    @Test
    void remindersCanBeSwitchedOffPerBusiness() {
        setUp();
        long trial = createPlan(owner, business, "Trial", "300", 3);
        startMembership(owner, business, customer("Asha Verma", "asha@example.com"), trial);

        JsonNode settings = ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("MEMBERSHIPS"),
                "timezone", "Asia/Kolkata", "invoicePrefix", "INV", "receiptPrefix", "RCT", "renewalReminders", false), business), 200);
        assertThat(settings.path("renewalReminders").asBoolean()).isFalse();
        assertThat(reminders.run()).isZero();

        // Leaving the field out of a later update keeps the choice; switching it on resumes reminders.
        assertThat(ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("MEMBERSHIPS"),
                "timezone", "Asia/Kolkata", "invoicePrefix", "INV", "receiptPrefix", "RCT"), business), 200)
                .path("renewalReminders").asBoolean()).isFalse();
        ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("MEMBERSHIPS"),
                "timezone", "Asia/Kolkata", "invoicePrefix", "INV", "receiptPrefix", "RCT", "renewalReminders", true), business), 200);
        assertThat(reminders.run()).isEqualTo(1);
    }

    @Test
    void remindersFollowEachBusinessesOwnDate() {
        setUp();
        // At 04:30 UTC it is 20 Oct in Kolkata but still 19 Oct in Los Angeles.
        long fourDays = createPlan(owner, business, "Four days", "300", 4);
        ok(post(owner.token(), B + "/memberships", Map.of("customerId", customer("Asha Verma", "asha@example.com"),
                "offeringId", fourDays, "startsOn", "2026-10-20"), business), 201);   // ends 23 Oct

        setTimezone("America/Los_Angeles");
        assertThat(reminders.run()).isZero();   // 19 Oct + 3 days reaches only the 22nd
        setTimezone("Asia/Kolkata");
        assertThat(reminders.run()).isEqualTo(1);   // 20 Oct + 3 days reaches the 23rd
    }

    private void setTimezone(String zone) {
        ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("MEMBERSHIPS"), "timezone", zone,
                "invoicePrefix", "INV", "receiptPrefix", "RCT"), business), 200);
    }

    @Test
    void expiredTokensAreRemovedAfterAWeek() {
        setUp();
        Long before = jdbc.sql("SELECT count(*) FROM refresh_tokens").query(Long.class).single();
        assertThat(before).isPositive();
        assertThat(cleanup.run()).isZero();

        clock.advance(Duration.ofDays(200));
        assertThat(cleanup.run()).isPositive();
        assertThat(jdbc.sql("SELECT count(*) FROM refresh_tokens").query(Long.class).single()).isZero();
        assertThat(jdbc.sql("SELECT count(*) FROM one_time_tokens").query(Long.class).single()).isZero();
    }
}
