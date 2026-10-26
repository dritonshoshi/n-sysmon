package com.nsysmon.servlet.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Authorizes access to the N-SysMon web front. The implementation is chosen through the
 *  {@code web-authorizer} configuration property and is instantiated once at start-up.
 * <p>
 * A single instance is shared by all requests, so an implementation must be thread-safe.
 * <p>
 * Only the web front served by {@link com.nsysmon.servlet.NSysMonServlet} is guarded. Data
 *  ingestion, e.g. the upload endpoint of the N-SysMon server, is deliberately not affected.
 *
 * @author arno
 */
public interface AWebAuthorizer {
    /**
     * @return true if the request may access the N-SysMon web front
     */
    boolean isAuthorized(HttpServletRequest request);

    /**
     * Writes the response for a request rejected by {@link #isAuthorized(HttpServletRequest)}. The
     *  default sends a status 403 without a body, so that nothing of the web front is disclosed.
     *  Implementations that want a browser login dialog (HTTP Basic) or a redirect to a login page
     *  override this method.
     */
    default void onUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
