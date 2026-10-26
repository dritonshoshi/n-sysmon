package com.nsysmon.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AAllowAllWebAuthorizerTest {
    @Test void authorizesEveryRequest() {
        assertTrue(AAllowAllWebAuthorizer.INSTANCE.isAuthorized(mock(HttpServletRequest.class)));
    }

    @Test void defaultUnauthorizedResponseIsAnEmptyForbidden() throws Exception {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        final HttpServletResponse response = mock(HttpServletResponse.class);

        AAllowAllWebAuthorizer.INSTANCE.onUnauthorized(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setHeader("Cache-Control", "no-store");
        verify(response, never()).getOutputStream();
    }
}
