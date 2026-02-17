package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.User;
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
import java.util.stream.Collectors;
import com.nitin.saas.staff.entity.Staff;  // ADD THIS
import com.nitin.saas.member.entity.Member;  // ADD THIS
@Component
@Slf4j
public class JwtUtil {

    private final SecretKey accessTokenKey;
    private final SecretKey refreshTokenKey;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;
    private final String issuer;

    public JwtUtil(
            @Value("${app.security.jwt.access-secret}") String accessSecret,
            @Value("${app.security.jwt.refresh-secret}") String refreshSecret,
            @Value("${app.security.jwt.access-expiry-seconds:900}") long accessTokenExpiry,
            @Value("${app.security.jwt.refresh-expiry-seconds:2592000}") long refreshTokenExpiry,
            @Value("${app.security.jwt.issuer:managio}") String issuer) {

        if (accessSecret.length() < 32) {
            throw new IllegalArgumentException("Access token secret must be at least 32 characters");
        }
        if (refreshSecret.length() < 32) {
            throw new IllegalArgumentException("Refresh token secret must be at least 32 characters");
        }

        this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
        this.issuer = issuer;
    }

    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toList()));
        claims.put("emailVerified", user.getEmailVerified());
        claims.put("type", "access");

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiry);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(accessTokenKey, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("type", "refresh");

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(refreshTokenExpiry);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(refreshTokenKey, Jwts.SIG.HS512)
                .compact();
    }

    public Claims validateAccessToken(String token) {
        try {
            JwtParser parser = Jwts.parser()
                    .verifyWith(accessTokenKey)
                    .requireIssuer(issuer)
                    .build();

            Jws<Claims> jws = parser.parseSignedClaims(token);
            Claims claims = jws.getPayload();

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

            Jws<Claims> jws = parser.parseSignedClaims(token);
            Claims claims = jws.getPayload();

            if (!"refresh".equals(claims.get("type", String.class))) {
                throw new JwtException("Invalid token type");
            }

            return claims;
        } catch (JwtException e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            throw e;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.get("userId", Long.class);
    }

    public String getEmailFromToken(String token) {
        Claims claims = validateAccessToken(token);
        return claims.get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public java.util.Set<String> getRolesFromToken(String token) {
        Claims claims = validateAccessToken(token);
        java.util.List<String> roles = claims.get("roles", java.util.List.class);
        return new java.util.HashSet<>(roles);
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAccessToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public long getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public long getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    public String extractTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    // ADD THESE METHODS TO JWT UTIL CLASS

    /**
     * Generate access token for staff member with business context
     */
    public String generateStaffAccessToken(User user, Staff staff) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("roles", user.getRoles().stream().map(Enum::name).collect(Collectors.toList()));
        claims.put("emailVerified", user.getEmailVerified());
        claims.put("type", "access");

        // Staff-specific claims
        claims.put("staffId", staff.getId());
        claims.put("businessId", staff.getBusinessId());
        claims.put("staffRole", staff.getRole().name());
        claims.put("canManageMembers", staff.getCanManageMembers());
        claims.put("canManagePayments", staff.getCanManagePayments());
        claims.put("canManageSubscriptions", staff.getCanManageSubscriptions());
        claims.put("canViewReports", staff.getCanViewReports());

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiry);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(accessTokenKey, Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Generate access token for member
     */
    public String generateMemberAccessToken(Member member) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("memberId", member.getId());
        claims.put("businessId", member.getBusinessId());
        claims.put("phone", member.getPhone());
        claims.put("email", member.getEmail());
        claims.put("fullName", member.getFullName());
        claims.put("type", "access");
        claims.put("userType", "member");

        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiry);

        return Jwts.builder()
                .claims(claims)
                .subject(member.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(accessTokenKey, Jwts.SIG.HS512)
                .compact();
    }
}