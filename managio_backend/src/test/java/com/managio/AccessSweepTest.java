package com.managio;

import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Walks every business-scoped route the API has, instead of a hand-picked list, so a new endpoint can't forget the
 * tenancy gate (D-008): outsiders get 404 everywhere, and members lacking a route's permission get 403.
 */
class AccessSweepTest extends IntegrationTest {

    private static final String BUSINESS_ROUTES = "/api/v1/businesses/{businessId}";
    private static final List<String> TABLES = List.of("customers", "invoices", "payments", "expenses", "tasks",
            "requests", "team_members", "invitations", "roles", "memberships", "offerings", "payroll_entries",
            "customer_groups", "attendance", "day_closes", "notifications", "businesses", "expense_categories");

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mappings;

    private record Route(HttpMethod method, String template, HandlerMethod handler, boolean multipart) {
        String uri(long businessId) {
            return template.replace("{businessId}", Long.toString(businessId)).replaceAll("\\{[^}]+}", "1");
        }

        boolean needsPermission() {
            return AnnotatedElementUtils.hasAnnotation(handler.getMethod(), RequirePermission.class)
                    || AnnotatedElementUtils.hasAnnotation(handler.getBeanType(), RequirePermission.class);
        }

        @Override
        public String toString() {
            return method + " " + template;
        }
    }

    private List<Route> businessRoutes() {
        List<Route> routes = new ArrayList<>();
        mappings.getHandlerMethods().forEach((info, handler) -> info.getPatternValues().stream()
                .filter(p -> p.startsWith(BUSINESS_ROUTES))
                .forEach(p -> info.getMethodsCondition().getMethods().stream().map(RequestMethod::asHttpMethod)
                        .forEach(m -> routes.add(new Route(m, p, handler, info.getConsumesCondition().getConsumableMediaTypes()
                                .contains(MediaType.MULTIPART_FORM_DATA))))));
        routes.sort(Comparator.comparing(Route::toString));
        return routes;
    }

    private MvcTestResult call(Route route, String token, long businessId) {
        var request = mvc.method(route.method()).uri(route.uri(businessId)).headers(h -> h.setBearerAuth(token));
        if (route.multipart()) {
            request = request.contentType("multipart/form-data; boundary=sweep").content("--sweep--");
        } else if (route.method() != HttpMethod.GET && route.method() != HttpMethod.DELETE) {
            request = request.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return request.exchange();
    }

    private Map<String, Long> rowCounts() {
        Map<String, Long> counts = new java.util.TreeMap<>();
        for (String table : TABLES) {
            counts.put(table, jdbc.sql("SELECT count(*) FROM " + table).query(Long.class).single());
        }
        return counts;
    }

    @Test
    void theSweepCoversTheWholeBusinessApi() {
        List<Route> routes = businessRoutes();
        assertThat(routes).hasSizeGreaterThan(90);
        assertThat(routes.stream().map(Route::toString)).contains(
                "POST /api/v1/businesses/{businessId}/invoices/{invoiceId}/payments",
                "PATCH /api/v1/businesses/{businessId}/team/{memberId}",
                "PUT /api/v1/businesses/{businessId}/settings",
                "GET /api/v1/businesses/{businessId}/reports/profit-loss",
                "POST /api/v1/businesses/{businessId}/customers/import");
    }

    @Test
    void outsidersGetNotFoundOnEveryBusinessRouteAndChangeNothing() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        enableAllModules(owner, business);
        long customer = createCustomer(owner, business, "Rohan Mehta", "9845012873");
        ok(post(owner.token(), "/api/v1/businesses/{b}/invoices", Map.of("customerId", customer,
                "lines", List.of(Map.of("description", "Training", "unitPrice", "500", "quantity", 1))), business), 201);
        Session outsider = verifiedUser("Vikram Shah", "vikram@example.com");
        createBusiness(outsider, "Vikram's Salon", "SALON");
        Map<String, Long> before = rowCounts();

        List<String> leaks = new ArrayList<>();
        for (Route route : businessRoutes()) {
            MvcTestResult result = call(route, outsider.token(), business);
            int status = result.getResponse().getStatus();
            if (status != 404) {
                leaks.add(route + " -> " + status);
            }
        }

        assertThat(leaks).isEmpty();
        assertThat(rowCounts()).isEqualTo(before);
    }

    @Test
    void membersWithoutARoutesPermissionAreRefused() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        enableAllModules(owner, business);
        long nothing = ok(post(owner.token(), "/api/v1/businesses/{b}/roles",
                Map.of("name", "No access", "permissions", List.of()), business), 201).path("id").asLong();
        Session member = joinAs(owner, business, nothing, "Divya", "divya@example.com");
        Map<String, Long> before = rowCounts();

        List<String> allowed = new ArrayList<>();
        List<Route> permissioned = businessRoutes().stream().filter(Route::needsPermission).toList();
        for (Route route : permissioned) {
            MvcTestResult result = call(route, member.token(), business);
            if (result.getResponse().getStatus() != 403) {
                allowed.add(route + " -> " + result.getResponse().getStatus());
            }
        }

        assertThat(permissioned).hasSizeGreaterThan(60);
        assertThat(allowed).isEmpty();
        assertThat(rowCounts()).isEqualTo(before);
    }

    @Test
    void aManagerWithEveryPermissionStillCannotDoWhatOnlyTheOwnerMay() {
        Session owner = verifiedUser("Ananya Rao", "ananya@example.com");
        long business = createBusiness(owner, "Ironwood Fitness", "FITNESS");
        List<String> every = Arrays.stream(Permission.values()).map(Permission::name).toList();
        long manager = ok(post(owner.token(), "/api/v1/businesses/{b}/roles",
                Map.of("name", "Deputy", "permissions", every), business), 201).path("id").asLong();
        Session meera = joinAs(owner, business, manager, "Meera", "meera@example.com");
        long ownerRole = roleId(owner, business, "OWNER");
        long ownerMember = jdbc.sql("SELECT id FROM team_members WHERE user_id = :u").param("u", owner.userId())
                .query(Long.class).single();

        problem(post(meera.token(), "/api/v1/businesses/{b}/archive", null, business), 403, "auth.forbidden");
        problem(post(meera.token(), "/api/v1/businesses/{b}/roles", Map.of("name", "Mine", "permissions", every), business),
                403, "auth.forbidden");
        problem(patch(meera.token(), "/api/v1/businesses/{b}/roles/{r}", Map.of("name", "Deputy", "permissions", List.of()),
                business, manager), 403, "auth.forbidden");
        problem(patch(meera.token(), "/api/v1/businesses/{b}/team/{m}", Map.of("roleId", manager), business, ownerMember),
                422, "team.owner");
        problem(post(meera.token(), "/api/v1/businesses/{b}/team/{m}/remove", null, business, ownerMember), 422, "team.owner");
        problem(post(meera.token(), "/api/v1/businesses/{b}/invitations",
                Map.of("email", "friend@example.com", "roleId", ownerRole), business), 422, "role.owner_not_assignable");
        assertThat(jdbc.sql("SELECT archived_at FROM businesses WHERE id = :b").param("b", business)
                .query(java.sql.Timestamp.class).optional()).isEmpty();
    }
}
