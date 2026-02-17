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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = extractJwtFromRequest(request);

            if (jwt != null) {
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("Blacklisted token attempted: {}", jwt.substring(0, 20));
                    filterChain.doFilter(request, response);
                    return;
                }

                Claims claims = jwtUtil.validateAccessToken(jwt);
                Long userId = claims.get("userId", Long.class);
                String email = claims.get("email", String.class);

                @SuppressWarnings("unchecked")
                List<String> roleNames = claims.get("roles", List.class);
                Set<Role> roles = roleNames.stream()
                        .map(Role::valueOf)
                        .collect(Collectors.toSet());

                User user = userRepository.findById(userId).orElse(null);

                if (user != null && user.getEnabled() && !user.getAccountLocked()) {
                    UserPrincipal userPrincipal = UserPrincipal.builder()
                            .id(userId)
                            .email(email)
                            .roles(roles)
                            .enabled(user.getEnabled())
                            .accountLocked(user.getAccountLocked())
                            .emailVerified(user.getEmailVerified())
                            .accountNonExpired(user.isAccountNonExpired())
                            .credentialsNonExpired(user.isCredentialsNonExpired())
                            .build();

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userPrincipal, null, userPrincipal.getAuthorities());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Set authentication for user: {}", email);
                }
            }
        } catch (JwtException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}