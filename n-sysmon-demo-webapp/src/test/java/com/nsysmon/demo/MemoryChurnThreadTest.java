package com.nsysmon.demo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryChurnThreadTest {

    @Test void isADaemonSoItCannotKeepTheJvmAlive() {
        final MemoryChurnThread thread = new MemoryChurnThread(10, 10, 1000, 1000);

        assertTrue(thread.isDaemon());
        assertEquals(MemoryChurnThread.NAME, thread.getName());
    }

    @Test void keepsRunningUntilItIsInterrupted() throws Exception {
        final MemoryChurnThread thread = new MemoryChurnThread(10, 10, 60000, 1);
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
