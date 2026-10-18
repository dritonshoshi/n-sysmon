package com.nsysmon.config.wiring;

import com.ajjpj.afoundation.collection.immutable.AOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Direct tests of the package-private {@link ConfigValueResolver}, which {@link ConfigPropsFile} delegates to.
 */
class ConfigValueResolverTest extends ConfigPropsFileTestBase {

    private static final Class<?>[] NO_PARAM_TYPES = new Class<?>[0];

    private ConfigValueResolver resolver(String key, Class<?> type) {
        return resolver(key, type, NO_PARAM_TYPES);
    }

    private ConfigValueResolver resolver(String key, Class<?> type, Class<?>[] paramTypes) {
        return new ConfigValueResolver(props, key, type, paramTypes);
    }

    @Test
    void rawValueIsTrimmed() {
        props.setProperty("k", "  hi  ");

        final AOption<String> raw = resolver("k", String.class).getRaw();

        assertTrue(raw.isDefined());
        assertEquals("hi", raw.get());
    }

    @Test
    void rawValueIsAbsentForAnUnknownKey() {
        assertTrue(resolver("absent", String.class).getRaw().isEmpty());
    }

    @Test
    void aliasMakesTheTargetKeysValueBeUsed() {
        props.setProperty("k.alias", "target");
        props.setProperty("target", "12");

        assertEquals(12, resolver("k", Integer.TYPE).get(AOption.none()));
    }

    @Test
    void aliasTargetTakesPrecedenceOverTheValueAtTheAliasedKey() {
        props.setProperty("k", "direct");
        props.setProperty("k.alias", "target");
        props.setProperty("target", "indirect");

        assertEquals("indirect", resolver("k", String.class).get(AOption.none()));
    }

    @Test
    void aliasIsIgnoredWhenTheTargetKeyDoesNotExist() {
        props.setProperty("k", "3");
        props.setProperty("k.alias", "target");

        assertEquals(3, resolver("k", Integer.TYPE).get(AOption.none()));
    }

    @Test
    void aliasChainIsNotFollowedBeyondOneLevel() {
        props.setProperty("k.alias", "second");
        props.setProperty("second.alias", "third");
        props.setProperty("third", "7");

        assertThrows(IllegalArgumentException.class, () -> resolver("k", Integer.TYPE).get(AOption.none()));
    }

    @Test
    void aliasTargetIsOnlyFollowedOneLevelPerResolver() {
        // the alias key itself is not consulted again by getRawWithoutAlias
        props.setProperty("k.alias", "second");
        props.setProperty("second.alias", "third");

        assertThrows(IllegalArgumentException.class, () -> resolver("k", String.class).get(AOption.none()));
    }

    @Test
    void childKeysAreBuiltByAppendingDotSeparatedSegments() {
        props.setProperty("a.b.c", "leaf");

        assertEquals("leaf", resolver("a", String.class).child(String.class, NO_PARAM_TYPES, "b", "c").get(AOption.none()));
    }

    @Test
    void childWithoutSegmentsResolvesTheSameKey() {
        props.setProperty("a", "leaf");

        assertEquals("leaf", resolver("a", String.class).child(String.class, NO_PARAM_TYPES).get(AOption.none()));
    }

    @Test
    void childWithoutSegmentsAlsoLooksUpTheAlias() {
        props.setProperty("a", "direct");
        props.setProperty("a.alias", "b");
        props.setProperty("b", "indirect");

        assertEquals("indirect", resolver("a", String.class).child(String.class, NO_PARAM_TYPES).get(AOption.none()));
    }

    @Test
    void configErrorWithoutCauseHasNoCauseAttached() {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> resolver("k", Integer.TYPE).throwConfigError("nope"));

        assertEquals("config error @ k for type int: nope", exc.getMessage());
        assertNull(exc.getCause());
    }

    @Test
    void configErrorWithCauseKeepsTheCause() {
        final Exception cause = new IllegalStateException("root");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> resolver("k", String.class).throwConfigError("nope", cause));

        assertEquals("config error @ k for type java.lang.String: nope", exc.getMessage());
        assertEquals(cause, exc.getCause());
    }

    @Test
    void configErrorRendersASingleParameterWithoutSeparator() {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> resolver("k", String.class, new Class<?>[] {Long.TYPE}).throwConfigError("nope"));

        assertEquals("config error @ k for type java.lang.String <long>: nope", exc.getMessage());
    }

    @Test
    void configErrorRendersSeveralParametersCommaSeparated() {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> resolver("k", String.class, new Class<?>[] {Long.TYPE, String.class}).throwConfigError("nope"));

        assertEquals("config error @ k for type java.lang.String <long, java.lang.String>: nope", exc.getMessage());
    }

    @Test
    void explicitRawValueIsConverted() {
        assertEquals(3, resolver("k", Integer.TYPE).get("  3 "));
    }

    @Test
    void explicitEmptyValueFallsThroughToTheBeanHandler() {
        // an empty string is not a class name, so it is treated as a child reference
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> resolver("k", NoArgBean.class).get(""));

        assertTrue(exc.getMessage().contains("no class name specified"), exc.getMessage());
    }

    @Test
    void handlerExceptionIsPropagatedUnwrapped() {
        props.setProperty("k", "nope");

        assertThrows(NumberFormatException.class, () -> resolver("k", Long.TYPE).get(AOption.none()));
    }

    @Test
    void defaultIsReturnedForAnAbsentKey() {
        assertEquals("dflt", resolver("absent", String.class).get(AOption.some("dflt")));
    }

    @Test
    void missingValueWithNoneDefaultFails() {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> resolver("absent", String.class).get(AOption.<String>none()));

        assertEquals("config error @ absent for type java.lang.String: Missing value, no default", exc.getMessage());
    }
}
