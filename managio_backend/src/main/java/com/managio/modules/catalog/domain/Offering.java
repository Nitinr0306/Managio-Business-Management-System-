package com.managio.modules.catalog.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Something a business sells: a plan with a duration, a service or a product. */
@Entity
@Table(name = "offerings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offering {

    public enum Kind { PLAN, SERVICE, PRODUCT }

    public enum DurationUnit { DAYS, MONTHS }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long businessId;

    @Enumerated(EnumType.STRING)
    private Kind kind;

    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal taxRate;
    private Integer durationValue;

    @Enumerated(EnumType.STRING)
    private DurationUnit durationUnit;

    private int installments = 1;
    private String sku;
    private boolean trackStock;
    private Integer stockQuantity;
    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    /** The editable fields; plan-only and product-only fields are null for other kinds. */
    public record Details(String name, String description, BigDecimal price, BigDecimal taxRate, Integer durationValue,
                          DurationUnit durationUnit, Integer installments, String sku, Boolean trackStock, Boolean active) {
    }

    public static Offering create(long businessId, Kind kind, Details details, Integer initialStock) {
        Offering offering = new Offering();
        offering.businessId = businessId;
        offering.kind = kind;
        offering.apply(details);
        if (offering.trackStock) {
            offering.stockQuantity = initialStock == null ? 0 : initialStock;
        }
        return offering;
    }

    public void apply(Details d) {
        this.name = d.name();
        this.description = d.description();
        this.price = d.price();
        this.taxRate = d.taxRate();
        this.durationValue = kind == Kind.PLAN ? d.durationValue() : null;
        this.durationUnit = kind == Kind.PLAN ? d.durationUnit() : null;
        this.installments = kind == Kind.PLAN && d.installments() != null ? d.installments() : 1;
        this.sku = kind == Kind.PRODUCT ? d.sku() : null;
        boolean track = kind == Kind.PRODUCT && Boolean.TRUE.equals(d.trackStock());
        if (track && !trackStock) {
            stockQuantity = stockQuantity == null ? 0 : stockQuantity;
        }
        this.trackStock = track;
        if (!track) {
            stockQuantity = null;
        }
        if (d.active() != null) {
            this.active = d.active();
        }
    }

    /** MEM-1: inclusive end date — a 30-day plan from 20 Oct ends 18 Nov; a 1-month plan ends 19 Nov. */
    public LocalDate endDateFrom(LocalDate start) {
        if (kind != Kind.PLAN) {
            throw new IllegalStateException("Only plans have a duration");
        }
        return durationUnit == DurationUnit.MONTHS
                ? start.plusMonths(durationValue).minusDays(1)
                : start.plusDays(durationValue - 1L);
    }

    /** Changes stock by {@code delta}; never below zero. Returns false if there isn't enough stock. */
    public boolean changeStock(int delta) {
        if (!trackStock) {
            return true;
        }
        int next = stockQuantity + delta;
        if (next < 0) {
            return false;
        }
        stockQuantity = next;
        return true;
    }
}
