package com.managio.platform.security;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

/** Parsing and grouping of client IP addresses for rate limits and sign-in records. */
public final class ClientAddresses {

    private static final Pattern IPV4 = Pattern.compile(
            "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}");
    /** Hex digits, colons and an optional embedded IPv4 part; no zone IDs, brackets or host names. */
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9A-Fa-f:.]{2,45}");

    private ClientAddresses() {
    }

    /**
     * The address as a normalised IP literal, or empty when {@code value} isn't one. Only literals are accepted, so
     * this never triggers a DNS lookup.
     */
    public static Optional<String> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String candidate = value.trim();
        if (IPV4.matcher(candidate).matches()) {
            return Optional.of(candidate);
        }
        if (!candidate.contains(":") || !IPV6_CHARACTERS.matcher(candidate).matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(InetAddress.getByName(candidate).getHostAddress());
        } catch (UnknownHostException e) {
            return Optional.empty();
        }
    }

    /**
     * The key a rate limit counts against. IPv6 addresses are grouped by their /64 network, because one
     * subscriber usually controls a whole /64 and could otherwise pick a fresh address for every request.
     */
    public static String rateLimitKey(String address) {
        Optional<String> parsed = parse(address);
        if (parsed.isEmpty()) {
            return String.valueOf(address);
        }
        try {
            InetAddress ip = InetAddress.getByName(parsed.get());
            if (ip instanceof Inet6Address) {
                byte[] network = Arrays.copyOf(ip.getAddress(), 16);
                Arrays.fill(network, 8, 16, (byte) 0);
                return InetAddress.getByAddress(network).getHostAddress() + "/64";
            }
            return ip.getHostAddress();
        } catch (UnknownHostException e) {
            return parsed.get();
        }
    }
}
