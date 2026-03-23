package com.nitin.saas.common.utils;

import java.util.Locale;
import java.security.SecureRandom;

public final class PublicIdGenerator {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PublicIdGenerator() {
    }

    public static String generate(String prefix, int randomPartLength) {
        String safePrefix = prefix == null ? "" : prefix.trim().toUpperCase(Locale.ROOT);
        int length = Math.max(4, randomPartLength);
        return safePrefix + randomChars(ALPHANUMERIC, length);
    }

    public static String generateBusinessPublicId() {
        String digits = String.format(Locale.ROOT, "%04d", RANDOM.nextInt(10_000));
        String letters = randomChars(ALPHABET, 4);
        return digits + letters;
    }

    private static String randomChars(String source, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = RANDOM.nextInt(source.length());
            sb.append(source.charAt(idx));
        }
        return sb.toString();
    }
}
