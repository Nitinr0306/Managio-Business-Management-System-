package com.managio.modules.admin.service;

import com.managio.modules.admin.dto.AdminDtos.AppStatus;
import com.managio.modules.admin.dto.AdminDtos.IntegrationStatus;
import com.managio.modules.admin.dto.AdminDtos.KeyCount;
import com.managio.modules.admin.dto.AdminDtos.PlatformHealth;
import com.managio.modules.admin.dto.AdminDtos.PlatformOverview;
import com.managio.modules.admin.dto.AdminDtos.PlatformUserRow;
import com.managio.modules.admin.dto.AdminDtos.TenantRow;
import com.managio.modules.admin.repository.AdminQueries;
import com.managio.modules.admin.repository.AdminQueries.PeopleView;
import com.managio.modules.admin.repository.AdminQueries.TenantSort;
import com.managio.modules.admin.repository.AdminQueries.TenantView;
import com.managio.modules.business.domain.BusinessType;
import com.managio.platform.config.ManagioProperties;
import com.managio.platform.tenancy.Module;
import com.managio.platform.web.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** The platform console's figures (D-028). Callers are checked by {@link AdminAccess} before they get here. */
@Service
@RequiredArgsConstructor
public class AdminService {

    /** Days and weeks on the console are counted in the owner's time zone, as the scheduled jobs are. */
    public static final String REPORTING_ZONE = "Asia/Kolkata";

    private static final int GROWTH_WEEKS = 12;
    private static final int ACTIVITY_DAYS = 30;

    private final AdminQueries queries;
    private final ManagioProperties properties;
    private final ObjectProvider<BuildProperties> build;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PlatformOverview overview() {
        Instant now = clock.instant();
        Map<String, Long> modules = queries.moduleCounts();
        List<KeyCount> moduleCounts = Arrays.stream(Module.values())
                .map(module -> new KeyCount(module.name(), modules.getOrDefault(module.name(), 0L)))
                .toList();
        return new PlatformOverview(now, REPORTING_ZONE, queries.totals(now), queries.growth(now, REPORTING_ZONE, GROWTH_WEEKS),
                queries.activity(now, REPORTING_ZONE, ACTIVITY_DAYS), queries.businessTypes(), moduleCounts,
                queries.features(now.minus(Duration.ofDays(30))), queries.activation(now));
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantRow> tenants(TenantView view, BusinessType type, String search, TenantSort sort, int page, int size) {
        return queries.tenants(clock.instant(), view, type == null ? null : type.name(), search, sort, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformUserRow> people(PeopleView view, String search, int page, int size) {
        return queries.people(clock.instant(), view, search, page, size);
    }

    @Transactional(readOnly = true)
    public PlatformHealth health() {
        Instant now = clock.instant();
        Runtime runtime = Runtime.getRuntime();
        BuildProperties info = build.getIfAvailable();
        AppStatus application = new AppStatus(info == null ? null : info.getVersion(), info == null ? null : info.getTime(),
                Runtime.version().toString(), ManagementFactory.getRuntimeMXBean().getUptime() / 1000,
                runtime.totalMemory() - runtime.freeMemory(), runtime.maxMemory(), runtime.availableProcessors());
        IntegrationStatus integrations = new IntegrationStatus(properties.mail().delivers(), properties.google().enabled());
        return new PlatformHealth(now, application, queries.database(), integrations, queries.signIns(now),
                queries.accounts(now));
    }
}
