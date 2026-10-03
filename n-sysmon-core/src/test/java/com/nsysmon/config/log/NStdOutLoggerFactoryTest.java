package com.nsysmon.config.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class NStdOutLoggerFactoryTest {

    @Test
    void getLoggerReturnsStdOutBackedLogger() {
        final NSysMonLogger result = NStdOutLoggerFactory.INSTANCE.getLogger(NStdOutLoggerFactoryTest.class);

        assertInstanceOf(NStdOutLogger.class, result);
    }

    @Test
    void getLoggerReturnsANewLoggerPerCall() {
        assertNotSame(
                NStdOutLoggerFactory.INSTANCE.getLogger(Object.class),
                NStdOutLoggerFactory.INSTANCE.getLogger(Object.class));
    }

    @Test
    void instanceIsShared() {
        assertSame(NStdOutLoggerFactory.INSTANCE, NStdOutLoggerFactory.INSTANCE);
    }
}