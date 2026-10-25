package com.nsysmon.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Authorizer that lets every request through. It is the default when no {@code web-authorizer} is
 *  configured, which keeps the web front open as in previous releases.
 *
 * @author arno
 */
public final class AAllowAllWebAuthorizer implements AWebAuthorizer {
    public static final AAllowAllWebAuthorizer INSTANCE = new AAllowAllWebAuthorizer();

    /**
     * Public so that the authorizer can also be configured explicitly through the
     *  {@code web-authorizer} property.
     */
    public AAllowAllWebAuthorizer() {
        // stateless
    }

    @Override public boolean isAuthorized(HttpServletRequest request) {
        return true;
    }
}
