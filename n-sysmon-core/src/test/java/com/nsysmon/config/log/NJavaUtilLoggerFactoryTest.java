package com.nsysmon.config.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class NJavaUtilLoggerFactoryTest {

    @Test
    void getLoggerReturnsJavaUtilBackedLogger() {
        final NSysMonLogger result = NJavaUtilLoggerFactory.INSTANCE.getLogger(NJavaUtilLoggerFactoryTest.class);

        assertInstanceOf(NJavaUtilLogger.class, result);
    }

    @Test
    void getLoggerReturnsANewLoggerPerCall() {
        assertNotSame(
                NJavaUtilLoggerFactory.INSTANCE.getLogger(Object.class),
                NJavaUtilLoggerFactory.INSTANCE.getLogger(Object.class));
    }

    @Test
    void instanceIsShared() {
        assertSame(NJavaUtilLoggerFactory.INSTANCE, NJavaUtilLoggerFactory.INSTANCE);
    }
}
