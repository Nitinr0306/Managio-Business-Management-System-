package com.managio.modules.business.dto;

import com.managio.modules.business.domain.Business;
import com.managio.modules.business.domain.BusinessType;
import com.managio.modules.business.domain.BusinessType.Labels;
import com.managio.platform.tenancy.Module;
import com.managio.platform.tenancy.Permission;
import com.managio.platform.web.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/** Request and response bodies for businesses, roles and the permission catalogue. */
public final class BusinessDtos {

    private BusinessDtos() {
    }

    public record CreateBusinessRequest(
            @NotBlank(message = "Enter the business name") @Size(max = 150, message = "Use 150 characters or fewer") String name,
            @NotNull(message = "Choose the type of business") BusinessType type,
            @Size(max = 20) String phone,
            @Size(max = 100) String city) {
    }

    public record UpdateBusinessRequest(
            @NotBlank(message = "Enter the business name") @Size(max = 150, message = "Use 150 characters or fewer") String name,
            @NotNull(message = "Choose the type of business") BusinessType type,
            @Size(max = 20) String phone,
            @Email(message = "Enter a valid email") @Size(max = 255) String email,
            @Size(max = 300) String addressLine,
            @Size(max = 100) String city,
            @Size(max = 100) String state,
            @Size(max = 12) String postalCode,
            @Size(max = 60) String country,
            @Pattern(regexp = "^$|^[0-9]{2}[A-Z0-9]{13}$", message = "Enter a valid 15-character GSTIN") String gstin) {
    }

    public record UpdateSettingsRequest(
            @NotNull Set<Module> modules,
            @NotBlank String timezone,
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{1,10}$", message = "Use up to 10 capital letters or digits") String invoicePrefix,
            @NotBlank @Pattern(regexp = "^[A-Z0-9]{1,10}$", message = "Use up to 10 capital letters or digits") String receiptPrefix,
            boolean allowCustomerSignup,
            Boolean renewalReminders) {
    }

    public record BusinessResponse(long id, String code, String name, BusinessType type, String typeLabel,
                                   Labels labels, Set<Module> modules, @Nullable String phone, @Nullable String email,
                                   @Nullable String addressLine, @Nullable String city, @Nullable String state,
                                   @Nullable String postalCode, String country, @Nullable String gstin, String timezone,
                                   String currency, String invoicePrefix, String receiptPrefix,
                                   boolean allowCustomerSignup, boolean renewalReminders, Instant createdAt) {
        public static BusinessResponse from(Business b) {
            return new BusinessResponse(b.getId(), b.getCode(), b.getName(), b.getType(), b.getType().label(),
                    b.getType().labels(), b.enabledModules(), b.getPhone(), b.getEmail(), b.getAddressLine(), b.getCity(),
                    b.getState(), b.getPostalCode(), b.getCountry(), b.getGstin(), b.getTimezone(), b.getCurrency(),
                    b.getInvoicePrefix(), b.getReceiptPrefix(), b.isAllowCustomerSignup(), b.isRenewalReminders(), b.getCreatedAt());
        }
    }

    /** One business the signed-in user belongs to, with what they may do there (drives the app shell). */
    public record MyBusinessResponse(long id, String code, String name, BusinessType type, Labels labels,
                                     Set<Module> modules, long roleId, String roleName, boolean owner,
                                     List<String> permissions) {
    }

    public record RoleRequest(
            @NotBlank(message = "Enter a role name") @Size(max = 60, message = "Use 60 characters or fewer") String name,
            @Size(max = 200, message = "Use 200 characters or fewer") String description,
            @NotNull Set<Permission> permissions) {
    }

    public record PermissionResponse(String key, String name, String action) {
    }

    public record BusinessTypeResponse(BusinessType type, String label, Labels labels, Set<Module> modules) {
    }
}
