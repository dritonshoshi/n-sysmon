package com.nsysmon.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DaemonThreadFactoryTest {

    private final DaemonThreadFactory factory = new DaemonThreadFactory();

    @Test
    void newThreadImplementsTheThreadFactoryContract() {
        assertTrue(ThreadFactory.class.isAssignableFrom(DaemonThreadFactory.class));
    }

    @Test
    void theProducedThreadIsADaemon() {
        assertTrue(factory.newThread(() -> { }).isDaemon());
    }

    @Test
    void theProducedThreadRunsTheGivenRunnable() throws InterruptedException {
        final boolean[] ran = {false};
        final Thread thread = factory.newThread(() -> ran[0] = true);

        thread.start();
        thread.join();

        assertTrue(ran[0]);
    }

    @Test
    void eachCallProducesANewThread() {
        assertNotSame(factory.newThread(() -> { }), factory.newThread(() -> { }));
    }

    @Test
    void shutdownableIsAnInterfaceWithAShutdownMethod() throws Exception {
        assertTrue(AShutdownable.class.isInterface());
        assertSame(void.class, AShutdownable.class.getMethod("shutdown").getReturnType());
    }
}