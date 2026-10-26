package com.nsysmon.demo;

import com.nsysmon.servlet.auth.AAbstractBasicWebAuthorizer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Demo authorizer for the N-SysMon web front: accepts the fixed credentials {@code admin} / {@code admin}.
 * <p>
 * <b>Demo only.</b> Never use fixed credentials like these in production; configure a real authorizer
 *  (LDAP, SSO, ...) instead. HTTP Basic sends the credentials in clear text, so the web front must only
 *  be reached through TLS.
 *
 * @author arno
 */
public class DemoWebAuthorizer extends AAbstractBasicWebAuthorizer {
    static final String USER = "admin";
    static final String PASSWORD = "admin";

    @Override protected boolean checkCredentials(String user, String password) {
        return constantTimeEquals(USER, user) & constantTimeEquals(PASSWORD, password);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
