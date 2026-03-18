package com.nitin.saas.common.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import okhttp3.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {


    private final OkHttpClient client = new OkHttpClient();


    @Value("${RESEND_API_KEY}")
    private String resendApiKey;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;



    @Value("${app.mail.from-name:Managio}")
    private String fromName;
    // ================================================================
    // CORE EMAIL SENDER
    // ================================================================

    @Async
    public void sendEmail(String to, String subject, String htmlBody, String plainBody) {


        String json = "{"
                + "\"from\":\"" + fromName + " <onboarding@resend.dev>\","
                + "\"to\":[\"" + to + "\"],"
                + "\"subject\":\"" + subject + "\","
                + "\"html\":\"" + htmlBody.replace("\"", "\\\"") + "\""
                + "}";

        Request request = new Request.Builder()
                .url("https://api.resend.com/emails")
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + resendApiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                log.error("Email failed: {}", response.body().string());
                throw new RuntimeException("Email failed");
            }

            log.info("✅ Email sent via Resend | to={} | subject={}", to, subject);

        } catch (Exception e) {
            log.error("Email sending failed via Resend", e);
            throw new RuntimeException("Email failed");
        }
    }

    // ================================================================
    // AUTH EMAILS
    // ================================================================

    @Async
    public void sendVerificationEmail(String email, String token) {

        String link = frontendUrl + "/verify-email?token=" + token;

        String html = html(
                "Verify Your Email",
                "Welcome to Managio! Please verify your email address to activate your account.",
                "Verify Email",
                link,
                "This link expires in 24 hours. If you did not create an account, you can safely ignore this email."
        );

        String plain = "Welcome to Managio!\n\n"
                + "Please verify your email by visiting:\n" + link
                + "\n\nThis link expires in 24 hours.";

        sendEmail(email, "Verify Your Email – Managio", html, plain);
    }

    @Async
    public void sendPasswordResetEmail(String email, String token) {

        String link = frontendUrl + "/reset-password?token=" + token;

        String html = html(
                "Reset Your Password",
                "We received a request to reset your Managio password.",
                "Reset Password",
                link,
                "This link expires in 1 hour. If you did not request a password reset, please ignore this email — your password has not been changed."
        );

        String plain = "You requested a password reset for your Managio account.\n\n"
                + "Reset your password here:\n" + link
                + "\n\nThis link expires in 1 hour.";

        sendEmail(email, "Password Reset – Managio", html, plain);
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

        String roleInfo = "<tr><td style='padding:4px 0;color:#6b7280;'>Role</td>"
                + "<td style='padding:4px 0;font-weight:600;'>" + escape(role) + "</td></tr>";

        if (department != null && !department.isBlank()) {
            roleInfo += "<tr><td style='padding:4px 0;color:#6b7280;'>Department</td>"
                    + "<td style='padding:4px 0;'>" + escape(department) + "</td></tr>";
        }

        if (designation != null && !designation.isBlank()) {
            roleInfo += "<tr><td style='padding:4px 0;color:#6b7280;'>Designation</td>"
                    + "<td style='padding:4px 0;'>" + escape(designation) + "</td></tr>";
        }

        String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;"
                + "background:#f3f4f6;margin:0;padding:32px;'>"
                + "<div style='max-width:520px;margin:0 auto;background:#fff;"
                + "border-radius:8px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.12);'>"
                + "<div style='background:#4f46e5;padding:32px 32px 24px;text-align:center;'>"
                + "<h1 style='color:#fff;margin:0;font-size:22px;'>Staff Invitation</h1></div>"
                + "<div style='padding:32px;'>"
                + "<p style='color:#374151;margin:0 0 16px;'>You have been invited to join "
                + "<strong>" + escape(businessName) + "</strong> on Managio.</p>"

                + "<p style='margin:0 0 16px;color:#374151;'>"
                + "<strong>Business ID:</strong> " + businessId + "</p>"

                + "<table style='width:100%;border-collapse:collapse;margin:0 0 24px;'>"
                + roleInfo + "</table>"

                + "<a href='" + link + "' style='display:inline-block;background:#4f46e5;"
                + "color:#fff;text-decoration:none;padding:12px 28px;border-radius:6px;"
                + "font-weight:600;'>Accept Invitation</a>"

                + "<p style='color:#9ca3af;font-size:12px;margin:24px 0 0;'>"
                + "This invitation expires in 72 hours. If you did not expect this, ignore this email.</p>"
                + "</div></div></body></html>";

        String plain = "You have been invited to join " + businessName + " on Managio.\n\n"
                + "Business ID: " + businessId + "\n"
                + "Role: " + role + "\n"
                + "\nAccept invitation:\n" + link;

        sendEmail(email, "You're invited to join " + businessName + " – Managio", html, plain);
    }

    // ------------------------------------------------

    @Async
    public void sendStaffWelcomeEmail(
            String email,
            Long businessId,
            String name,
            String businessName,
            String role) {

        String loginLink = frontendUrl + "/staff/login";

        String html = html(
                "Welcome to " + businessName + "!",
                "Hi " + escape(name) + ", your staff account for <strong>"
                        + escape(businessName) + "</strong> has been created."
                        + "<br><br>Your role: <strong>" + escape(role) + "</strong>"
                        + "<br><br><strong>Business ID:</strong> " + businessId,
                "Go to Staff Login",
                loginLink,
                "Use the Business ID above when logging in."
        );

        String plain = "Hi " + name + "\n\n"
                + "Business: " + businessName + "\n"
                + "Business ID: " + businessId + "\n"
                + "Role: " + role + "\n"
                + "Login: " + loginLink;

        sendEmail(email, "Welcome to " + businessName + " – Managio", html, plain);
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

        String html = html(
                "Welcome to " + businessName + "!",
                "Hi " + escape(memberName) + ", your membership at <strong>"
                        + escape(businessName) + "</strong> is now active."
                        + "<br><br><strong>Business ID:</strong> " + businessId,
                "Member Portal",
                link,
                "Use the Business ID above when logging in."
        );

        String plain = "Hi " + memberName + "\n\n"
                + "Business: " + businessName + "\n"
                + "Business ID: " + businessId + "\n"
                + "Login: " + link;

        sendEmail(email, "Welcome to " + businessName + " – Managio", html, plain);
    }

    // ------------------------------------------------

    @Async
    public void sendSubscriptionExpiryReminder(
            String email,
            Long businessId,
            String memberName,
            int daysRemaining) {

        String link = frontendUrl + "/member/subscription";

        String html = html(
                "Subscription Reminder",
                "Hi " + escape(memberName)
                        + ", your subscription expires in <strong>"
                        + daysRemaining + " days</strong>."
                        + "<br><br><strong>Business ID:</strong> " + businessId,
                "Renew Now",
                link,
                "If already renewed, ignore this email."
        );

        String plain = "Subscription expires in " + daysRemaining + " days.\n"
                + "Business ID: " + businessId;

        sendEmail(email, "Subscription Reminder – Managio", html, plain);
    }

    // ------------------------------------------------

    @Async
    public void sendPaymentConfirmation(
            String email,
            Long businessId,
            String memberName,
            String amount,
            String method) {

        String html = html(
                "Payment Received",
                "Hi " + escape(memberName)
                        + ", we recorded a payment of ₹"
                        + escape(amount) + " via "
                        + escape(method)
                        + "<br><br><strong>Business ID:</strong> " + businessId,
                "View Payment History",
                frontendUrl + "/member/payments",
                "Contact support if you did not make this payment."
        );

        String plain = "Payment ₹" + amount + " received via " + method
                + "\nBusiness ID: " + businessId;

        sendEmail(email, "Payment Confirmed – ₹" + amount + " – Managio", html, plain);
    }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    private String html(String heading,
                        String bodyHtml,
                        String ctaLabel,
                        String ctaUrl,
                        String footer) {

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Arial,sans-serif;background:#f3f4f6;"
                + "margin:0;padding:32px;'>"
                + "<div style='max-width:520px;margin:0 auto;background:#ffffff;"
                + "border-radius:8px;overflow:hidden;"
                + "box-shadow:0 1px 4px rgba(0,0,0,.12);'>"

                + "<div style='background:#4f46e5;padding:32px;text-align:center;'>"
                + "<h1 style='color:#ffffff;margin:0;font-size:22px;'>"
                + heading + "</h1></div>"

                + "<div style='padding:32px;'>"
                + "<p style='color:#374151;line-height:1.6;margin:0 0 24px;'>"
                + bodyHtml + "</p>"

                + "<a href='" + ctaUrl + "' "
                + "style='display:inline-block;background:#4f46e5;color:#ffffff;"
                + "text-decoration:none;padding:12px 28px;border-radius:6px;"
                + "font-weight:600;font-size:15px;'>"
                + escape(ctaLabel) + "</a>"

                + "<p style='color:#9ca3af;font-size:12px;margin:24px 0 0;'>"
                + escape(footer) + "</p>"

                + "</div>"

                + "<div style='background:#f9fafb;padding:16px 32px;text-align:center;"
                + "border-top:1px solid #e5e7eb;'>"
                + "<p style='color:#9ca3af;font-size:11px;margin:0;'>"
                + "© Managio – Gym &amp; Studio Management Platform</p>"
                + "</div>"

                + "</div></body></html>";
    }

    private String escape(String input) {

        if (input == null) return "";

        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}