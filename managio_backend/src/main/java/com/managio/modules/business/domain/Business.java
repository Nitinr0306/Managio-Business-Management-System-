package com.managio.modules.business.domain;

import com.managio.platform.tenancy.Module;
import jakarta.persistence.Column;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;

@Entity
@Table(name = "businesses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Business {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;
    private String name;

    @Enumerated(EnumType.STRING)
    private BusinessType type;

    private String phone;
    private String email;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String gstin;
    private String timezone;
    private String currency;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] modules;

    private String invoicePrefix;
    private String receiptPrefix;
    private boolean allowCustomerSignup;
    private boolean renewalReminders = true;
    private Long createdByUserId;
    private Instant archivedAt;

    @CreationTimestamp
    private Instant createdAt;
    @UpdateTimestamp
    private Instant updatedAt;
    @Version
    private long version;

    public static Business create(String code, String name, BusinessType type, long createdByUserId) {
        Business business = new Business();
        business.code = code;
        business.name = name;
        business.type = type;
        business.country = "India";
        business.timezone = "Asia/Kolkata";
        business.currency = "INR";
        business.modules = Module.toNames(type.defaultModules());
        business.invoicePrefix = "INV";
        business.receiptPrefix = "RCT";
        business.createdByUserId = createdByUserId;
        return business;
    }

    public Set<Module> enabledModules() {
        return Module.fromNames(modules);
    }

    public ZoneId zone() {
        return ZoneId.of(timezone);
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public void updateProfile(String name, BusinessType type, String phone, String email, String addressLine, String city,
                       String state, String postalCode, String country, String gstin) {
        this.name = name;
        this.type = type;
        this.phone = phone;
        this.email = email;
        this.addressLine = addressLine;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.gstin = gstin;
    }

    public void updateSettings(Set<Module> modules, String timezone, String invoicePrefix, String receiptPrefix,
                        boolean allowCustomerSignup, boolean renewalReminders) {
        this.modules = Module.toNames(modules);
        this.timezone = timezone;
        this.invoicePrefix = invoicePrefix;
        this.receiptPrefix = receiptPrefix;
        this.allowCustomerSignup = allowCustomerSignup;
        this.renewalReminders = renewalReminders;
    }

    public void archive(Instant now) {
        this.archivedAt = now;
    }
}
