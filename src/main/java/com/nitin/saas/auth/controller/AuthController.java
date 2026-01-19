package com.nitin.saas.auth.controller;

import com.nitin.saas.auth.dto.AuthTokenResponse;
import com.nitin.saas.auth.dto.UserResponse;
import com.nitin.saas.auth.entity.AuthAuditLog;
import com.nitin.saas.auth.entity.RefreshToken;
import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.repository.AuthAuditLogRepository;
import com.nitin.saas.auth.repository.RefreshTokenRepository;
import com.nitin.saas.auth.repository.UserRepository;
import com.nitin.saas.auth.service.AuthService;
import com.nitin.saas.common.exception.BadRequestException;
import com.nitin.saas.common.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final AuthAuditLogRepository auditLogRepository;

    public AuthController(AuthService authService, RefreshTokenRepository refreshTokenRepository, UserRepository userRepository, AuthAuditLogRepository auditLogRepository) {
        this.authService = authService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;

    }

    @PostMapping("/register")
    public UserResponse register(@RequestParam String email,@RequestParam String password){
        User user=authService.register(email,password);
        return new UserResponse(user.getId(), user.getEmail());
    }

    @PostMapping("/login")
    public AuthTokenResponse login(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request
    ) {
        User user = authService.login(email, password);

        String accessToken = JwtUtil.generateToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        String refreshToken = authService.createRefreshToken(user);

        auditLogRepository.save(
                new AuthAuditLog(user.getId(), "LOGIN_SUCCESS", request.getRemoteAddr())
        );

        return new AuthTokenResponse(accessToken, refreshToken);
    }


    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token){
        authService.verifyEmail(token);
        return "Email verified Successfully";
    }

    @GetMapping("/resend-email-verificationCode")
    public String resendEmailVerificationCode(@RequestParam String email){
        authService.resendEmailVerificationCode(email);
        return "Email verification code sent successfully";
    }

    @GetMapping("/forget-password")
    public String forgetPassword(@RequestParam String email){
        authService.forgetPassword(email);
        return "Password reset link sent successfully";
    }

    @GetMapping("/reset-password")
    public String resetPassword(@RequestParam String token,@RequestParam String newPassword){
        authService.resetPassword(token,newPassword);
        return "Password reset successfully";
    }

    @PostMapping("/refresh")
    public AuthTokenResponse refresh(@RequestBody String refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        if (token.getExpiryTime().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new BadRequestException("Refresh token expired");
        }

        User user = token.getUser();

        String newAccessToken = JwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthTokenResponse(newAccessToken, refreshToken);
    }


    @PostMapping("/logout")
    public String logout(@RequestBody String refreshToken) {

        RefreshToken token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new BadRequestException("Invalid refresh token"));

        refreshTokenRepository.delete(token);

        return "Logged out successfully";
    }

}
