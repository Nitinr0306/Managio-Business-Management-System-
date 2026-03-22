package com.nitin.saas.common.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

        private final OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();

        @Value("${RESEND_API_KEY}")
        private String resendApiKey;

        @Value("${app.frontend.url:http://localhost:3000}")
        private String frontendUrl;

        @Value("${app.mail.from-email:no-reply@managio.com}")
        private String fromEmail;

        @Value("${app.mail.from-name:Managio}")
        private String fromName;

        // ================================================================
        // CORE EMAIL SENDER (PRODUCTION SAFE)
        // ================================================================

        @Async
        public void sendEmail(String to, String subject, String htmlBody, String plainBody) {

                String json = buildJson(to, subject, htmlBody, plainBody);

                Request request = new Request.Builder()
                        .url("https://api.resend.com/emails")
                        .post(RequestBody.create(json, MediaType.get("application/json")))
                        .addHeader("Authorization", "Bearer " + resendApiKey)
                        .build();

                try (Response response = client.newCall(request).execute()) {

                        if (!response.isSuccessful()) {
                                String error = response.body() != null ? response.body().string() : "unknown";
                                log.error("❌ Email failed | to={} | subject={} | error={}", to, subject, error);
                                throw new RuntimeException("Email sending failed");
                        }

                        log.info("✅ Email sent | to={} | subject={}", to, subject);

                } catch (IOException e) {
                        log.error("❌ Email sending exception | to={} | subject={}", to, subject, e);
                        throw new RuntimeException("Email sending failed", e);
                }
        }

        private String buildJson(String to, String subject, String html, String text) {
                return "{"
                        + "\"from\":\"" + fromName + " <" + fromEmail + ">\","
                        + "\"to\":[\"" + escapeJson(to) + "\"],"
                        + "\"subject\":\"" + escapeJson(subject) + "\","
                        + "\"html\":\"" + escapeJson(html) + "\","
                        + "\"text\":\"" + escapeJson(text) + "\""
                        + "}";
        }

        private String escapeJson(String input) {
                if (input == null) return "";
                return input.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "");
        }

        // ================================================================
        // AUTH EMAILS
        // ================================================================


        public void sendVerificationEmail(String email, String token) {

                String link = frontendUrl + "/verify-email?token=" + token;

                sendEmail(
                        email,
                        "Verify Your Email – Managio",
                        html(
                                "Verify Your Email",
                                "Please verify your email to activate your account.",
                                "Verify Email",
                                link,
                                "Expires in 24 hours."
                        ),
                        "Verify your email: " + link
                );
        }

        @Async
        public void sendPasswordResetEmail(String email, String token) {

                String link = frontendUrl + "/reset-password?token=" + token;

                sendEmail(
                        email,
                        "Password Reset – Managio",
                        html(
                                "Reset Password",
                                "You requested a password reset.",
                                "Reset Password",
                                link,
                                "Expires in 1 hour."
                        ),
                        "Reset password: " + link
                );
        }

        // ================================================================
        // STAFF EMAILS
        // ================================================================

        @Async
        public void sendStaffInvitationEmail(
                String email,
                Long businessId,
                String businessName,
                String role,
                String department,
                String designation,
                String token) {

                String link = frontendUrl + "/staff/accept-invitation?token=" + token;

                String body = "You are invited to join <strong>" + escape(businessName) + "</strong>"
                        + "<br><br><strong>Business ID:</strong> " + businessId
                        + "<br><strong>Role:</strong> " + escape(role);

                sendEmail(
                        email,
                        "Staff Invitation – " + businessName,
                        html("Staff Invitation", body, "Accept Invitation", link, "Expires in 72 hours"),
                        "Join " + businessName + ": " + link
                );
        }

        @Async
        public void sendStaffWelcomeEmail(
                String email,
                Long businessId,
                String name,
                String businessName,
                String role) {

                String link = frontendUrl + "/staff/login";

                sendEmail(
                        email,
                        "Welcome – " + businessName,
                        html(
                                "Welcome",
                                "Hi " + escape(name) + ", your role is " + escape(role)
                                        + "<br><br>Business ID: " + businessId,
                                "Login",
                                link,
                                ""
                        ),
                        "Login: " + link
                );
        }

        // ================================================================
        // MEMBER EMAILS
        // ================================================================

        @Async
        public void sendMemberWelcomeEmail(
                String email,
                Long businessId,
                String memberName,
                String businessName) {

                String link = frontendUrl + "/member/login";

                sendEmail(
                        email,
                        "Welcome – " + businessName,
                        html(
                                "Welcome",
                                "Hi " + escape(memberName)
                                        + "<br>Business ID: " + businessId,
                                "Login",
                                link,
                                ""
                        ),
                        "Login: " + link
                );
        }

        @Async
        public void sendSubscriptionExpiryReminder(
                String email,
                Long businessId,
                String memberName,
                int daysRemaining) {

                String link = frontendUrl + "/member/subscription";

                sendEmail(
                        email,
                        "Subscription Reminder",
                        html(
                                "Reminder",
                                "Expires in " + daysRemaining + " days"
                                        + "<br>Business ID: " + businessId,
                                "Renew",
                                link,
                                ""
                        ),
                        "Expires in " + daysRemaining + " days"
                );
        }

        @Async
        public void sendPaymentConfirmation(
                String email,
                Long businessId,
                String memberName,
                String amount,
                String method) {

                sendEmail(
                        email,
                        "Payment Received",
                        html(
                                "Payment Received",
                                "₹" + amount + " via " + method
                                        + "<br>Business ID: " + businessId,
                                "View",
                                frontendUrl + "/member/payments",
                                ""
                        ),
                        "Payment ₹" + amount
                );
        }

        // ================================================================
        // TEMPLATE
        // ================================================================

        private String html(String heading, String body, String cta, String link, String footer) {

                return "<html><body style='font-family:Arial;background:#f3f4f6;padding:32px;'>"
                        + "<div style='max-width:520px;margin:auto;background:#fff;padding:24px;border-radius:8px;'>"
                        + "<h2>" + heading + "</h2>"
                        + "<p>" + body + "</p>"
                        + "<a href='" + link + "' style='background:#4f46e5;color:#fff;padding:10px 20px;text-decoration:none;border-radius:6px;'>"
                        + cta + "</a>"
                        + "<p style='font-size:12px;color:#888;margin-top:20px;'>"
                        + footer + "</p>"
                        + "</div></body></html>";
        }

        private String escape(String input) {
                if (input == null) return "";
                return input.replace("<", "&lt;").replace(">", "&gt;");
        }
}