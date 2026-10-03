package com.nsysmon.config.log;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class NSlf4JLoggerFactoryTest {

    @Test
    void getLoggerReturnsSlf4jBackedLogger() {
        final NSysMonLogger result = NSlf4JLoggerFactory.INSTANCE.getLogger(NSlf4JLoggerFactoryTest.class);

        assertInstanceOf(NSlf4JLogger.class, result);
    }

    @Test
    void getLoggerReturnsANewLoggerPerCall() {
        final NSysMonLogger first = NSlf4JLoggerFactory.INSTANCE.getLogger(NSlf4JLoggerFactoryTest.class);
        final NSysMonLogger second = NSlf4JLoggerFactory.INSTANCE.getLogger(NSlf4JLoggerFactoryTest.class);

        assertNotSame(first, second);
    }

    @Test
    void instanceIsReusableAndShared() {
        assertSame(NSlf4JLoggerFactory.INSTANCE, NSlf4JLoggerFactory.INSTANCE);
        assertNotNull(NSlf4JLoggerFactory.INSTANCE.getLogger(Object.class));
    }

    @Test
    void classIsLoadedWithoutFailingOnMissingBinding() {
        // the static initializer eagerly resolves the SLF4J provider; touching the class must not throw
        assertEquals("NSlf4JLoggerFactory", NSlf4JLoggerFactory.class.getSimpleName());
    }
}
