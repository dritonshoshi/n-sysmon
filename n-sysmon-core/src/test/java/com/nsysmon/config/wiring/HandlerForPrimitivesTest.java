package com.nsysmon.config.wiring;

import com.ajjpj.afoundation.collection.immutable.AOption;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HandlerForPrimitivesTest extends ConfigPropsFileTestBase {

    @Test
    void stringValueIsReturnedVerbatim() {
        props.setProperty("k", "hello world");

        assertEquals("hello world", configFile().get("k", String.class));
    }

    @Test
    void valuesAreTrimmedBeforeConversion() {
        props.setProperty("k", "  42  ");

        assertEquals(42, configFile().get("k", Integer.TYPE));
    }

    @Test
    void boxedIntegerIsConverted() {
        props.setProperty("k", "17");

        assertEquals(Integer.valueOf(17), configFile().get("k", Integer.class));
    }

    @Test
    void longIsConverted() {
        props.setProperty("k", "9223372036854775807");

        assertEquals(Long.MAX_VALUE, configFile().get("k", Long.TYPE));
    }

    @Test
    void booleanIsConverted() {
        props.setProperty("t", "true");
        props.setProperty("f", "no");

        assertEquals(Boolean.TRUE, configFile().get("t", Boolean.TYPE));
        assertEquals(Boolean.FALSE, configFile().get("f", Boolean.class));
    }

    @Test
    void doubleAndFloatAreConverted() {
        props.setProperty("d", "1.5");
        props.setProperty("f", "0.25");

        assertEquals(1.5d, configFile().get("d", Double.TYPE));
        assertEquals(0.25f, configFile().get("f", Float.TYPE));
    }

    @Test
    void byteAndShortAreConverted() {
        props.setProperty("b", "7");
        props.setProperty("s", "-9");

        assertEquals((byte) 7, configFile().get("b", Byte.TYPE));
        assertEquals((short) -9, configFile().get("s", Short.class));
    }

    @Test
    void singleCharacterValueIsConvertedToCharacter() {
        props.setProperty("k", "x");

        assertEquals('x', configFile().get("k", Character.TYPE));
    }

    @Test
    void characterWithTooLongValueIsRejectedWithConfigError() {
        props.setProperty("k", "xy");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("k", Character.TYPE));

        assertEquals("config error @ k for type char: string value must have exactly one char to be coerced to character", exc.getMessage());
    }

    @Test
    void malformedIntegerPropagatesTheUnderlyingParseFailure() {
        props.setProperty("k", "not-a-number");

        final NumberFormatException exc = assertThrows(NumberFormatException.class, () -> configFile().get("k", Integer.TYPE));

        assertTrue(exc.getMessage().contains("not-a-number"), exc.getMessage());
    }

    @Test
    void missingValueWithoutDefaultIsRejectedWithConfigError() {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class, () -> configFile().get("absent", Integer.TYPE));

        assertEquals("config error @ absent for type int: Missing value, no default", exc.getMessage());
    }

    @Test
    void missingValueFallsBackToTheGivenDefault() {
        assertEquals(7, configFile().get("absent", AOption.some(7), Integer.TYPE));
    }

    @Test
    void defaultIsOnlyUsedWhenTheKeyIsAbsent() {
        props.setProperty("k", "3");

        assertEquals(3, configFile().get("k", AOption.some(7), Integer.TYPE));
    }

    @Test
    void defaultValueIsUsedAsIsWithoutConversion() {
        final Object raw = new Object();

        assertSame(raw, configFile().get("absent", AOption.some(raw), Object.class));
    }

    @Test
    void errorMessageMentionsConstructorParameterTypes() {
        props.setProperty("bean", "x");

        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> configFile().get("bean", IFixtureBean.class, String.class, Integer.TYPE));

        assertEquals("config error @ bean for type " + IFixtureBean.class.getName()
                + " <java.lang.String, int>: no class name specified", exc.getMessage());
    }

    @Test
    void nonPrimitiveTypeFallsThroughToTheBeanHandler() {
        props.setProperty("k", NoArgBean.class.getName());

        assertInstanceOf(NoArgBean.class, configFile().get("k", Object.class));
    }

    @Test
    void listOfPrimitivesIsParsed() {
        props.setProperty("k", "1, 2 ,3");

        assertEquals(Arrays.asList(1, 2, 3), configFile().getList("k", Integer.class));
    }

    @Test
    void emptyListValueYieldsEmptyList() {
        props.setProperty("k", "");

        assertTrue(configFile().getList("k", Integer.class).isEmpty());
    }

    @Test
    void missingListKeyFallsBackToEmptyList() {
        assertTrue(configFile().getList("absent", Integer.class).isEmpty());
    }

    @Test
    void listElementsAreResolvedByTheElementTypeHandler() {
        props.setProperty("k", "FIRST, SECOND");

        assertEquals(Arrays.asList(TestEnum.FIRST, TestEnum.SECOND), configFile().getList("k", TestEnum.class));
    }

    @Test
    void singleElementListIsParsed() {
        props.setProperty("k", "42");

        assertEquals(Arrays.asList(42), configFile().getList("k", Integer.class));
    }

    @Test
    void listOfBeansIsParsedElementWise() {
        props.setProperty("k", NoArgBean.class.getName());

        assertInstanceOf(NoArgBean.class, configFile().getList("k", IFixtureBean.class).get(0));
    }
}
