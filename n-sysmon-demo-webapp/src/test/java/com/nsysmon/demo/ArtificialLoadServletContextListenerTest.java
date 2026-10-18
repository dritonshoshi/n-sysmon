package com.nsysmon.demo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The listener is asserted through the threads it starts, which is the only effect it has. All of
 * them are daemon threads, so a failing test cannot keep the test JVM alive.
 */
class ArtificialLoadServletContextListenerTest {

    private final ArtificialLoadServletContextListener listener = new ArtificialLoadServletContextListener();

    @AfterEach void stopTheLoad() {
        listener.contextDestroyed(null);
    }

    @Test void startsTheDeadlockAndBothChurns() {
        listener.contextInitialized(null);

        assertTrue(isAlive(DeadlockThread.NAME));
        assertTrue(isAlive(GcChurnThread.NAME));
        assertTrue(isAlive(MemoryChurnThread.NAME));
    }

    @Test void stopsBothChurnsOnShutdown() throws Exception {
        listener.contextInitialized(null);
        listener.contextDestroyed(null);

        awaitDeath(GcChurnThread.NAME);
        awaitDeath(MemoryChurnThread.NAME);
    }

    @Test void survivesAShutdownWithoutAStartup() {
        // the container is free to call contextDestroyed only, e.g. after a deployment failure; it
        // must not throw, and it must not touch threads it never started. What those threads are is
        // asserted by the tests above - asserting it here too would race with their shutdown.
        assertDoesNotThrow(() -> listener.contextDestroyed(null));
    }

    private static boolean isAlive(String threadName) {
        return Thread.getAllStackTraces().keySet().stream()
                .anyMatch(thread -> threadName.equals(thread.getName()) && thread.isAlive());
    }

    private static void awaitDeath(String threadName) throws InterruptedException {
        for(int i = 0; i < 200 && isAlive(threadName); i++) {
            TimeUnit.MILLISECONDS.sleep(10);
        }

        assertFalse(isAlive(threadName), threadName + " is still alive after interrupt");
    }
}
