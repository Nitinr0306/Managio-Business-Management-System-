package com.nitin.saas.common.security;

import com.nitin.saas.auth.entity.User;
import com.nitin.saas.auth.enums.Role;
import com.nitin.saas.auth.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.sql.rowset.serial.SerialException;
import java.io.IOException;

public class OAuth2SuccessHandler  implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public OAuth2SuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    )throws IOException, ServletException {
        OAuth2User oAuth2User=(OAuth2User) authentication.getPrincipal();
        String email=oAuth2User.getAttribute("email");
        String googleId=oAuth2User.getAttribute("sub");
        if (email == null || googleId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Google account");
            return;
        }

        User user=userRepository.findByEmail(email).orElseGet(()->{
            User newUser=new User();
            newUser.setEmail(email);
            newUser.setProvider("GOOGLE");
            newUser.setProviderId(googleId);
            newUser.setEmailVerified(true);
            newUser.setRole(Role.USER);
            return userRepository.save(newUser);
        });

        String token = JwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
        response.sendRedirect(
                "http://localhost:3000/oauth-success?token=" + token
        );
    }
}
