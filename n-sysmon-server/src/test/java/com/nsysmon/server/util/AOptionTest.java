package com.nsysmon.server.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AOptionTest {
    @Test void someIsDefinedAndReturnsItsValue() {
        final AOption<String> underTest = AOption.some("value");

        assertTrue(underTest.isDefined());
        assertFalse(underTest.isEmpty());
        assertEquals("value", underTest.get());
    }

    @Test void noneIsEmptyAndHasNoValue() {
        final AOption<String> underTest = AOption.none();

        assertFalse(underTest.isDefined());
        assertTrue(underTest.isEmpty());
        assertThrows(IllegalStateException.class, underTest::get);
    }

    @Test void noneIsASingleton() {
        assertSame(AOption.none(), AOption.none());
    }

    @Test void someAllowsNullValues() {
        final AOption<String> underTest = AOption.some(null);

        assertTrue(underTest.isDefined());
        assertEquals(null, underTest.get());
    }

    @Test void fromNullableWrapsNullAsNone() {
        assertTrue(AOption.fromNullable(null).isEmpty());
    }

    @Test void fromNullableWrapsNonNullAsSome() {
        final AOption<String> underTest = AOption.fromNullable("value");

        assertTrue(underTest.isDefined());
        assertEquals("value", underTest.get());
    }

    @Test void getOrElseReturnsValueOfSome() {
        assertEquals("value", AOption.some("value").getOrElse("default"));
    }

    @Test void getOrElseReturnsDefaultForNone() {
        assertEquals("default", AOption.<String>none().getOrElse("default"));
    }

    @Test void getOrElseReturnsNullDefaultForNone() {
        assertEquals(null, AOption.<String>none().getOrElse(null));
    }
}