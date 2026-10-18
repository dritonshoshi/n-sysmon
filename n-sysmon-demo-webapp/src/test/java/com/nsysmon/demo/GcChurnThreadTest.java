package com.nsysmon.demo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GcChurnThreadTest {

    @Test void isADaemonSoItCannotKeepTheJvmAlive() {
        final GcChurnThread thread = new GcChurnThread(1000, 1000);

        assertTrue(thread.isDaemon());
        assertEquals(GcChurnThread.NAME, thread.getName());
    }

    @Test void keepsRunningUntilItIsInterrupted() throws Exception {
        final GcChurnThread thread = new GcChurnThread(60000, 1);
        thread.start();
        try {
            TimeUnit.MILLISECONDS.sleep(50);

            assertTrue(thread.isAlive());
        } finally {
            thread.interrupt();
            thread.join(2000);
        }

        assertFalse(thread.isAlive());
    }
}
