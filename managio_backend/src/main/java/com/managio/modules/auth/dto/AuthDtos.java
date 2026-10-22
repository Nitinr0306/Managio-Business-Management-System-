package com.managio.modules.auth.dto;

import com.managio.modules.auth.domain.User;
import com.managio.platform.web.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request and response bodies for /api/v1/auth and /api/v1/me. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record SignUpRequest(
            @NotBlank(message = "Enter your name") @Size(max = 150, message = "Use 150 characters or fewer") String fullName,
            @NotBlank(message = "Enter your email") @Email(message = "Enter a valid email") @Size(max = 255) String email,
            @NotBlank(message = "Choose a password") @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String password) {
    }

    public record SignInRequest(
            @NotBlank(message = "Enter your email") @Email(message = "Enter a valid email") String email,
            @NotBlank(message = "Enter your password") String password) {
    }

    public record GoogleSignInRequest(@NotBlank String credential) {
    }

    public record VerifyEmailRequest(@NotBlank String token) {
    }

    public record PasswordResetRequest(
            @NotBlank(message = "Enter your email") @Email(message = "Enter a valid email") String email) {
    }

    public record PasswordResetConfirmRequest(
            @NotBlank String token,
            @NotBlank(message = "Choose a password") @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String password) {
    }

    /** {@code currentPassword} is required unless the account has no password yet (Google-only). */
    public record ChangePasswordRequest(
            String currentPassword,
            @NotBlank(message = "Choose a password") @Size(min = 8, max = 72, message = "Use 8 to 72 characters") String newPassword) {
    }

    public record UpdateProfileRequest(
            @NotBlank(message = "Enter your name") @Size(max = 150, message = "Use 150 characters or fewer") String fullName,
            @Size(max = 20, message = "Use 20 characters or fewer") String phone) {
    }

    public record UserResponse(long id, String email, String fullName, @Nullable String phone, boolean emailVerified,
                               boolean hasPassword, boolean googleLinked) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(),
                    user.isEmailVerified(), user.hasPassword(), user.getGoogleSubject() != null);
        }
    }

    /** Returned by every endpoint that signs someone in; the refresh token travels in the cookie. */
    public record SessionResponse(String accessToken, long expiresIn, UserResponse user) {
    }
}
