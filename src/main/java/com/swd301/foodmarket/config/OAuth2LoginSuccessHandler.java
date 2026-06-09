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

        // Kiểm tra client type từ session
        String clientType = (String) request.getSession().getAttribute("client_type");
        request.getSession().removeAttribute("client_type");
        boolean isMobile = "mobile".equals(clientType);

        // App scheme cho mobile deep link
        final String APP_SCHEME = "nongsanxauma://";

        User user = userRepository.findByEmail(email).orElse(null);

        String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
        String encodedName = fullName != null ? java.net.URLEncoder.encode(fullName, java.nio.charset.StandardCharsets.UTF_8) : "";
        String encodedAvatar = avatar != null ? java.net.URLEncoder.encode(avatar, java.nio.charset.StandardCharsets.UTF_8) : "";

        if (user == null) {
            String roleQuery = (roleParam != null && !roleParam.isBlank())
                                ? "&role=" + java.net.URLEncoder.encode(roleParam, java.nio.charset.StandardCharsets.UTF_8)
                                : "";

            String registerUrl = isMobile
                    ? APP_SCHEME + "oauth-register?email=" + encodedEmail + "&name=" + encodedName + "&avatar=" + encodedAvatar + roleQuery
                    : FE_URL + "/oauth-register?email=" + encodedEmail + "&name=" + encodedName + "&avatar=" + encodedAvatar + roleQuery;

            response.sendRedirect(registerUrl);
            return;
        }

        // ===== GENERATE JWT =====
        String token = jwtService.generateToken(user);

        // ===== REDIRECT VỀ FRONTEND hoặc APP =====
        String successUrl = isMobile
                ? APP_SCHEME + "oauth-success?token=" + token
                : FE_URL + "/oauth-success?token=" + token;

        response.sendRedirect(successUrl);
    }
}