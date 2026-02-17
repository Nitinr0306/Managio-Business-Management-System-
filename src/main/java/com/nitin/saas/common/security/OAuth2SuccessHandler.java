package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/auth/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        log.info("OAuth2 login successful for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewUser(email, name, picture));

        String accessToken = jwtUtil.generateAccessToken(user);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", accessToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User createNewUser(String email, String name, String picture) {
        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};

        User user = User.builder()
                .email(email)
                .password("")
                .firstName(nameParts.length > 0 ? nameParts[0] : "")
                .lastName(nameParts.length > 1 ? nameParts[1] : "")
                .profileImageUrl(picture)
                .emailVerified(true)
                .enabled(true)
                .accountLocked(false)
                .roles(Set.of(Role.USER))
                .build();

        user = userRepository.save(user);
        log.info("New user created via OAuth2: {}", email);

        return user;
    }
}