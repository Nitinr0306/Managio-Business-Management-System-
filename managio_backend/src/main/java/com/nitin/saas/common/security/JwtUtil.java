package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.member.entity.Member;
import com.nitin.saas.staff.entity.Staff;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JwtUtil {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;
    private final long      accessTokenExpiry;
    private final long      refreshTokenExpiry;
    private final String    issuer;

    public JwtUtil(
            @Value("${app.security.jwt.access-secret}")  String accessSecret,
            @Value("${app.security.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.security.jwt.access-expiry-seconds:900}")    long accessTokenExpiry,
            @Value("${app.security.jwt.refresh-expiry-seconds:2592000}") long refreshTokenExpiry,
            @Value("${app.security.jwt.issuer:managio}") String issuer) {

        if (accessSecret.length()  < 32) throw new IllegalArgumentException(
                "Access token secret must be at least 32 characters");
        if (refreshSecret.length() < 32) throw new IllegalArgumentException(
                "Refresh token secret must be at least 32 characters");

        this.accessTokenKey    = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey   = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry  = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.issuer             = issuer;
    }

    // ── User / Staff access token ─────────────────────────────────────────────

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",       user.getId());
        claims.put("email",        user.getEmail());
        claims.put("roles",        user.getRoles().stream().map(Enum::name)
                .collect(Collectors.toList()));
        claims.put("emailVerified", user.getEmailVerified());
        claims.put("type",          "access");

        /*
         * ROOT CAUSE FIX — logoutJourney re-login token rejected (401):
         *
         * JwtUtil.generateAccessToken() previously produced identical token strings
         * when called twice for the same user within the same second — because "iat"
         * (issued-at) and "exp" share second-level precision, all other claims are
         * identical, and the HMAC key never changes.
         *
         * After logout the first token is blacklisted.  On re-login within the same
         * second the new token is byte-for-byte identical → also blacklisted → 401.
         *
         * Fix: include a "jti" (JWT ID) claim containing a random UUID.  JTI makes
         * every generated token string unique regardless of timing, so the re-login
         * token cannot accidentally match the previously blacklisted one.
         */
        claims.put("jti", UUID.randomUUID().toString());

        return buildToken(claims, user.getId().toString(), accessTokenExpiry, accessTokenKey);
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type",   "refresh");
        claims.put("jti",    UUID.randomUUID().toString());

        return buildToken(claims, user.getId().toString(), refreshTokenExpiry, refreshTokenKey);
    }

    // ── Staff access token (includes business context) ────────────────────────

    public String generateStaffAccessToken(User user, Staff staff) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId",       user.getId());
        claims.put("email",        user.getEmail());
        claims.put("roles",        user.getRoles().stream().map(Enum::name)
                .collect(Collectors.toList()));
        claims.put("emailVerified",           user.getEmailVerified());
        claims.put("type",                    "access");
        claims.put("jti",                     UUID.randomUUID().toString());
        claims.put("staffId",                 staff.getId());
        claims.put("businessId",              staff.getBusinessId());
        claims.put("staffRole",               staff.getRole().name());
        claims.put("canManageMembers",        staff.getCanManageMembers());
        claims.put("canManagePayments",       staff.getCanManagePayments());
        claims.put("canManageSubscriptions",  staff.getCanManageSubscriptions());
        claims.put("canViewReports",          staff.getCanViewReports());

        return buildToken(claims, user.getId().toString(), accessTokenExpiry, accessTokenKey);
    }

    // ── Member access token ───────────────────────────────────────────────────

    public String generateMemberAccessToken(Member member) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId",   member.getId());
        claims.put("businessId", member.getBusinessId());
        claims.put("phone",      member.getPhone());
        claims.put("email",      member.getEmail());
        claims.put("fullName",   member.getFullName());
        claims.put("type",       "access");
        claims.put("userType",   "member");
        claims.put("jti",        UUID.randomUUID().toString());

        return buildToken(claims, member.getId().toString(), accessTokenExpiry, accessTokenKey);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public Claims validateAccessToken(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(accessTokenKey)
                    .requireIssuer(issuer)
                    .build();

            Jws<Claims> jws    = parser.parseSignedClaims(token);
            Claims      claims = jws.getPayload();

            if (!"access".equals(claims.get("type", String.class))) {
                throw new JwtException("Invalid token type");
            }
            return claims;

        } catch (ExpiredJwtException e) {
            log.debug("Access token expired: {}", e.getMessage());
            throw new JwtException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new JwtException("Unsupported token", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new JwtException("Malformed token", e);
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new JwtException("Invalid signature", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw new JwtException("Empty claims", e);
        }
    }

    public Claims validateRefreshToken(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(refreshTokenKey)
                    .requireIssuer(issuer)
                    .build();

            Jws<Claims> jws    = parser.parseSignedClaims(token);
            Claims      claims = jws.getPayload();

            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new JwtException("Invalid token type");
            }
            return claims;
        } catch (JwtException e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    // ── Convenience accessors ─────────────────────────────────────────────────

    public Long getUserIdFromToken(String token) {
        return validateAccessToken(token).get("userId", Long.class);
    }

    public String getEmailFromToken(String token) {
        return validateAccessToken(token).get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.Set<String> getRolesFromToken(String token) {
        java.util.List<String> roles = validateAccessToken(token)
                .get("roles", java.util.List.class);
        return new java.util.HashSet<>(roles);
    }

    public boolean isTokenExpired(String token) {
        try {
            return validateAccessToken(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public long getAccessTokenExpiry()  { return accessTokenExpiry;  }
    public long getRefreshTokenExpiry() { return refreshTokenExpiry; }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // ── Private builder ───────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> claims, String subject,
                              long expirySeconds, SecretKey key) {
        Instant now    = Instant.now();
        Instant expiry = now.plusSeconds(expirySeconds);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS512)
                .compact();
    }
}