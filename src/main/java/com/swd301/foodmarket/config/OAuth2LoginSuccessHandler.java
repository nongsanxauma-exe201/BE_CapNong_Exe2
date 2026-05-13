package com.swd301.foodmarket.config;

import com.swd301.foodmarket.entity.Role;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.enums.RoleName;
import com.swd301.foodmarket.enums.UserStatus;
import com.swd301.foodmarket.repository.RoleRepository;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.impl.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    @Value("${FRONTEND_URL}")
    private  String FE_URL;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");
        String avatar = oAuth2User.getAttribute("picture");

        //  Lấy role từ query param hoặc session
        String roleParam = request.getParameter("role");
        if (roleParam == null || roleParam.isBlank()) {
            roleParam = (String) request.getSession().getAttribute("pending_role");
            request.getSession().removeAttribute("pending_role");
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            // User doesn't exist. Instead of auto-creating, we redirect to the frontend registration form
            // URL encode the parameters to ensure safe transmission
            String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            String encodedName = fullName != null ? java.net.URLEncoder.encode(fullName, java.nio.charset.StandardCharsets.UTF_8) : "";
            String encodedAvatar = avatar != null ? java.net.URLEncoder.encode(avatar, java.nio.charset.StandardCharsets.UTF_8) : "";

            // Determine if a role was pre-selected on the frontend
            String roleQuery = (roleParam != null && !roleParam.isBlank()) 
                                ? "&role=" + java.net.URLEncoder.encode(roleParam, java.nio.charset.StandardCharsets.UTF_8)
                                : "";

            response.sendRedirect(
                    FE_URL+"/oauth-register?email=" + encodedEmail
                    + "&name=" + encodedName 
                    + "&avatar=" + encodedAvatar 
                    + roleQuery
            );
            return;
        }

        // ===== GENERATE JWT =====
        String token = jwtService.generateToken(user);

        // ===== REDIRECT VỀ FRONTEND =====
        response.sendRedirect(
                FE_URL+"/oauth-success?token=" + token
        );
    }
}