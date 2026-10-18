package com.nsysmon.config.log;

import com.ajjpj.afoundation.function.AFunction0NoThrow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies that {@link NSlf4JLogger} is a thin, faithful adapter on top of {@code org.slf4j.Logger}.
 */
class NSlf4JLoggerTest {

    private Logger slf4j;
    private NSlf4JLogger logger;

    @BeforeEach
    void before() throws Exception {
        slf4j = mock(Logger.class);
        logger = new NSlf4JLogger(NSlf4JLoggerTest.class);
        inject(slf4j);
    }

    /**
     * The delegate is looked up from the class name inside the constructor, so tests swap it through
     * the declared field to be able to verify the delegation.
     */
    private void inject(Logger replacement) throws Exception {
        final Field field = NSlf4JLogger.class.getDeclaredField("log");
        field.setAccessible(true);
        field.set(logger, replacement);
    }

    @Test
    void constructorLooksUpDelegateForContextClass() throws Exception {
        assertEquals(NSlf4JLoggerTest.class.getName(), delegateOf(new NSlf4JLogger(NSlf4JLoggerTest.class)).getName());
    }

    @Test
    void constructorLooksUpDelegateForAnotherContextClass() throws Exception {
        assertEquals(NStdOutLogger.class.getName(), delegateOf(new NSlf4JLogger(NStdOutLogger.class)).getName());
    }

    private Logger delegateOf(NSysMonLogger candidate) throws Exception {
        final Field field = NSlf4JLogger.class.getDeclaredField("log");
        field.setAccessible(true);
        return (Logger) field.get(candidate);
    }

    @Test
    void debugLogsMessageWhenDebugEnabled() {
        when(slf4j.isDebugEnabled()).thenReturn(true);

        logger.debug(() -> "the message");

        verify(slf4j).debug("the message");
    }

    @Test
    void debugDoesNotEvaluateSupplierWhenDebugDisabled() {
        when(slf4j.isDebugEnabled()).thenReturn(false);
        final AFunction0NoThrow<String> supplier = mock(AFunction0NoThrow.class);

        logger.debug(supplier);

        verify(supplier, never()).apply();
        verify(slf4j, never()).debug(anyString());
    }

    @Test
    void debugEvaluatesSupplierExactlyOnceWhenEnabled() {
        when(slf4j.isDebugEnabled()).thenReturn(true);
        final AFunction0NoThrow<String> supplier = mock(AFunction0NoThrow.class);
        when(supplier.apply()).thenReturn("lazy");

        logger.debug(supplier);

        verify(supplier, times(1)).apply();
        verify(slf4j).debug("lazy");
    }

    @Test
    void infoDelegatesToSlf4j() {
        logger.info("hello");

        verify(slf4j).info("hello");
    }

    @Test
    void warnDelegatesToSlf4j() {
        logger.warn("careful");

        verify(slf4j).warn("careful");
    }

    @Test
    void warnWithExceptionDelegatesBothArguments() {
        final Exception exc = new IllegalStateException("boom");

        logger.warn("careful", exc);

        verify(slf4j).warn("careful", exc);
    }

    @Test
    void errorDelegatesToSlf4j() {
        logger.error("broken");

        verify(slf4j).error("broken");
    }

    @Test
    void errorWithExceptionKeepsCauseInMessageAndAsThrowable() {
        final Exception exc = new IllegalStateException("boom");

        logger.error(exc);

        verify(slf4j).error("unexpected exception: java.lang.IllegalStateException: boom", exc);
    }

    @Test
    void errorWithMessageAndExceptionDelegatesBothArguments() {
        final Exception exc = new IllegalStateException("boom");

        logger.error("broken", exc);

        verify(slf4j).error("broken", exc);
    }

    @Test
    void levelsOtherThanDebugDoNotConsultDebugSwitch() {
        logger.info("i");
        logger.warn("w");
        logger.error("e");

        verify(slf4j, never()).isDebugEnabled();
    }

    @Test
    void everyLogMethodDelegatesToTheSameDelegate() {
        when(slf4j.isDebugEnabled()).thenReturn(true);
        final Exception exc = new IllegalStateException("boom");

        logger.debug(() -> "d");
        logger.info("i");
        logger.warn("w");
        logger.warn("w", exc);
        logger.error("e");
        logger.error(exc);
        logger.error("e", exc);

        verify(slf4j).debug("d");
        verify(slf4j).info("i");
        verify(slf4j).warn("w");
        verify(slf4j).warn("w", exc);
        verify(slf4j).error("e");
        verify(slf4j).error("unexpected exception: java.lang.IllegalStateException: boom", exc);
        verify(slf4j).error("e", exc);
    }
}
