package com.managio.modules.business.service;

import com.managio.modules.business.domain.Business;
import com.managio.modules.business.domain.BusinessType;
import com.managio.modules.business.repository.BusinessRepository;
import com.managio.modules.team.domain.Role;
import com.managio.modules.business.dto.BusinessDtos.BusinessResponse;
import com.managio.modules.business.dto.BusinessDtos.MyBusinessResponse;
import com.managio.modules.business.dto.BusinessDtos.UpdateBusinessRequest;
import com.managio.modules.business.dto.BusinessDtos.UpdateSettingsRequest;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Array;
import java.sql.SQLException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Reading and changing a business's profile and settings (BUS-3). Creation lives in {@link BusinessOnboarding}. */
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final BusinessRepository businesses;
    private final JdbcClient jdbc;
    private final ActivityLog activity;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Business get(long businessId) {
        return businesses.findById(businessId).orElseThrow(() -> ApiException.notFound("Business"));
    }

    @Transactional(readOnly = true)
    public BusinessResponse describe(long businessId) {
        return BusinessResponse.from(get(businessId));
    }

    /** Every active business the user belongs to, with their role and permissions there. */
    @Transactional(readOnly = true)
    public List<MyBusinessResponse> mine(long userId) {
        return jdbc.sql("""
                        SELECT b.id, b.code, b.name, b.type, b.modules, r.id AS role_id, r.name AS role_name,
                               r.system_key, r.permissions
                        FROM team_members tm
                        JOIN businesses b ON b.id = tm.business_id
                        JOIN roles r ON r.id = tm.role_id
                        WHERE tm.user_id = :userId AND tm.status = 'ACTIVE' AND b.archived_at IS NULL
                        ORDER BY lower(b.name)""")
                .param("userId", userId)
                .query((rs, i) -> {
                    BusinessType type = BusinessType.valueOf(rs.getString("type"));
                    return new MyBusinessResponse(rs.getLong("id"), rs.getString("code"), rs.getString("name"), type,
                            type.labels(), Module.fromNames(strings(rs.getArray("modules"))), rs.getLong("role_id"),
                            rs.getString("role_name"), Role.OWNER_KEY.equals(rs.getString("system_key")),
                            Permission.fromKeys(strings(rs.getArray("permissions"))).stream().map(Permission::key).toList());
                })
                .list();
    }

    @Transactional
    public BusinessResponse updateProfile(BusinessContext ctx, UpdateBusinessRequest request) {
        Business business = get(ctx.businessId());
        business.updateProfile(request.name().trim(), request.type(), Normalize.phone(request.phone()),
                Normalize.email(request.email()), Normalize.text(request.addressLine()), Normalize.text(request.city()),
                Normalize.text(request.state()), Normalize.text(request.postalCode()),
                request.country() == null || request.country().isBlank() ? "India" : request.country().trim(),
                Normalize.text(request.gstin()));
        activity.record(ctx, "BUSINESS_UPDATED", "BUSINESS", business.getId(), "Updated business details");
        return BusinessResponse.from(business);
    }

    @Transactional
    public BusinessResponse updateSettings(BusinessContext ctx, UpdateSettingsRequest request) {
        Business business = get(ctx.businessId());
        Set<Module> modules = request.modules().isEmpty() ? EnumSet.noneOf(Module.class) : EnumSet.copyOf(request.modules());
        if (modules.contains(Module.INVENTORY) && !modules.contains(Module.CATALOG)) {
            throw ApiException.invalidField("modules", "Stock tracking needs Products and services turned on");
        }
        business.updateSettings(modules, validZone(request.timezone()), request.invoicePrefix(), request.receiptPrefix(),
                request.allowCustomerSignup(), request.renewalReminders() == null ? business.isRenewalReminders() : request.renewalReminders());
        activity.record(ctx, "BUSINESS_SETTINGS_UPDATED", "BUSINESS", business.getId(), "Updated business settings");
        return BusinessResponse.from(business);
    }

    /** BUS-3: only the owner archives; data is kept. */
    @Transactional
    public void archive(BusinessContext ctx) {
        if (!ctx.owner()) {
            throw ApiException.forbidden("Only the owner can archive the business.");
        }
        Business business = get(ctx.businessId());
        business.archive(clock.instant());
        activity.record(ctx, "BUSINESS_ARCHIVED", "BUSINESS", business.getId(), "Archived the business");
    }

    private static String validZone(String timezone) {
        try {
            return ZoneId.of(timezone).getId();
        } catch (DateTimeException e) {
            throw ApiException.invalidField("timezone", "Choose a valid time zone");
        }
    }

    private static String[] strings(Array array) throws SQLException {
        return array == null ? new String[0] : (String[]) array.getArray();
    }
}
