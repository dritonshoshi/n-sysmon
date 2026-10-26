package com.nsysmon.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoWebAuthorizerTest {
    private final DemoWebAuthorizer authorizer = new DemoWebAuthorizer();

    @Test void acceptsTheDemoCredentials() {
        assertTrue(authorizer.isAuthorized(requestWith("admin", "admin")));
    }

    @Test void rejectsADifferentPassword() {
        assertFalse(authorizer.isAuthorized(requestWith("admin", "secret")));
    }

    @Test void rejectsADifferentUser() {
        assertFalse(authorizer.isAuthorized(requestWith("root", "admin")));
    }

    private static HttpServletRequest requestWith(String user, String password) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final String credentials = user + ":" + password;
        final String header = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        when(request.getHeader("Authorization")).thenReturn(header);
        return request;
    }
}
