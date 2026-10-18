package com.nsysmon.config.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Captures {@code System.out} to verify the stdout fallback logger.
 */
class NStdOutLoggerTest {

    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    private PrintStream originalOut;

    @BeforeEach
    void before() {
        originalOut = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void after() {
        System.setOut(originalOut);
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    private String context() {
        return NStdOutLoggerTest.class.getName() + " ";
    }

    @Test
    void contextIsTheSimpleClassNameForShortNames() {
        final NStdOutLogger logger = new NStdOutLogger(NStdOutLoggerTest.class);

        logger.info("msg");

        assertTrue(output().contains(NStdOutLoggerTest.class.getName()), output());
    }

    @Test
    void contextIsTruncatedToTheLastFortyCharacters() {
        final NStdOutLogger logger = new NStdOutLogger(LongNestedName.class);

        logger.info("msg");

        final String context = LongNestedName.class.getName();
        assertTrue(context.length() > 40);
        assertTrue(output().contains(context.substring(context.length() - 40)), output());
        assertFalse(output().contains(context.substring(0, context.length() - 40)), output());
    }

    @Test
    void contextOfExactlyFortyCharactersIsKept() {
        final String name = ExactlyFortyChars.class.getName();
        assertTrue(name.length() >= 40);

        new NStdOutLogger(ExactlyFortyChars.class).info("msg");

        assertTrue(output().contains(name.substring(name.length() - 40)), output());
    }

    @Test
    void debugIsSuppressedByDefault() {
        final NStdOutLogger logger = new NStdOutLogger(NStdOutLoggerTest.class);

        logger.debug(() -> "should not appear");

        assertFalse(logger.isDebugEnabled);
        assertEquals("", output());
    }

    @Test
    void debugIsEvaluatedWhenExplicitlyEnabled() {
        final NStdOutLogger logger = new NStdOutLogger(NStdOutLoggerTest.class);
        logger.isDebugEnabled = true;

        logger.debug(() -> "debug msg");

        assertTrue(output().contains("DEBUG: "), output());
        assertTrue(output().contains("debug msg"), output());
    }

    @Test
    void infoIsLoggedWithInfoLevel() {
        new NStdOutLogger(NStdOutLoggerTest.class).info("hello");

        assertTrue(output().contains("INFO : "), output());
        assertTrue(output().endsWith("hello" + System.lineSeparator()), output());
    }

    @Test
    void warnIsLoggedWithWarnLevel() {
        new NStdOutLogger(NStdOutLoggerTest.class).warn("careful");

        assertTrue(output().contains("WARN : "), output());
        assertTrue(output().contains("careful"), output());
    }

    @Test
    void warnWithExceptionLogsMessageAndStackTrace() {
        new NStdOutLogger(NStdOutLoggerTest.class).warn("careful", new IllegalStateException("boom"));

        assertTrue(output().contains("WARN : "), output());
        assertTrue(output().contains("java.lang.IllegalStateException: boom"), output());
    }

    @Test
    void errorIsLoggedWithErrorLevel() {
        new NStdOutLogger(NStdOutLoggerTest.class).error("broken");

        assertTrue(output().contains("ERROR: "), output());
        assertTrue(output().contains("broken"), output());
    }

    @Test
    void errorWithMessageAndExceptionLogsMessageAndStackTrace() {
        new NStdOutLogger(NStdOutLoggerTest.class).error("broken", new IllegalStateException("boom"));

        assertTrue(output().contains("ERROR: " + context() + "broken"), output());
        assertTrue(output().contains("java.lang.IllegalStateException: boom"), output());
    }

    @Test
    void errorWithExceptionOnlyLogsGenericMessage() {
        new NStdOutLogger(NStdOutLoggerTest.class).error(new IllegalStateException("boom"));

        assertTrue(output().contains("ERROR: " + context() + "an exception occurred"), output());
        assertTrue(output().contains("java.lang.IllegalStateException: boom"), output());
    }

    @Test
    void everyLineCarriesTheCurrentThreadName() {
        new NStdOutLogger(NStdOutLoggerTest.class).info("hello");

        assertTrue(output().startsWith(Thread.currentThread().getName() + " "), output());
    }

    @SuppressWarnings("unused")
    static class LongNestedName {
    }

    @SuppressWarnings("unused")
    static class ExactlyFortyChars {
    }
}
