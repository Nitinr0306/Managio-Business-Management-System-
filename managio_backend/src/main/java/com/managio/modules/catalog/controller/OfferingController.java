package com.managio.modules.catalog.controller;

import com.managio.modules.catalog.service.OfferingService;
import com.managio.modules.catalog.domain.Offering.Details;
import com.managio.modules.catalog.domain.Offering.DurationUnit;
import com.managio.modules.catalog.domain.Offering.Kind;
import com.managio.modules.catalog.service.OfferingService.OfferingResponse;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.tenancy.RequireModule;
import com.managio.platform.tenancy.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/offerings")
@RequiredArgsConstructor
class OfferingController {

    private final OfferingService offerings;

    record OfferingRequest(@NotNull(message = "Choose plan, service or product") Kind kind,
                           @NotBlank(message = "Enter a name") @Size(max = 120, message = "Use 120 characters or fewer") String name,
                           @Size(max = 500) String description,
                           @NotNull(message = "Enter a price") BigDecimal price,
                           BigDecimal taxRate,
                           Integer durationValue,
                           DurationUnit durationUnit,
                           Integer installments,
                           @Size(max = 40) String sku,
                           Boolean trackStock,
                           Integer initialStock,
                           Boolean active) {
        Details details() {
            return new Details(name, description, price, taxRate, durationValue, durationUnit, installments, sku,
                    trackStock, active);
        }
    }

    record StockRequest(@NotNull(message = "Enter how many to add or remove") Integer change, @Size(max = 200) String reason) {
    }

    @GetMapping
    List<OfferingResponse> list(BusinessContext ctx, @RequestParam(required = false) Kind kind,
                                @RequestParam(defaultValue = "false") boolean includeInactive) {
        return offerings.list(ctx, kind, includeInactive);
    }

    @GetMapping("/{offeringId}")
    OfferingResponse get(BusinessContext ctx, @PathVariable long offeringId) {
        return offerings.describe(ctx, offeringId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(Permission.OFFERINGS_MANAGE)
    OfferingResponse create(BusinessContext ctx, @Valid @RequestBody OfferingRequest request) {
        return offerings.create(ctx, request.kind(), request.details(), request.initialStock());
    }

    /** The kind of an offering can't change; it is ignored here. */
    @PatchMapping("/{offeringId}")
    @RequirePermission(Permission.OFFERINGS_MANAGE)
    OfferingResponse update(BusinessContext ctx, @PathVariable long offeringId, @Valid @RequestBody OfferingRequest request) {
        return offerings.update(ctx, offeringId, request.details());
    }

    @DeleteMapping("/{offeringId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(Permission.OFFERINGS_MANAGE)
    void delete(BusinessContext ctx, @PathVariable long offeringId) {
        offerings.delete(ctx, offeringId);
    }

    @PostMapping("/{offeringId}/stock")
    @RequireModule(Module.INVENTORY)
    @RequirePermission(Permission.OFFERINGS_MANAGE)
    OfferingResponse adjustStock(BusinessContext ctx, @PathVariable long offeringId, @Valid @RequestBody StockRequest request) {
        return offerings.adjustStock(ctx, offeringId, request.change(), request.reason());
    }
}
