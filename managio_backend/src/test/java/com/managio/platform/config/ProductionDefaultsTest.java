package com.managio.platform.config;

import com.managio.platform.security.RefreshCookies;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseCookie;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What production gets when a variable is left unset (Render: DEPLOYMENT.md). The safe value must be the default, so
 * forgetting a setting can never weaken production.
 */
class ProductionDefaultsTest {

    private static PropertySource<?> applicationYml() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"));
        return sources.getFirst();
    }

    private static String raw(String key) throws IOException {
        return String.valueOf(applicationYml().getProperty(key));
    }

    @Test
    void unsetVariablesFallBackToTheSafeChoice() throws IOException {
        assertThat(raw("managio.security.secure-cookies")).isEqualTo("${SECURE_COOKIES:true}");
        assertThat(raw("managio.mail.log-content")).isEqualTo("${MAIL_LOG_CONTENT:false}");
        assertThat(raw("springdoc.api-docs.enabled")).isEqualTo("${API_DOCS_ENABLED:false}");
        assertThat(raw("springdoc.swagger-ui.enabled")).isEqualTo("${API_DOCS_ENABLED:false}");
        assertThat(raw("managio.security.rate-limit-multiplier")).isEqualTo("${RATE_LIMIT_MULTIPLIER:1}");
        assertThat(raw("server.forward-headers-strategy")).isEqualTo("none");
        assertThat(raw("server.error.include-message")).isEqualTo("never");
        assertThat(raw("server.error.include-stacktrace")).isEqualTo("never");
        assertThat(raw("management.endpoints.web.exposure.include")).isEqualTo("health");
        assertThat(raw("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        // 8080 belongs to Jenkins on the development machine; Render sets PORT itself.
        assertThat(raw("server.port")).isEqualTo("${PORT:8081}");
    }

    @Test
    void refreshCookiesAreSecureHttpOnlyAndLaxWhenSecureCookiesIsOn() {
        ManagioProperties properties = new ManagioProperties("https://managio.me",
                new ManagioProperties.Security("a-secret-that-is-long-enough-for-hs256-signing!!", Duration.ofMinutes(15),
                        Duration.ofDays(30), true, List.of("https://managio.me"), 1, ""),
                new ManagioProperties.Google(""), new ManagioProperties.Mail("", "Managio <noreply@managio.me>", false),
                new ManagioProperties.Admin(List.of()));
        ResponseCookie cookie = new RefreshCookies(properties).issue(RefreshCookies.USER, "token-value");

        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
        assertThat(cookie.toString()).contains("Secure", "HttpOnly", "SameSite=Lax").doesNotContain("Domain=");
    }
}
