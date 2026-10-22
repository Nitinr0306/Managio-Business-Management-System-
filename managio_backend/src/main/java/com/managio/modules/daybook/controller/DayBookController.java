package com.managio.modules.daybook.controller;

import com.managio.modules.daybook.service.DayBookService;
import com.managio.modules.daybook.service.DayBookService.DayBook;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/day-book")
@RequiredArgsConstructor
class DayBookController {

    private final DayBookService dayBook;
    private final Clock clock;

    record CloseBody(LocalDate date, @NotNull(message = "Enter the cash you counted") BigDecimal countedCash,
                     @Size(max = 500) String notes) {
    }

    /** Anyone who can close the day, record expenses or see billing may see the day's figures. */
    @GetMapping
    @RequirePermission(Permission.DAYBOOK_CLOSE)
    DayBook get(BusinessContext ctx, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return dayBook.get(ctx, date != null ? date : ctx.today(clock));
    }

    @PostMapping("/close")
    @RequirePermission(Permission.DAYBOOK_CLOSE)
    DayBook close(BusinessContext ctx, @Valid @RequestBody CloseBody body) {
        return dayBook.close(ctx, body.date() != null ? body.date() : ctx.today(clock), body.countedCash(), body.notes());
    }

    @GetMapping("/history")
    @RequirePermission(Permission.DAYBOOK_CLOSE)
    List<DayBook> history(BusinessContext ctx, @RequestParam(defaultValue = "30") int days) {
        return dayBook.history(ctx, days);
    }
}
