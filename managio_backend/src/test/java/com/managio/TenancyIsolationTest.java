package com.managio;

import com.managio.platform.security.AccessTokens;
import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** D-008: nobody reaches another business's data, and IDs from other businesses are indistinguishable from missing ones. */
class TenancyIsolationTest extends IntegrationTest {

    @Autowired
    AccessTokens accessTokens;

    @Test
    void outsidersSeeNothingOfAnotherBusiness() {
        Session ananya = verifiedUser("Ananya Rao", "ananya@example.com");
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long ironwood = createBusiness(ananya, "Ironwood Fitness", "FITNESS");
        createBusiness(rahul, "Corner Mart", "RETAIL");
        long ananyaRole = roleId(ananya, ironwood, "TRAINER");

        for (String uri : List.of("/api/v1/businesses/{id}", "/api/v1/businesses/{id}/team", "/api/v1/businesses/{id}/roles",
                "/api/v1/businesses/{id}/activity", "/api/v1/businesses/{id}/invitations")) {
            problem(get(rahul.token(), uri, ironwood), 404, "not_found");
        }
        problem(post(rahul.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "x@example.com", "roleId", ananyaRole), ironwood), 404, "not_found");
        problem(patch(rahul.token(), "/api/v1/businesses/{id}", Map.of("name", "Taken", "type", "OTHER"), ironwood),
                404, "not_found");
        // A business that does not exist answers exactly the same way.
        problem(get(rahul.token(), "/api/v1/businesses/{id}", 999_999), 404, "not_found");
    }

    @Test
    void recordsFromAnotherBusinessCannotBeReachedThroughYourOwn() {
        Session ananya = verifiedUser("Ananya Rao", "ananya@example.com");
        Session rahul = verifiedUser("Rahul Shah", "rahul@example.com");
        long ironwood = createBusiness(ananya, "Ironwood Fitness", "FITNESS");
        long mart = createBusiness(rahul, "Corner Mart", "RETAIL");
        long ironwoodRole = roleId(ananya, ironwood, "TRAINER");

        // Rahul uses his own business in the path but Ananya's role ID in the body or path.
        problem(post(rahul.token(), "/api/v1/businesses/{id}/invitations",
                Map.of("email", "x@example.com", "roleId", ironwoodRole), mart), 404, "not_found");
        problem(delete(rahul.token(), "/api/v1/businesses/{b}/roles/{r}", mart, ironwoodRole), 404, "not_found");
    }

    @Test
    void membersWithoutAPermissionAreRefused() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        Session trainer = joinAs(owner, business, roleId(owner, business, "TRAINER"), "Kiran", "kiran@example.com");

        ok(get(trainer.token(), "/api/v1/businesses/{id}", business), 200);
        problem(get(trainer.token(), "/api/v1/businesses/{id}/team", business), 403, "auth.forbidden");
        problem(get(trainer.token(), "/api/v1/businesses/{id}/activity", business), 403, "auth.forbidden");
        problem(patch(trainer.token(), "/api/v1/businesses/{id}", Map.of("name", "Mine now", "type", "FITNESS"), business),
                403, "auth.forbidden");
    }

    @Test
    void customerTokensCannotUseBusinessRoutes() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        String customerToken = accessTokens.forCustomer(owner.userId(), business).value();

        problem(get(customerToken, "/api/v1/businesses/{id}", business), 403, "auth.forbidden");
        problem(get(customerToken, "/api/v1/businesses/{id}/team", business), 403, "auth.forbidden");
    }

    @Test
    void activityRecordsWhoChangedWhat() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        ok(patch(owner.token(), "/api/v1/businesses/{id}", Map.of("name", "Ironwood Fitness Indiranagar", "type", "FITNESS"),
                business), 200);

        var items = ok(get(owner.token(), "/api/v1/businesses/{id}/activity", business), 200).path("items");
        assertThat(items.get(0).path("action").asString()).isEqualTo("BUSINESS_UPDATED");
        assertThat(items.get(0).path("actorName").asString()).isEqualTo("Ananya Rao");
        assertThat(items.get(1).path("action").asString()).isEqualTo("BUSINESS_CREATED");
    }
}
