package com.managio.modules.portal.dto;

import com.managio.platform.web.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request and response bodies for /api/v1/portal. */
public final class PortalDtos {

    private PortalDtos() {
    }

    public record SignInBody(
            @NotBlank(message = "Enter the business code") String businessCode,
            @NotBlank(message = "Enter your email or phone number") String identifier,
            @NotBlank(message = "Enter your password") String password) {
    }

    public record SignUpBody(
            @NotBlank(message = "Enter the business code") String businessCode,
            @NotBlank(message = "Enter your name") @Size(max = 150, message = "Use 150 characters or fewer") String fullName,
            @Email(message = "Enter a valid email") @Size(max = 255) String email,
            @Size(max = 20, message = "Use 20 characters or fewer") String phone,
            @NotBlank(message = "Choose a password") @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String password) {
    }

    public record ResetBody(
            @NotBlank(message = "Enter the business code") String businessCode,
            @NotBlank(message = "Enter your email") String identifier) {
    }

    public record SetPasswordBody(
            @NotBlank String token,
            @NotBlank(message = "Choose a password") @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String password) {
    }

    public record ChangePasswordBody(
            @NotBlank(message = "Enter your current password") String currentPassword,
            @NotBlank(message = "Choose a password") @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String newPassword) {
    }

    /** What the sign-in page shows once the business code is known. */
    public record BusinessPreview(String code, String name, String type, boolean signUpOpen) {
    }

    public record CustomerSummary(long id, int number, String fullName, @Nullable String email, @Nullable String phone) {
    }

    public record PortalSession(String accessToken, long expiresIn, CustomerSummary customer, BusinessPreview business) {
    }

    public record Profile(CustomerSummary customer, BusinessPreview business, String membershipState,
                          @Nullable LocalDate coveredUntil, BigDecimal balanceDue, BigDecimal upcoming) {
    }

    public record PortalMembership(long id, String planName, LocalDate startsOn, LocalDate endsOn, BigDecimal price,
                                   String state) {
    }
}
