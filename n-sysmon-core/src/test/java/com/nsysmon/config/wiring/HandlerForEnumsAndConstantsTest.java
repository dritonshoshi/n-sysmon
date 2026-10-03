package com.nsysmon.config.wiring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@link HandlerForEnumsAndConstants}: enum constants and public static constants whose type
 * matches the configured type are resolved by name.
 */
class HandlerForEnumsAndConstantsTest extends ConfigPropsFileTestBase {

    @Test
    void enumConstantIsResolvedByName() {
        props.setProperty("k", "SECOND");

        assertEquals(TestEnum.SECOND, configFile().get("k", TestEnum.class));
    }

    @Test
    void unknownEnumConstantFallsThroughToTheBeanHandlerAndIsRejected() {
        props.setProperty("k", "THIRD");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", TestEnum.class));

        assertTrue(exc.getMessage().contains("no class name specified"), exc.getMessage());
    }

    @Test
    void publicStaticConstantOfTheRequestedTypeIsResolvedByName() {
        props.setProperty("k", "GOLD");

        assertSame(Token.GOLD, configFile().get("k", Token.class));
    }

    @Test
    void constantOfIncompatibleTypeIsNotResolved() {
        props.setProperty("k", "NOT_A_TOKEN");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", Token.class));

        assertTrue(exc.getMessage().contains("no class name specified"), exc.getMessage());
    }

    @Test
    void primitivesHandlerWinsOverConstantsForTheSameType() {
        props.setProperty("k", "MAGIC");

        // Integer is claimed by the primitives handler, so the constants handler is never consulted
        assertThrows(NumberFormatException.class, () -> configFile().get("k", Integer.class));
    }

    @Test
    void constantsAreNotResolvedForString() {
        props.setProperty("k", "NOT_A_TOKEN");

        // String values are taken verbatim by the primitives handler
        assertEquals("NOT_A_TOKEN", configFile().get("k", String.class));
    }
}