package com.managio.modules.admin.web;

import com.managio.modules.admin.service.AdminAccess;
import com.managio.platform.error.ApiException;
import com.managio.platform.security.Principals.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Guards every {@code /api/v1/admin/**} endpoint, so a new console endpoint can't be added without the check.
 * Each use is logged with the administrator's user ID only, as an access trail.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AdminAccessInterceptor implements HandlerInterceptor {

    private final AdminAccess access;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            throw ApiException.forbidden("The platform console is only for Managio's administrators.");
        }
        access.require(user.id());
        log.info("Platform console: user {} {} {}", user.id(), request.getMethod(), request.getRequestURI());
        return true;
    }

    @Configuration
    @RequiredArgsConstructor
    static class Registration implements WebMvcConfigurer {

        private final AdminAccessInterceptor interceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(interceptor).addPathPatterns("/api/v1/admin/**");
        }
    }
}
