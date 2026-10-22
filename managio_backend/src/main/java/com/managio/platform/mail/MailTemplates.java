package com.managio.platform.mail;

import com.managio.platform.mail.Mailer.MailMessage;
import org.springframework.web.util.HtmlUtils;

/** Plain, readable transactional emails with one clear action each. */
public final class MailTemplates {

    private MailTemplates() {
    }

    public static MailMessage verifyEmail(String to, String name, String link) {
        return action(to, "Confirm your email for Managio",
                "Hi " + name + ",",
                "Confirm this is your email address so we can send you account notices and password resets.",
                "Confirm email", link,
                "This link works for 48 hours. If you didn't create a Managio account, ignore this email.");
    }

    public static MailMessage resetPassword(String to, String name, String link) {
        return action(to, "Reset your Managio password",
                "Hi " + name + ",",
                "Someone asked to reset the password for this account. Choose a new password with the button below.",
                "Choose a new password", link,
                "This link works for 1 hour and only once. If you didn't ask for this, ignore this email; your password stays the same.");
    }

    public static MailMessage invitation(String to, String businessName, String inviterName, String roleName, String link) {
        return action(to, inviterName + " invited you to " + businessName + " on Managio",
                "Hello,",
                inviterName + " added you to " + businessName + " as " + roleName + ". Accept the invitation to set up your access.",
                "Accept invitation", link,
                "This invitation works for 7 days.");
    }

    public static MailMessage portalAccess(String to, String name, String businessName, String link) {
        return action(to, businessName + " gave you access to your account",
                "Hi " + name + ",",
                businessName + " uses Managio. Set a password to see your plan, what you owe and your receipts.",
                "Set your password", link,
                "This link works for 7 days and only once.");
    }

    public static MailMessage portalPasswordReset(String to, String name, String businessName, String link) {
        return action(to, "Reset your " + businessName + " password",
                "Hi " + name + ",",
                "Someone asked to reset the password for your " + businessName + " account. Choose a new password with the button below.",
                "Choose a new password", link,
                "This link works for 1 hour and only once. If you didn't ask for this, ignore this email.");
    }

    public static MailMessage renewalReminder(String to, String name, String businessName, String planName, String endsOn) {
        String text = "Your " + planName + " at " + businessName + " ends on " + endsOn
                + ". Renew at the front desk or contact the business to keep it running without a break.";
        return new MailMessage(to, "Your " + planName + " ends on " + endsOn,
                html("Hi " + name + ",", text, null, null, businessName),
                "Hi " + name + ",\n\n" + text + "\n\n" + businessName);
    }

    private static MailMessage action(String to, String subject, String greeting, String body, String buttonLabel,
                                      String link, String footnote) {
        String html = html(greeting, body, buttonLabel, link, footnote);
        String text = greeting + "\n\n" + body + "\n\n" + buttonLabel + ": " + link + "\n\n" + footnote;
        return new MailMessage(to, subject, html, text);
    }

    private static String html(String greeting, String body, String buttonLabel, String link, String footnote) {
        String button = link == null ? "" : """
                <p style="margin:24px 0"><a href="%s" style="background:#2952CC;color:#ffffff;text-decoration:none;\
                padding:12px 20px;border-radius:6px;font-weight:600;display:inline-block">%s</a></p>
                <p style="color:#5B6577;font-size:13px">Or paste this link into your browser:<br>%s</p>"""
                .formatted(HtmlUtils.htmlEscape(link), HtmlUtils.htmlEscape(buttonLabel), HtmlUtils.htmlEscape(link));
        return """
                <!doctype html><html><body style="margin:0;background:#F5F6F8;font-family:Arial,Helvetica,sans-serif;color:#17202E">
                <div style="max-width:520px;margin:0 auto;padding:32px 24px">
                <p style="font-weight:700;font-size:18px;margin:0 0 24px">managio</p>
                <div style="background:#ffffff;border:1px solid #E3E6EB;border-radius:8px;padding:28px">
                <p style="margin:0 0 12px">%s</p><p style="margin:0;line-height:1.55">%s</p>%s
                </div><p style="color:#5B6577;font-size:12px;line-height:1.5;margin-top:16px">%s</p>
                </div></body></html>"""
                .formatted(HtmlUtils.htmlEscape(greeting), HtmlUtils.htmlEscape(body), button,
                        footnote == null ? "" : HtmlUtils.htmlEscape(footnote));
    }
}
