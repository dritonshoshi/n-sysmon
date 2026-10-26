package com.nsysmon.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AAbstractBasicWebAuthorizerTest {
    private static final String USER = "user";
    private static final String PASSWORD = "secret";

    private final AAbstractBasicWebAuthorizer authorizer = new AAbstractBasicWebAuthorizer() {
        @Override protected boolean checkCredentials(String user, String password) {
            return USER.equals(user) && PASSWORD.equals(password);
        }
    };

    @Test void acceptsTheConfiguredCredentials() {
        assertTrue(authorizer.isAuthorized(requestWith(basicHeader(USER + ":" + PASSWORD))));
    }

    @Test void acceptsTheSchemeCaseInsensitively() {
        assertTrue(authorizer.isAuthorized(requestWith("basic " + encode(USER + ":" + PASSWORD))));
    }

    @Test void rejectsARequestWithoutAuthorizationHeader() {
        assertFalse(authorizer.isAuthorized(mock(HttpServletRequest.class)));
    }

    @Test void rejectsAnotherScheme() {
        assertFalse(authorizer.isAuthorized(requestWith("Bearer " + encode(USER + ":" + PASSWORD))));
    }

    @Test void rejectsCredentialsThatAreNotValidBase64() {
        assertFalse(authorizer.isAuthorized(requestWith("Basic this-is-not-base64!")));
    }

    @Test void rejectsCredentialsWithoutASeparator() {
        assertFalse(authorizer.isAuthorized(requestWith(basicHeader(USER))));
    }

    @Test void rejectsAWrongPassword() {
        assertFalse(authorizer.isAuthorized(requestWith(basicHeader(USER + ":wrong"))));
    }

    @Test void rejectsAnUnknownUser() {
        assertFalse(authorizer.isAuthorized(requestWith(basicHeader("other:" + PASSWORD))));
    }

    @Test void splitsAtTheFirstSeparatorSoThatPasswordsMayContainColons() {
        final RecordingAuthorizer recording = new RecordingAuthorizer();

        assertTrue(recording.isAuthorized(requestWith(basicHeader("user:sec:ret"))));
        assertEquals("user", recording.user);
        assertEquals("sec:ret", recording.password);
    }

    @Test void unauthorizedResponseCarriesTheBasicChallengeWithoutABody() throws Exception {
        final HttpServletResponse response = mock(HttpServletResponse.class);

        authorizer.onUnauthorized(mock(HttpServletRequest.class), response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setHeader("WWW-Authenticate", "Basic realm=\"n-sysmon\", charset=\"UTF-8\"");
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response, never()).getOutputStream();
    }

    private static HttpServletRequest requestWith(String authorizationHeader) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        return request;
    }

    private static String basicHeader(String credentials) {
        return "Basic " + encode(credentials);
    }

    private static String encode(String credentials) {
        return Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    /** Captures the credentials the base class extracted. */
    private static final class RecordingAuthorizer extends AAbstractBasicWebAuthorizer {
        private String user;
        private String password;

        @Override protected boolean checkCredentials(String user, String password) {
            this.user = user;
            this.password = password;
            return true;
        }
    }
}
