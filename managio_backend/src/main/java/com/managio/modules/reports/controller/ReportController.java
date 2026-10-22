package com.managio.modules.reports.controller;

import com.managio.modules.reports.service.ReportService;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequireModule;
import com.managio.platform.tenancy.RequirePermission;
import com.managio.modules.reports.service.ReportService.ProfitAndLoss;
import com.managio.modules.reports.service.ReportService.Runway;
import com.managio.modules.reports.service.ReportService.Today;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}")
@RequiredArgsConstructor
class ReportController {

    private final ReportService reports;

    /** The home screen. Every team member can open it; sections they may not see come back empty. */
    @GetMapping("/today")
    Today today(BusinessContext ctx) {
        return reports.today(ctx);
    }

    @GetMapping("/reports/runway")
    @RequirePermission(Permission.CUSTOMERS_VIEW)
    @RequireModule(Module.MEMBERSHIPS)
    Runway runway(BusinessContext ctx, @RequestParam(defaultValue = "30") int days) {
        return reports.runway(ctx, days);
    }

    @GetMapping("/reports/profit-loss")
    @RequirePermission(Permission.REPORTS_VIEW)
    ProfitAndLoss profitAndLoss(BusinessContext ctx,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reports.profitAndLoss(ctx, from, to);
    }
}
