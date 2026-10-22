package com.managio;

import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The OpenAPI document is what the frontend's types are generated from; these are the guarantees it relies on. */
class OpenApiContractTest extends IntegrationTest {

    private JsonNode docs() {
        return ok(get(null, "/v3/api-docs"), 200);
    }

    @Test
    void businessScopedPathsDeclareTheBusinessIdAndNeverTheServerSideContext() {
        JsonNode docs = docs();
        assertThat(docs.path("components").path("schemas").has("BusinessContext")).isFalse();
        List<String> problems = new ArrayList<>();
        docs.path("paths").properties().forEach(path -> path.getValue().properties().forEach(op -> {
            List<String> names = new ArrayList<>();
            op.getValue().path("parameters").forEach(p -> names.add(p.path("in").asString() + ":" + p.path("name").asString()));
            if (names.stream().anyMatch(n -> n.endsWith(":ctx"))) {
                problems.add(op.getKey() + " " + path.getKey() + " exposes ctx");
            }
            if (path.getKey().contains("{businessId}") && !names.contains("path:businessId")) {
                problems.add(op.getKey() + " " + path.getKey() + " lacks businessId");
            }
        }));
        assertThat(problems).isEmpty();
    }

    @Test
    void responsePropertiesArePresentAndOnlyMarkedOnesAreNullable() {
        JsonNode schemas = docs().path("components").path("schemas");
        JsonNode today = schemas.path("Today");
        List<String> required = new ArrayList<>();
        today.path("required").forEach(r -> required.add(r.asString()));
        assertThat(required).contains("date", "myTasks", "dues", "renewals");
        assertThat(today.path("properties").path("dues").path("anyOf").get(1).path("type").asString()).isEqualTo("null");
        assertThat(today.path("properties").path("date").path("type").asString()).isEqualTo("string");

        JsonNode customer = schemas.path("CustomerResponse").path("properties");
        assertThat(customer.path("phone").path("type").toString()).contains("null");
        assertThat(customer.path("fullName").path("type").asString()).isEqualTo("string");

        // Two records with the same simple name would silently merge into one schema.
        assertThat(schemas.path("ExpenseSummary").path("properties").path("byCategory").path("items").path("$ref").asString())
                .endsWith("/CategoryTotal");
        assertThat(schemas.path("CategoryTotal").path("properties").has("categoryId")).isTrue();
        assertThat(schemas.path("ProfitAndLoss").path("properties").path("expensesByCategory").path("items").path("$ref").asString())
                .endsWith("/CategoryAmount");

        // Request bodies keep their own required list, so optional inputs stay optional.
        List<String> taskBody = new ArrayList<>();
        schemas.path("TaskBody").path("required").forEach(r -> taskBody.add(r.asString()));
        assertThat(taskBody).containsExactly("title");
    }
}
