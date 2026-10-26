package com.nsysmon.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Base class for authorizers that use HTTP Basic authentication. It implements the header parsing
 *  and the login challenge; subclasses only decide whether a pair of credentials is accepted.
 * <p>
 * HTTP Basic sends the credentials in clear text. Use it behind TLS, or implement
 *  {@link AWebAuthorizer} directly for a scheme that does not transmit reusable credentials.
 *
 * @author arno
 */
public abstract class AAbstractBasicWebAuthorizer implements AWebAuthorizer {
    private static final String BASIC_PREFIX = "Basic ";
    private static final String CHALLENGE = "Basic realm=\"n-sysmon\", charset=\"UTF-8\"";

    @Override public final boolean isAuthorized(HttpServletRequest request) {
        final Credentials credentials = parseCredentials(request);
        return credentials != null && checkCredentials(credentials.user, credentials.password);
    }

    /**
     * @return true if the given credentials grant access to the web front. Implementations should
     *  compare secrets in constant time.
     */
    protected abstract boolean checkCredentials(String user, String password);

    @Override public void onUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("WWW-Authenticate", CHALLENGE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private static Credentials parseCredentials(HttpServletRequest request) {
        final String header = request.getHeader("Authorization");
        if(header == null || !header.regionMatches(true, 0, BASIC_PREFIX, 0, BASIC_PREFIX.length())) {
            return null;
        }

        try {
            final String decoded = new String(Base64.getDecoder().decode(header.substring(BASIC_PREFIX.length()).trim()), StandardCharsets.UTF_8);
            final int separator = decoded.indexOf(':');
            if(separator < 0) {
                return null;
            }
            return new Credentials(decoded.substring(0, separator), decoded.substring(separator + 1));
        } catch(IllegalArgumentException exc) {
            // invalid Base64
            return null;
        }
    }

    private static final class Credentials {
        private final String user;
        private final String password;

        private Credentials(String user, String password) {
            this.user = user;
            this.password = password;
        }
    }
}
