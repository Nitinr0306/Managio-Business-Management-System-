package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT authentication filter — runs once per request.
 *
 * FIX CVL-004: Member tokens (userType=member) are now explicitly identified
 * and isolated. They populate the SecurityContext with a MemberPrincipal that
 * carries ROLE_MEMBER authority so that member-only endpoints can verify
 * the caller identity correctly without conflicting with User IDs.
 *
 * Regular User/Staff tokens carry no userType claim (or userType=user) and
 * continue to populate UserPrincipal as before.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil               jwtUtil;
    private final UserRepository        userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwt(request);

            if (jwt != null) {

                // Reject blacklisted tokens (logged-out access tokens)
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("Rejected blacklisted token from {}", request.getRemoteAddr());
                    filterChain.doFilter(request, response);
                    return;
                }

                Claims claims = jwtUtil.validateAccessToken(jwt);

                // ── FIX CVL-004: route by userType claim ──────────────────────────
                String userType = claims.get("userType", String.class);

                if ("member".equals(userType)) {
                    authenticateMember(claims, request);
                } else {
                    authenticateUser(claims, request);
                }
            }

        } catch (JwtException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            // Do not set authentication — request proceeds as anonymous
        } catch (Exception ex) {
            log.error("Unexpected error in JwtAuthFilter: {}", ex.getMessage(), ex);
        }

        filterChain.doFilter(request, response);
    }

    // ── User / Staff token ────────────────────────────────────────────────────

    private void authenticateUser(Claims claims, HttpServletRequest request) {
        Long userId = claims.get("userId", Long.class);
        if (userId == null) return;

        String email = claims.get("email", String.class);

        @SuppressWarnings("unchecked")
        List<String> roleNames = claims.get("roles", List.class);
        if (roleNames == null) return;

        Set<Role> roles = roleNames.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.getEnabled() || user.getAccountLocked()) return;

        UserPrincipal principal = UserPrincipal.builder()
                .id(userId)
                .email(email)
                .roles(roles)
                .enabled(user.getEnabled())
                .accountLocked(user.getAccountLocked())
                .emailVerified(user.getEmailVerified())
                .accountNonExpired(user.isAccountNonExpired())
                .credentialsNonExpired(user.isCredentialsNonExpired())
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.debug("Authenticated user: {}", email);
    }

    // ── Member token ──────────────────────────────────────────────────────────

    /**
     * Member tokens carry memberId (not userId) and have no system Role enum values.
     * We create a lightweight MemberPrincipal with ROLE_MEMBER authority so that
     * member endpoints can distinguish the caller type.
     */
    private void authenticateMember(Claims claims, HttpServletRequest request) {
        Long memberId = claims.get("memberId", Long.class);
        if (memberId == null) return;

        Long   businessId = claims.get("businessId", Long.class);
        String phone      = claims.get("phone",      String.class);
        String email      = claims.get("email",      String.class);
        String fullName   = claims.get("fullName",   String.class);

        // Single authority for member tokens
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

        MemberPrincipal principal = MemberPrincipal.builder()
                .memberId(memberId)
                .businessId(businessId)
                .phone(phone)
                .email(email)
                .fullName(fullName)
                .build();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        log.debug("Authenticated member: memberId={}, businessId={}", memberId, businessId);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String extractJwt(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}