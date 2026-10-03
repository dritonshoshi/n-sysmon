package com.nsysmon.servlet.correlationflow;

import com.nsysmon.data.ACorrelationId;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ACorrelationIdComparatorTest {
    private static final ACorrelationIdComparator COMPARATOR = new ACorrelationIdComparator();

    private static Map.Entry<ACorrelationId, Set<ACorrelationId>> entry(String qualifier) {
        return new AbstractMap.SimpleEntry<>(new ACorrelationId(qualifier, "id-" + qualifier, null), Set.of());
    }

    @Test void sortsByQualifier() {
        assertTrue(COMPARATOR.compare(entry("a"), entry("b")) < 0);
        assertTrue(COMPARATOR.compare(entry("b"), entry("a")) > 0);
    }

    @Test void treatsEqualQualifiersAsEqual() {
        assertEquals(0, COMPARATOR.compare(entry("a"), entry("a")));
    }

    @Test void sortsNullEntriesLast() {
        assertEquals(1, COMPARATOR.compare(null, entry("a")));
    }

    @Test void treatsTwoNullEntriesAsEqual() {
        assertEquals(0, COMPARATOR.compare(null, null));
    }

    @Test void sortsEntriesWithoutKeyLast() {
        final Map.Entry<ACorrelationId, Set<ACorrelationId>> withoutKey = new AbstractMap.SimpleEntry<>(null, Set.of());

        assertEquals(1, COMPARATOR.compare(withoutKey, entry("a")));
    }

    @Test void sortsEntriesWithoutKeyFirstWhenTheyAreTheSecondArgument() {
        final Map.Entry<ACorrelationId, Set<ACorrelationId>> withoutKey = new AbstractMap.SimpleEntry<>(null, Set.of());

        assertEquals(-1, COMPARATOR.compare(entry("a"), withoutKey));
    }

    @Test void treatsTwoEntriesWithoutKeyAsEqual() {
        final Map.Entry<ACorrelationId, Set<ACorrelationId>> withoutKey = new AbstractMap.SimpleEntry<>(null, Set.of());

        assertEquals(0, COMPARATOR.compare(withoutKey, withoutKey));
    }

    @Test void sortsEntriesWithoutQualifierFirst() {
        final Map.Entry<ACorrelationId, Set<ACorrelationId>> withoutQualifier =
                new AbstractMap.SimpleEntry<>(new ACorrelationId(null, "id", null), Set.of());

        assertEquals(-1, COMPARATOR.compare(withoutQualifier, entry("a")));
        assertEquals(1, COMPARATOR.compare(entry("a"), withoutQualifier));
    }
}
