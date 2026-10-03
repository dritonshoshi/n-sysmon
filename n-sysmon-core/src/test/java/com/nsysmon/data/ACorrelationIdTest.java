package com.nsysmon.data;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ACorrelationIdTest {

    @Test
    void gettersReturnTheConstructorArguments() {
        final ACorrelationId id = new ACorrelationId("soa", "42", "7");

        assertEquals("soa", id.getQualifier());
        assertEquals("42", id.getId());
        assertEquals("7", id.getIdParent());
    }

    @Test
    void identicalValuesAreEqual() {
        assertEquals(new ACorrelationId("soa", "42", "7"), new ACorrelationId("soa", "42", "7"));
    }

    @Test
    void equalValuesHaveEqualHashCodes() {
        assertEquals(new ACorrelationId("soa", "42", "7").hashCode(), new ACorrelationId("soa", "42", "7").hashCode());
    }

    @Test
    void identityIsEnforced() {
        final ACorrelationId id = new ACorrelationId("soa", "42", "7");

        assertEquals(id, id);
    }

    @Test
    void aDifferentQualifierMakesIdsUnequal() {
        assertNotEquals(new ACorrelationId("soa", "42", "7"), new ACorrelationId("http", "42", "7"));
    }

    @Test
    void aDifferentIdMakesIdsUnequal() {
        assertNotEquals(new ACorrelationId("soa", "42", "7"), new ACorrelationId("soa", "43", "7"));
    }

    @Test
    void aDifferentParentMakesIdsUnequal() {
        assertNotEquals(new ACorrelationId("soa", "42", "7"), new ACorrelationId("soa", "42", "8"));
    }

    @Test
    void nullQualifierIsHandled() {
        assertEquals(new ACorrelationId(null, "42", "7"), new ACorrelationId(null, "42", "7"));
    }

    @Test
    void aNullQualifierDiffersFromAPopulatedOne() {
        assertNotEquals(new ACorrelationId(null, "42", "7"), new ACorrelationId("soa", "42", "7"));
    }

    @Test
    void aNullIdDiffersFromAPopulatedOne() {
        assertNotEquals(new ACorrelationId("soa", null, "7"), new ACorrelationId("soa", "42", "7"));
    }

    @Test
    void aNullParentDiffersFromAPopulatedOne() {
        assertNotEquals(new ACorrelationId("soa", "42", null), new ACorrelationId("soa", "42", "7"));
    }

    @Test
    void allNullValuesAreEqual() {
        assertEquals(new ACorrelationId(null, null, null), new ACorrelationId(null, null, null));
    }

    @Test
    void allNullValuesHashToTheSameValue() {
        assertEquals(new ACorrelationId(null, null, null).hashCode(), new ACorrelationId(null, null, null).hashCode());
    }

    @Test
    void isNotEqualToNull() {
        assertFalse(new ACorrelationId("soa", "42", "7").equals(null));
    }

    @Test
    void isNotEqualToAnObjectOfAnotherType() {
        assertFalse(new ACorrelationId("soa", "42", "7").equals("soa-42-7"));
    }

    @Test
    void usableAsASetElement() {
        final Set<ACorrelationId> ids = new HashSet<>();

        ids.add(new ACorrelationId("soa", "42", "7"));
        ids.add(new ACorrelationId("soa", "42", "7"));

        assertEquals(1, ids.size());
        assertTrue(ids.contains(new ACorrelationId("soa", "42", "7")));
    }

    @Test
    void toStringContainsAllThreeParts() {
        final String text = new ACorrelationId("soa", "42", "7").toString();

        assertTrue(text.contains("qualifier='soa'"), text);
        assertTrue(text.contains("id='42'"), text);
        assertTrue(text.contains("idParent='7'"), text);
    }
}