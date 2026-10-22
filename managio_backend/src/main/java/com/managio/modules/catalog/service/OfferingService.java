package com.managio.modules.catalog.service;

import com.managio.modules.catalog.domain.Offering;
import com.managio.modules.catalog.repository.OfferingRepository;
import com.managio.modules.catalog.domain.Offering.Details;
import com.managio.modules.catalog.domain.Offering.Kind;
import com.managio.platform.util.Money;
import com.managio.modules.activity.service.ActivityLog;
import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.error.ApiException;
import com.managio.platform.web.Normalize;
import com.managio.platform.web.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/** Plans, services and products a business sells (MEM-7, BIL-8). */
@Service
@RequiredArgsConstructor
public class OfferingService {

    private final OfferingRepository offerings;
    private final ActivityLog activity;
    private final JdbcClient jdbc;

    public record OfferingResponse(long id, Kind kind, String name, @Nullable String description, BigDecimal price,
                                   BigDecimal taxRate, @Nullable Integer durationValue,
                                   @Nullable Offering.DurationUnit durationUnit, int installments, @Nullable String sku,
                                   boolean trackStock, @Nullable Integer stockQuantity, boolean active) {
        public static OfferingResponse from(Offering o) {
            return new OfferingResponse(o.getId(), o.getKind(), o.getName(), o.getDescription(), o.getPrice(),
                    o.getTaxRate(), o.getDurationValue(), o.getDurationUnit(), o.getInstallments(), o.getSku(),
                    o.isTrackStock(), o.getStockQuantity(), o.isActive());
        }
    }

    /** Everyone in the business can see active offerings, since everyone may sell; inactive ones need offerings.manage. */
    @Transactional(readOnly = true)
    public List<OfferingResponse> list(BusinessContext ctx, Kind kind, boolean includeInactive) {
        boolean showInactive = includeInactive && ctx.can(Permission.OFFERINGS_MANAGE);
        return offerings.list(ctx.businessId(), kind, showInactive).stream().map(OfferingResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public OfferingResponse describe(BusinessContext ctx, long offeringId) {
        return OfferingResponse.from(get(ctx.businessId(), offeringId));
    }

    @Transactional
    public OfferingResponse create(BusinessContext ctx, Kind kind, Details details, Integer initialStock) {
        Details clean = validate(ctx, kind, details);
        if (initialStock != null && initialStock < 0) {
            throw ApiException.invalidField("initialStock", "Stock can't be negative");
        }
        Offering offering = offerings.save(Offering.create(ctx.businessId(), kind, clean, initialStock));
        activity.record(ctx, "OFFERING_CREATED", "OFFERING", offering.getId(), "Added " + label(kind) + " " + offering.getName());
        return OfferingResponse.from(offering);
    }

    /** MEM-7: price changes apply to future sales only; existing memberships keep their price. */
    @Transactional
    public OfferingResponse update(BusinessContext ctx, long offeringId, Details details) {
        Offering offering = get(ctx.businessId(), offeringId);
        offering.apply(validate(ctx, offering.getKind(), details));
        activity.record(ctx, "OFFERING_UPDATED", "OFFERING", offeringId, "Updated " + offering.getName());
        return OfferingResponse.from(offering);
    }

    /** MEM-7: anything already sold can only be deactivated. */
    @Transactional
    public void delete(BusinessContext ctx, long offeringId) {
        Offering offering = get(ctx.businessId(), offeringId);
        boolean used = jdbc.sql("""
                        SELECT EXISTS (SELECT 1 FROM memberships WHERE offering_id = :id)
                            OR EXISTS (SELECT 1 FROM invoice_lines WHERE offering_id = :id)""")
                .param("id", offeringId).query(Boolean.class).single();
        if (used) {
            throw ApiException.rule("offering.in_use", offering.getName() + " has been sold before. Deactivate it instead.");
        }
        offerings.delete(offering);
        activity.record(ctx, "OFFERING_DELETED", "OFFERING", offeringId, "Deleted " + offering.getName());
    }

    @Transactional
    public OfferingResponse adjustStock(BusinessContext ctx, long offeringId, int change, String reason) {
        Offering offering = offerings.findForUpdate(offeringId, ctx.businessId()).orElseThrow(() -> ApiException.notFound("Product"));
        if (!offering.isTrackStock()) {
            throw ApiException.rule("offering.no_stock", offering.getName() + " doesn't track stock.");
        }
        if (!offering.changeStock(change)) {
            throw ApiException.rule("stock.insufficient", "Only " + offering.getStockQuantity() + " in stock.");
        }
        activity.record(ctx, "STOCK_ADJUSTED", "OFFERING", offeringId,
                (change > 0 ? "Added " : "Removed ") + Math.abs(change) + " of " + offering.getName()
                        + (reason == null ? "" : " (" + reason + ")"));
        return OfferingResponse.from(offering);
    }

    /** BIL-8: takes stock for a sale; locks the product row so two sales can't oversell. */
    @Transactional
    public Offering takeStock(long businessId, long offeringId, int quantity) {
        Offering offering = offerings.findForUpdate(offeringId, businessId).orElseThrow(() -> ApiException.notFound("Item"));
        if (!offering.changeStock(-quantity)) {
            throw ApiException.rule("stock.insufficient",
                    "Only " + offering.getStockQuantity() + " of " + offering.getName() + " in stock.");
        }
        return offering;
    }

    /** BIL-8: puts stock back when a sale is voided. */
    @Transactional
    public void returnStock(long businessId, long offeringId, int quantity) {
        offerings.findForUpdate(offeringId, businessId).ifPresent(o -> o.changeStock(quantity));
    }

    @Transactional(readOnly = true)
    public Offering get(long businessId, long offeringId) {
        return offerings.findByIdAndBusinessId(offeringId, businessId).orElseThrow(() -> ApiException.notFound("Item"));
    }

    private static Details validate(BusinessContext ctx, Kind kind, Details d) {
        String name = Normalize.text(d.name());
        if (name == null) {
            throw ApiException.invalidField("name", "Enter a name");
        }
        if (d.price() == null || d.price().signum() < 0) {
            throw ApiException.invalidField("price", "Enter a price of 0 or more");
        }
        BigDecimal taxRate = d.taxRate() == null ? Money.ZERO : d.taxRate();
        if (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw ApiException.invalidField("taxRate", "Enter a tax rate between 0 and 100");
        }
        switch (kind) {
            case PLAN -> {
                requireModule(ctx, Module.MEMBERSHIPS, "Plans need the Memberships feature");
                if (d.durationValue() == null || d.durationValue() < 1 || d.durationUnit() == null) {
                    throw ApiException.invalidField("durationValue", "Enter how long the plan lasts");
                }
                int installments = d.installments() == null ? 1 : d.installments();
                if (installments < 1 || installments > 24) {
                    throw ApiException.invalidField("installments", "Use 1 to 24 installments");
                }
            }
            case SERVICE -> requireModule(ctx, Module.CATALOG, "Services need the Products and services feature");
            case PRODUCT -> {
                requireModule(ctx, Module.CATALOG, "Products need the Products and services feature");
                if (Boolean.TRUE.equals(d.trackStock()) && !ctx.has(Module.INVENTORY)) {
                    throw ApiException.invalidField("trackStock", "Turn on stock tracking in Settings first");
                }
            }
        }
        return new Details(name, Normalize.text(d.description()), Money.of(d.price()), taxRate, d.durationValue(),
                d.durationUnit(), d.installments(), Normalize.text(d.sku()), d.trackStock(), d.active());
    }

    private static void requireModule(BusinessContext ctx, Module module, String message) {
        if (!ctx.has(module)) {
            throw ApiException.rule("module.disabled", message + ". An owner can turn it on in Settings.");
        }
    }

    private static String label(Kind kind) {
        return switch (kind) {
            case PLAN -> "plan";
            case SERVICE -> "service";
            case PRODUCT -> "product";
        };
    }
}
