package com.swd301.foodmarket.controller;

import com.swd301.foodmarket.dto.request.CompleteProfileRequest;
import com.swd301.foodmarket.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class OAuth2Controller {

    private final OAuthService oAuthService;
    @Value("${FRONTEND_URL}")
    private String FE_URL;
    @GetMapping("/google")
    public Map<String, String> loginWithGoogle() {
        return Map.of(
                "url", FE_URL+"/api/v1/oauth2/authorization/google"
        );
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