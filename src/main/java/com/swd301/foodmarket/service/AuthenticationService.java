package com.swd301.foodmarket.service;

import com.nimbusds.jose.JOSEException;
import com.swd301.foodmarket.dto.request.AuthenticationRequest;
import com.swd301.foodmarket.dto.request.IntrospectRequest;
import com.swd301.foodmarket.dto.request.LogoutRequest;
import com.swd301.foodmarket.dto.request.RefreshRequest;
import com.swd301.foodmarket.dto.response.AuthenticationResponse;
import com.swd301.foodmarket.dto.response.IntrospectResponse;

import java.text.ParseException;

public interface AuthenticationService {
    AuthenticationResponse authenticated(AuthenticationRequest request);

    IntrospectResponse introspect(IntrospectRequest request)
            throws ParseException, JOSEException;

    void logout(LogoutRequest request)
            throws ParseException, JOSEException;

    AuthenticationResponse refreshToken(RefreshRequest request)
            throws ParseException, JOSEException;
}
