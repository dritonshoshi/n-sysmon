package com.nsysmon.config.log;

import com.ajjpj.afoundation.function.AFunction0NoThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link NJavaUtilLogger} adapts to {@code java.util.logging}.
 */
class NJavaUtilLoggerTest {

    private Logger jul;
    private NJavaUtilLogger logger;

    @BeforeEach
    void before() throws Exception {
        jul = mock(Logger.class);
        logger = new NJavaUtilLogger(NJavaUtilLoggerTest.class);
        inject(jul);
    }

    private void inject(Logger replacement) throws Exception {
        final Field field = NJavaUtilLogger.class.getDeclaredField("log");
        field.setAccessible(true);
        field.set(logger, replacement);
    }

    @Test
    void constructorLooksUpDelegateForContextClass() throws Exception {
        assertEquals(NJavaUtilLoggerTest.class.getName(), delegateOf(new NJavaUtilLogger(NJavaUtilLoggerTest.class)).getName());
    }

    private Logger delegateOf(NSysMonLogger candidate) throws Exception {
        final Field field = NJavaUtilLogger.class.getDeclaredField("log");
        field.setAccessible(true);
        return (Logger) field.get(candidate);
    }

    @Test
    void debugLogsFineWhenFineIsLoggable() {
        when(jul.isLoggable(Level.FINE)).thenReturn(true);

        logger.debug(() -> "the message");

        verify(jul).fine("the message");
    }

    @Test
    void debugDoesNotEvaluateSupplierWhenFineIsNotLoggable() {
        when(jul.isLoggable(Level.FINE)).thenReturn(false);
        final AFunction0NoThrow<String> supplier = mock(AFunction0NoThrow.class);

        logger.debug(supplier);

        verify(supplier, never()).apply();
        verify(jul, never()).fine(anyString());
    }

    @Test
    void infoMapsToInfoLevel() {
        logger.info("hello");

        verify(jul).info("hello");
    }

    @Test
    void warnMapsToWarningLevel() {
        logger.warn("careful");

        verify(jul).warning("careful");
    }

    @Test
    void warnWithExceptionMapsToWarningLevelWithThrowable() {
        final Exception exc = new IllegalStateException("boom");

        logger.warn("careful", exc);

        verify(jul).log(Level.WARNING, "careful", exc);
    }

    @Test
    void errorMapsToSevereLevel() {
        logger.error("broken");

        verify(jul).severe("broken");
    }

    @Test
    void errorWithExceptionMapsToSevereLevelWithEmptyMessage() {
        final Exception exc = new IllegalStateException("boom");

        logger.error(exc);

        verify(jul).log(Level.SEVERE, "", exc);
    }

    @Test
    void errorWithMessageAndExceptionMapsToSevereLevel() {
        final Exception exc = new IllegalStateException("boom");

        logger.error("broken", exc);

        verify(jul).log(Level.SEVERE, "broken", exc);
    }

    @Test
    void levelsOtherThanDebugDoNotConsultFineSwitch() {
        logger.info("i");
        logger.warn("w");
        logger.error("e");

        verify(jul, never()).isLoggable(Level.FINE);
    }
}