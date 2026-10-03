package com.nsysmon.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AHierarchicalDataTest {

    private static AHierarchicalData data(String identifier) {
        return new AHierarchicalData(true, 100L, 200L, identifier, Map.of(), List.of(), false);
    }

    @Test
    void gettersReturnTheConstructorArguments() {
        final Map<String, String> parameters = Map.of("a", "b");
        final AHierarchicalData node = new AHierarchicalData(false, 100L, 200L, "id", parameters, List.of(), true);

        assertFalse(node.isSerial());
        assertEquals(100L, node.getStartTimeMillis());
        assertEquals(200L, node.getDurationNanos());
        assertEquals("id", node.getIdentifier());
        assertEquals(parameters, node.getParameters());
        assertTrue(node.isWasKilled());
    }

    @Test
    void wasKilledDefaultsToFalse() {
        assertFalse(data("id").isWasKilled());
    }

    @Test
    void emptyParametersAreExposedAsAnEmptyMap() {
        assertTrue(data("id").getParameters().isEmpty());
    }

    @Test
    void parametersAreCopiedDefensively() {
        final Map<String, String> source = new HashMap<>();
        source.put("a", "b");
        final AHierarchicalData node = new AHierarchicalData(true, 0, 0, "id", source, List.of(), false);

        source.put("c", "d");

        assertEquals(Map.of("a", "b"), node.getParameters());
    }

    @Test
    void parametersAreExposedUnmodifiable() {
        final AHierarchicalData node = new AHierarchicalData(true, 0, 0, "id", Map.of("a", "b"), List.of(), false);

        assertThrows(UnsupportedOperationException.class, () -> node.getParameters().put("c", "d"));
    }

    @Test
    void childrenAreExposedUnmodifiable() {
        final AHierarchicalData node = new AHierarchicalData(true, 0, 0, "id", Map.of(), List.of(data("child")), false);

        assertThrows(UnsupportedOperationException.class, () -> node.getChildren().add(data("other")));
    }

    @Test
    void emptyChildrenAreExposedAsAnEmptyList() {
        assertTrue(data("id").getChildren().isEmpty());
    }

    @Test
    void childrenAreNotCopiedDefensively() {
        // the implementation deliberately keeps the caller's list to avoid per-measurement allocations
        final List<AHierarchicalData> children = new ArrayList<>();
        final AHierarchicalData node = new AHierarchicalData(true, 0, 0, "id", Map.of(), children, false);

        children.add(data("late"));

        assertEquals(1, node.getChildren().size());
    }

    @Test
    void identifierIsInterned() {
        assertSame(new String("identifier").intern(), data("identifier").getIdentifier());
    }

    @Test
    void toStringMentionsIdentifierAndKilledFlag() {
        final String text = data("the-id").toString();

        assertTrue(text.contains("the-id"), text);
        assertTrue(text.contains("false"), text);
    }
}