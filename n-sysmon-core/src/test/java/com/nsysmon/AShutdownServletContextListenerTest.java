package com.nsysmon;

import jakarta.servlet.ServletContextListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Covers the listener's wiring only. {@code contextDestroyed} is deliberately not exercised: it shuts
 * down the process wide {@link NSysMon} singleton, which other tests share.
 */
class AShutdownServletContextListenerTest {

    private final AShutdownServletContextListener listener = new AShutdownServletContextListener();

    @Test
    void theListenerIsAServletContextListener() {
        assertInstanceOf(ServletContextListener.class, listener);
    }

    @Test
    void contextInitializedDoesNothing() {
        assertDoesNotThrow(() -> listener.contextInitialized(null));
    }
}
