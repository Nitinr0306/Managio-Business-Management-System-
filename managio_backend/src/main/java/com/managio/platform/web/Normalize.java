package com.managio.platform.web;

import java.util.Locale;

/** Canonical forms for values people type in different ways. */
public final class Normalize {

    private Normalize() {
    }

    /** Emails are compared and stored trimmed and lowercase. */
    public static String email(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Phones keep a leading + and digits only, so "98450 12873" and "98450-12873" match. */
    public static String phone(String phone) {
        if (phone == null) {
            return null;
        }
        String trimmed = phone.trim();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (Character.isDigit(c) || (c == '+' && out.isEmpty())) {
                out.append(c);
            }
        }
        return out.isEmpty() ? null : out.toString();
    }

    /** Trims text and turns blank values into null. */
    public static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
