package com.swd301.foodmarket.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository repo, String authorizationRequestBaseUri) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(repo, authorizationRequestBaseUri);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = this.defaultResolver.resolve(request);
        return customizeAuthorizationRequest(request, authRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = this.defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(request, authRequest);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(HttpServletRequest request, OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) {
            return null;
        }

        String role = request.getParameter("role");
        if (role != null && !role.isBlank()) {
            request.getSession().setAttribute("pending_role", role);
        }

        if (role == null || role.isBlank()) {
            return authRequest;
        }

        Map<String, Object> additionalParameters = new LinkedHashMap<>(authRequest.getAdditionalParameters());
        additionalParameters.put("role", role);

        return OAuth2AuthorizationRequest.from(authRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}
