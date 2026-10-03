package com.nsysmon.config.wiring;

import com.ajjpj.afoundation.collection.immutable.AOption;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPropsFileTest extends ConfigPropsFileTestBase {

    @Test
    void getReturnsTheTypedValue() {
        props.setProperty("count", "12");

        assertEquals(12, configFile().get("count", Integer.TYPE));
    }

    @Test
    void getWithoutDefaultFailsForAnAbsentKey() {
        final IllegalArgumentException exc = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> configFile().get("count", Integer.TYPE));

        assertTrue(exc.getMessage().startsWith("config error @ count"), exc.getMessage());
    }

    @Test
    void getWithDefaultReturnsTheDefaultForAnAbsentKey() {
        assertEquals("fallback", configFile().get("absent", AOption.some("fallback"), String.class));
    }

    @Test
    void getListReturnsTypedElements() {
        props.setProperty("names", "a,b,c");

        final List<String> result = configFile().getList("names", String.class);

        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    void getListReturnsEmptyListForAnAbsentKey() {
        assertTrue(configFile().getList("absent", String.class).isEmpty());
    }

    @Test
    void getListRawSplitsAndTrimsEntries() {
        props.setProperty("menus", " Context , Trace ");

        assertEquals(Arrays.asList("Context", "Trace"), configFile().getListRaw("menus"));
    }

    @Test
    void getListRawReturnsEmptyListForAnAbsentKey() {
        assertTrue(configFile().getListRaw("absent").isEmpty());
    }

    @Test
    void getListRawReturnsEmptyListForABlankValue() {
        props.setProperty("menus", "   ");

        assertTrue(configFile().getListRaw("menus").isEmpty());
    }

    @Test
    void getListRawReturnsEmptyListForAnEmptyValue() {
        props.setProperty("menus", "");

        assertTrue(configFile().getListRaw("menus").isEmpty());
    }

    @Test
    void getListRawSkipsEmptyEntries() {
        props.setProperty("menus", "Context,,  ,Trace,");

        assertEquals(Arrays.asList("Context", "Trace"), configFile().getListRaw("menus"));
    }

    @Test
    void getListRawKeepsValuesContainingSpaces() {
        props.setProperty("names", "first entry, second entry");

        assertEquals(Arrays.asList("first entry", "second entry"), configFile().getListRaw("names"));
    }

    @Test
    void getListUsesTheSameKeyForItsElements() {
        props.setProperty("nums", "1,2");
        props.setProperty("nums.0", "ignored");
        props.setProperty("nums.1", "also-ignored");

        assertEquals(Arrays.asList(1, 2), configFile().getList("nums", Integer.class));
    }
}