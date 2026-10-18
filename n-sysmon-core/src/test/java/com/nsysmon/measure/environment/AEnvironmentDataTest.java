package com.nsysmon.measure.environment;

import com.ajjpj.afoundation.collection.immutable.AList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AEnvironmentDataTest {

    private static AEnvironmentData data(String value, String... name) {
        return new AEnvironmentData(AList.create(name), value);
    }

    @Test
    public void nameAndValueAreReturnedUnchanged() {
        final AEnvironmentData d = data("my value", "a", "b", "c");

        assertEquals(List.of("a", "b", "c"), d.getName().asJavaUtilList());
        assertEquals("my value", d.getValue());
    }

    @Test
    public void emptyNameIsAllowed() {
        final AEnvironmentData d = data("v");

        assertTrue(d.getName().asJavaUtilList().isEmpty());
        assertEquals("v", d.getValue());
    }

    @Test
    public void nullValueIsKeptAsIs() {
        assertNull(data(null, "k").getValue());
    }

    @Test
    public void everyInstanceGetsItsOwnUuid() {
        final AEnvironmentData d1 = data("v", "k");
        final AEnvironmentData d2 = data("v", "k");

        assertNotNull(d1.getUuid());
        assertNotNull(d2.getUuid());
        assertNotEquals(d1.getUuid(), d2.getUuid());
    }
}
