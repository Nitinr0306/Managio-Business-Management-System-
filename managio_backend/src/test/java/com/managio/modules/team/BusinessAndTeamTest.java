package com.managio.modules.team;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Businesses, roles, team and invitations: BUS-1, BUS-2, TEAM-1 to TEAM-9, IDN-8. */
class BusinessAndTeamTest extends IntegrationTest {

    @Test
    void creatingABusinessSeedsItsTypeAndMakesTheCreatorOwner() {
        Session owner = signUp("Ananya Rao", "ananya@example.com");

        JsonNode business = ok(post(owner.token(), "/api/v1/businesses",
                Map.of("name", "Ironwood Fitness", "type", "FITNESS", "city", "Bengaluru")), 201);

        assertThat(business.path("code").asString()).matches("[0-9]{4}[A-Z]{4}");
        assertThat(business.path("labels").path("customer").asString()).isEqualTo("Member");
        assertThat(names(business.path("modules"))).containsExactlyInAnyOrder("MEMBERSHIPS", "ATTENDANCE", "PAYROLL");
        long id = business.path("id").asLong();

        List<String> roles = new ArrayList<>();
        ok(get(owner.token(), "/api/v1/businesses/{id}/roles", id), 200).forEach(r -> roles.add(r.path("name").asString()));
        assertThat(roles).containsExactly("Owner", "Manager", "Front desk", "Trainer", "Accountant");
        assertThat(jdbc.sql("SELECT count(*) FROM expense_categories WHERE business_id = :id").param("id", id)
                .query(Long.class).single()).isEqualTo(7);

        JsonNode mine = ok(get(owner.token(), "/api/v1/me/businesses"), 200);
        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).path("owner").asBoolean()).isTrue();
        assertThat(mine.get(0).path("permissions")).hasSize(22);
    }

    @Test
    void schoolsAndShopsGetTheirOwnVocabularyAndRoles() {
        Session owner = signUp("Ananya Rao", "ananya@example.com");

        JsonNode school = ok(post(owner.token(), "/api/v1/businesses", Map.of("name", "Little Steps", "type", "SCHOOL")), 201);
        assertThat(school.path("labels").path("customers").asString()).isEqualTo("Students");
        assertThat(school.path("labels").path("group").asString()).isEqualTo("Class");

        long shop = ok(post(owner.token(), "/api/v1/businesses", Map.of("name", "Corner Mart", "type", "RETAIL")), 201)
                .path("id").asLong();
        List<String> roles = new ArrayList<>();
        ok(get(owner.token(), "/api/v1/businesses/{id}/roles", shop), 200).forEach(r -> roles.add(r.path("name").asString()));
        assertThat(roles).containsExactly("Owner", "Manager", "Cashier", "Stock keeper", "Accountant");
    }

    @Test
    void invitationsNeedAVerifiedSender() {
        Session owner = signUp("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");

        problem(post(owner.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "divya@example.com", "roleId", roleId(owner, business, "FRONT_DESK")), business),
                422, "invitation.unverified_sender");
    }

    @Test
    void anInviteeJoinsWithTheInvitedRole() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        long frontDesk = roleId(owner, business, "FRONT_DESK");

        ok(post(owner.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "Divya@Example.com", "roleId", frontDesk, "title", "Front desk lead"), business), 201);
        String token = mailer.lastTokenFor("divya@example.com");

        JsonNode preview = ok(post(null, "/api/v1/invitations/preview", Map.of("token", token)), 200);
        assertThat(preview.path("businessName").asString()).isEqualTo("Ironwood Fitness");
        assertThat(preview.path("roleName").asString()).isEqualTo("Front desk");
        assertThat(preview.path("accountExists").asBoolean()).isFalse();

        MvcTestResult accepted = post(null, "/api/v1/invitations/accept",
                Map.of("token", token, "fullName", "Divya Menon", "password", "correct-horse-1"));
        JsonNode session = ok(accepted, 200).path("session");
        assertThat(session.path("user").path("emailVerified").asBoolean()).isTrue();
        String divya = session.path("accessToken").asString();

        JsonNode mine = ok(get(divya, "/api/v1/me/businesses"), 200);
        assertThat(mine.get(0).path("roleName").asString()).isEqualTo("Front desk");
        assertThat(names(mine.get(0).path("permissions"))).contains("billing.collect").doesNotContain("team.manage");

        problem(post(null, "/api/v1/invitations/accept",
                Map.of("token", token, "fullName", "Divya Menon", "password", "correct-horse-1")), 422, "invitation.invalid");
    }

    @Test
    void anExistingAccountMustSignInToAccept() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        Session divya = signUp("Divya Menon", "divya@example.com");
        Session someoneElse = signUp("Rahul", "rahul@example.com");
        ok(post(owner.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "divya@example.com", "roleId", roleId(owner, business, "TRAINER")), business), 201);
        String token = mailer.lastTokenFor("divya@example.com");

        problem(post(null, "/api/v1/invitations/accept", Map.of("token", token, "fullName", "D", "password", "correct-horse-1")),
                409, "invitation.sign_in_required");
        problem(post(someoneElse.token(), "/api/v1/invitations/accept", Map.of("token", token)), 422, "invitation.wrong_account");

        JsonNode accepted = ok(post(divya.token(), "/api/v1/invitations/accept", Map.of("token", token)), 200);
        assertThat(accepted.path("businessId").asLong()).isEqualTo(business);
        assertThat(accepted.path("session").isNull() || accepted.path("session").isMissingNode()).isTrue();
    }

    @Test
    void invitationsExpireAfterSevenDaysAndResendReplacesTheLink() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        long invitation = ok(post(owner.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "divya@example.com", "roleId", roleId(owner, business, "TRAINER")), business), 201)
                .path("id").asLong();
        String first = mailer.lastTokenFor("divya@example.com");

        assertThat(post(owner.token(), "/api/v1/businesses/{b}/invitations/{i}/resend", null, business, invitation)).hasStatus(204);
        String second = mailer.lastTokenFor("divya@example.com");
        problem(post(null, "/api/v1/invitations/preview", Map.of("token", first)), 422, "invitation.invalid");
        ok(post(null, "/api/v1/invitations/preview", Map.of("token", second)), 200);

        clock.advance(Duration.ofDays(8));
        problem(post(null, "/api/v1/invitations/preview", Map.of("token", second)), 422, "invitation.invalid");
    }

    @Test
    void peopleCanOnlyHandOutAccessTheyHave() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        long hr = ok(post(owner.token(), "/api/v1/businesses/{id}/roles", Map.of("name", "HR",
                "permissions", List.of("TEAM_VIEW", "TEAM_MANAGE", "CUSTOMERS_VIEW", "ATTENDANCE_MARK")), business), 201)
                .path("id").asLong();
        Session meera = joinAs(owner, business, hr, "Meera", "meera@example.com");

        // TEAM-7: HR may not give out Manager, which holds permissions HR lacks.
        problem(post(meera.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "x@example.com", "roleId", roleId(owner, business, "MANAGER")), business), 403, "auth.forbidden");
        ok(post(meera.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "trainer@example.com", "roleId", roleId(owner, business, "TRAINER")), business), 201);
        // TEAM-6: only the owner edits roles.
        problem(post(meera.token(), "/api/v1/businesses/{id}/roles",
                Map.of("name", "Super", "permissions", List.of("SETTINGS_MANAGE")), business), 403, "auth.forbidden");
    }

    @Test
    void ownerAndSelfAreProtected() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        long ownerRole = roleId(owner, business, "OWNER");
        long ownerMember = memberId(owner, business, owner.userId());

        problem(post(owner.token(), "/api/v1/businesses/{b}/team/{m}/suspend", null, business, ownerMember), 422, "team.self");
        problem(patch(owner.token(), "/api/v1/businesses/{b}/roles/{r}",
                Map.of("name", "Boss", "permissions", List.of()), business, ownerRole), 422, "role.owner_locked");
        problem(post(owner.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "x@example.com", "roleId", ownerRole), business), 422, "role.owner_not_assignable");
        // TEAM-5: an unused role can be deleted; the Owner role never.
        assertThat(delete(owner.token(), "/api/v1/businesses/{b}/roles/{r}", business, roleId(owner, business, "MANAGER")))
                .hasStatus(204);
        problem(delete(owner.token(), "/api/v1/businesses/{b}/roles/{r}", business, ownerRole), 422, "role.owner_locked");
    }

    @Test
    void nobodyChangesTheirOwnRoleAndRolesInUseStay() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        long allTeam = ok(post(owner.token(), "/api/v1/businesses/{id}/roles", Map.of("name", "People lead",
                "permissions", List.of("TEAM_VIEW", "TEAM_MANAGE", "CUSTOMERS_VIEW", "ATTENDANCE_MARK")), business), 201)
                .path("id").asLong();
        Session lead = joinAs(owner, business, allTeam, "Meera", "meera@example.com");
        long leadMember = memberId(owner, business, lead.userId());

        // TEAM-3: even with team.manage, nobody moves themselves to another role.
        problem(patch(lead.token(), "/api/v1/businesses/{b}/team/{m}",
                Map.of("roleId", roleId(owner, business, "TRAINER")), business, leadMember), 422, "team.self");
        // TEAM-5: a role someone holds can't be deleted.
        problem(delete(owner.token(), "/api/v1/businesses/{b}/roles/{r}", business, allTeam), 422, "role.in_use");
    }

    @Test
    void onlyTheOwnerArchivesAndArchivedBusinessesDisappear() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");

        problem(post(manager.token(), "/api/v1/businesses/{id}/archive", null, business), 403, "auth.forbidden");
        assertThat(post(owner.token(), "/api/v1/businesses/{id}/archive", null, business)).hasStatus(204);

        // BUS-3: hidden from everyone, data kept.
        problem(get(owner.token(), "/api/v1/businesses/{id}", business), 404, "not_found");
        assertThat(ok(get(owner.token(), "/api/v1/me/businesses"), 200)).isEmpty();
        assertThat(jdbc.sql("SELECT count(*) FROM businesses WHERE id = :id").param("id", business).query(Long.class).single())
                .isEqualTo(1);
    }

    @Test
    void suspendedPeopleLoseAccessImmediately() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        Session divya = joinAs(owner, business, roleId(owner, business, "FRONT_DESK"), "Divya", "divya@example.com");
        ok(get(divya.token(), "/api/v1/businesses/{id}", business), 200);

        ok(post(owner.token(), "/api/v1/businesses/{b}/team/{m}/suspend", null, business, memberId(owner, business, divya.userId())), 200);

        problem(get(divya.token(), "/api/v1/businesses/{id}", business), 404, "not_found");
        assertThat(ok(get(divya.token(), "/api/v1/me/businesses"), 200)).isEmpty();
    }

    @Test
    void salariesAreVisibleOnlyWithPayrollPermission() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        Session manager = joinAs(owner, business, roleId(owner, business, "MANAGER"), "Ravi", "ravi@example.com");
        long managerMember = memberId(owner, business, manager.userId());
        JsonNode updated = ok(patch(owner.token(), "/api/v1/businesses/{b}/team/{m}", Map.of("monthlySalary", 25000), business, managerMember), 200);
        // The response itself must show the change, not the state before it.
        assertThat(updated.path("monthlySalary").decimalValue()).isEqualByComparingTo("25000");

        JsonNode asOwner = ok(get(owner.token(), "/api/v1/businesses/{b}/team/{m}", business, managerMember), 200);
        JsonNode asManager = ok(get(manager.token(), "/api/v1/businesses/{b}/team/{m}", business, managerMember), 200);
        assertThat(asOwner.path("monthlySalary").decimalValue()).isEqualByComparingTo("25000");
        assertThat(asManager.path("monthlySalary").isNull()).isTrue();
    }

    @Test
    void settingsValidateModulesAndPrefixes() {
        Session owner = signUp("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");

        problem(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("INVENTORY"),
                "timezone", "Asia/Kolkata", "invoicePrefix", "INV", "receiptPrefix", "RCT"), business), 400, "validation.failed");
        JsonNode saved = ok(put(owner.token(), "/api/v1/businesses/{id}/settings", Map.of("modules", List.of("CATALOG", "INVENTORY"),
                "timezone", "Asia/Dubai", "invoicePrefix", "IFI", "receiptPrefix", "IFR", "allowCustomerSignup", true), business), 200);
        assertThat(saved.path("timezone").asString()).isEqualTo("Asia/Dubai");
        assertThat(names(saved.path("modules"))).containsExactlyInAnyOrder("CATALOG", "INVENTORY");
    }

    private long memberId(Session session, long businessId, long userId) {
        for (JsonNode member : ok(get(session.token(), "/api/v1/businesses/{id}/team", businessId), 200)) {
            if (member.path("userId").asLong() == userId) {
                return member.path("id").asLong();
            }
        }
        throw new AssertionError("User " + userId + " is not on the team");
    }

    private static List<String> names(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(n -> values.add(n.asString()));
        return values;
    }
}
