package com.nsysmon.config.log;

import com.nsysmon.config.ADefaultConfigFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class NSysMonLoggerTest {

    private final NSysMonLogger fixed = new NStdOutLogger(NSysMonLoggerTest.class);

    @AfterEach
    void after() {
        // back to "not configured yet", so later tests re-derive it from the properties file
        ADefaultConfigFactory.setConfiguredLogger(null);
    }

    @Test
    void getReturnsLoggerFromTheConfiguredFactory() {
        ADefaultConfigFactory.setConfiguredLogger(context -> fixed);

        assertSame(fixed, NSysMonLogger.get(NSysMonLoggerTest.class));
    }

    @Test
    void getPassesTheRequestingClassToTheFactory() {
        ADefaultConfigFactory.setConfiguredLogger(FixedLoggerFactory.of(fixed));

        NSysMonLogger.get(NStdOutLogger.class);

        assertSame(NStdOutLogger.class, FixedLoggerFactory.lastContext);
    }

    @Test
    void getFallsBackToSlf4jWhenNothingIsConfigured() {
        assertSame(NSlf4JLoggerFactory.INSTANCE, ADefaultConfigFactory.getConfiguredLogger());
        assertInstanceOf(NSlf4JLogger.class, NSysMonLogger.get(NSysMonLoggerTest.class));
    }

    private static final class FixedLoggerFactory implements NSysMonLoggerFactory {
        private static Class<?> lastContext;

        private final NSysMonLogger logger;

        private FixedLoggerFactory(NSysMonLogger logger) {
            this.logger = logger;
        }

        static FixedLoggerFactory of(NSysMonLogger logger) {
            return new FixedLoggerFactory(logger);
        }

        @Override public NSysMonLogger getLogger(Class<?> context) {
            lastContext = context;
            return logger;
        }
    }
}