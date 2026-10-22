package com.managio.platform.tenancy;

import com.managio.platform.security.Principals.UserPrincipal;
import com.managio.platform.error.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.sql.Array;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

/**
 * Tenancy gate for every {@code /api/v1/businesses/{businessId}/**} request (D-008).
 *
 * <ol>
 *   <li>Loads the caller's active membership in the business; answers 404 if there is none, so business IDs
 *       cannot be probed.</li>
 *   <li>Checks {@link RequireModule} and {@link RequirePermission} on the handler.</li>
 *   <li>Stores the resulting {@link BusinessContext} for the controller.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class BusinessAccessInterceptor implements HandlerInterceptor {

    static final String CONTEXT_ATTRIBUTE = BusinessContext.class.getName();
    private static final String OWNER_ROLE = "OWNER";

    private final JdbcClient jdbc;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        @SuppressWarnings("unchecked")
        Map<String, String> pathVariables =
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        String rawBusinessId = pathVariables == null ? null : pathVariables.get("businessId");
        if (rawBusinessId == null) {
            return true;
        }

        long businessId = parseId(rawBusinessId);
        UserPrincipal user = currentUser();
        BusinessContext context = loadMembership(businessId, user.id())
                .orElseThrow(() -> ApiException.notFound("Business"));

        RequireModule module = findAnnotation(method, RequireModule.class);
        if (module != null && !context.has(module.value())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "module.disabled",
                    "This feature is turned off for this business. An owner can turn it on in Settings.");
        }
        RequirePermission required = findAnnotation(method, RequirePermission.class);
        if (required != null) {
            for (Permission permission : required.value()) {
                context.require(permission);
            }
        }
        request.setAttribute(CONTEXT_ATTRIBUTE, context);
        return true;
    }

    private Optional<BusinessContext> loadMembership(long businessId, long userId) {
        return jdbc.sql("""
                        SELECT tm.id AS member_id, r.system_key, r.permissions, b.modules, b.timezone
                        FROM team_members tm
                        JOIN roles r ON r.id = tm.role_id
                        JOIN businesses b ON b.id = tm.business_id
                        JOIN users u ON u.id = tm.user_id
                        WHERE tm.business_id = :businessId AND tm.user_id = :userId
                          AND tm.status = 'ACTIVE' AND b.archived_at IS NULL AND u.status = 'ACTIVE'
                        """)
                .param("businessId", businessId)
                .param("userId", userId)
                .query((rs, rowNum) -> new BusinessContext(
                        businessId,
                        userId,
                        rs.getLong("member_id"),
                        OWNER_ROLE.equals(rs.getString("system_key")),
                        Permission.fromKeys(strings(rs.getArray("permissions"))),
                        Module.fromNames(strings(rs.getArray("modules"))),
                        ZoneId.of(rs.getString("timezone"))))
                .optional();
    }

    private static String[] strings(Array array) throws SQLException {
        return array == null ? new String[0] : (String[]) array.getArray();
    }

    private static UserPrincipal currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        throw ApiException.unauthorized("auth.required", "Sign in to continue.");
    }

    private static long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw ApiException.notFound("Business");
        }
    }

    private static <A extends java.lang.annotation.Annotation> A findAnnotation(HandlerMethod method, Class<A> type) {
        A onMethod = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), type);
        return onMethod != null ? onMethod : AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), type);
    }
}
