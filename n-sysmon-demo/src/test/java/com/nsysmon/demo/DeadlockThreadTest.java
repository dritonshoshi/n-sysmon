package com.nsysmon.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only the structure of {@link DeadlockThread} is asserted here. Its {@code run()} method is deliberately not
 * executed: it provokes a real, permanent monitor deadlock between two non-interruptible threads and blocks for one
 * second before it does so, so running it would leak two permanently blocked threads into the test JVM.
 */
class DeadlockThreadTest {

    @Test void isAPlainThreadSubclass() {
        final DeadlockThread thread = new DeadlockThread();

        assertTrue(thread instanceof Thread);
    }

    @Test void holdsTwoDistinctLocksForTheTwoDeadlockingThreads() {
        final DeadlockThread thread = new DeadlockThread();

        assertNotSame(thread.lockA, thread.lockB);
        assertNotSame(thread.lockA, new DeadlockThread().lockA);
    }
}
