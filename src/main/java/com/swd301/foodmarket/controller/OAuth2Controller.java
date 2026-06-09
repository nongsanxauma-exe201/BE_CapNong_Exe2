package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.CompleteProfileRequest;
import com.swd301.foodmarket.dto.request.GoogleMobileRequest;
import com.swd301.foodmarket.dto.response.ApiResponse;
import com.swd301.foodmarket.dto.response.AuthenticationResponse;
import com.swd301.foodmarket.entity.User;
import com.swd301.foodmarket.repository.UserRepository;
import com.swd301.foodmarket.service.OAuthService;
import com.swd301.foodmarket.service.impl.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuthService oAuthService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${FRONTEND_URL}")
    private String FE_URL;

    @GetMapping("/google")
    public Map<String, String> loginWithGoogle() {
        return Map.of("url", FE_URL + "/api/v1/oauth2/authorization/google");
    }

    /**
     * Xác thực Google từ mobile app.
     * Client gửi accessToken nhận được từ expo-auth-session.
     * BE gọi Google userinfo API để verify, rồi trả JWT hoặc thông tin đăng ký.
     */
    @PostMapping("/google-mobile")
    public ResponseEntity<ApiResponse<?>> googleMobileLogin(@RequestBody GoogleMobileRequest request) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(request.getAccessToken());
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> googleResp = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (!googleResp.getStatusCode().is2xxSuccessful() || googleResp.getBody() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.<Void>builder().code(401).message("Google token không hợp lệ").build());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> googleUser = googleResp.getBody();
            String email  = (String) googleUser.get("email");
            String name   = (String) googleUser.getOrDefault("name", "");
            String avatar = (String) googleUser.getOrDefault("picture", "");

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Void>builder().code(400).message("Không lấy được email từ Google").build());
            }

            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                // User đã tồn tại → trả JWT
                String token = jwtService.generateToken(user);
                return ResponseEntity.ok(
                        ApiResponse.<AuthenticationResponse>builder()
                                .code(1000)
                                .result(AuthenticationResponse.builder()
                                        .authenticated(true)
                                        .token(token)
                                        .build())
                                .build()
                );
            } else {
                // User chưa có → trả data để client điều hướng đăng ký
                Map<String, Object> regData = new HashMap<>();
                regData.put("needsRegistration", true);
                regData.put("email",  email);
                regData.put("name",   name);
                regData.put("avatar", avatar);

                return ResponseEntity.ok(
                        ApiResponse.<Map<String, Object>>builder()
                                .code(1000)
                                .result(regData)
                                .build()
                );
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<Void>builder().code(401).message("Lỗi xác thực Google: " + e.getMessage()).build());
        }
    }

    @PutMapping("/complete-profile")
    public String completeProfile(
            @RequestBody CompleteProfileRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        oAuthService.completeProfile(email, request.getRoleName());
        return "Update role successfully";
    }
}