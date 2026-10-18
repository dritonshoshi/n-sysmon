package com.nsysmon.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AHierarchicalDataRootTest {

    private static final ACorrelationId FLOW = new ACorrelationId("q", "1", "0");

    private static AHierarchicalData node() {
        return new AHierarchicalData(true, 1L, 2L, "root", Map.of(), List.of(), false);
    }

    @Test
    void rootNodeIsExposed() {
        final AHierarchicalData root = node();

        assertSame(root, new AHierarchicalDataRoot(root, List.of(), List.of()).getRootNode());
    }

    @Test
    void startedAndJoinedFlowsAreExposed() {
        final AHierarchicalDataRoot result = new AHierarchicalDataRoot(node(), List.of(FLOW), List.of());

        assertEquals(List.of(FLOW), result.getStartedFlows());
        assertTrue(result.getJoinedFlows().isEmpty());
    }

    @Test
    void killedDefaultsToFalse() {
        assertFalse(new AHierarchicalDataRoot(node(), List.of(), List.of()).isKilled());
    }

    @Test
    void killedCanBeSetExplicitly() {
        assertTrue(new AHierarchicalDataRoot(node(), List.of(), List.of(), true).isKilled());
    }

    @Test
    void flowCollectionsAreCopiedDefensively() {
        final Collection<ACorrelationId> started = new ArrayList<>();
        started.add(FLOW);
        final AHierarchicalDataRoot result = new AHierarchicalDataRoot(node(), started, List.of());

        started.clear();

        assertEquals(1, result.getStartedFlows().size());
    }

    @Test
    void everyRootGetsItsOwnUuid() {
        assertNotSame(
                new AHierarchicalDataRoot(node(), List.of(), List.of()).getUuid(),
                new AHierarchicalDataRoot(node(), List.of(), List.of()).getUuid());
    }

    @Test
    void toStringMentionsFlowsAndKillState() {
        final String text = new AHierarchicalDataRoot(node(), List.of(FLOW), List.of(), true).toString();

        assertTrue(text.contains("startedFlows=" + List.of(FLOW)), text);
        assertTrue(text.contains("joinedFlows=[]"), text);
        assertTrue(text.contains("wasKilled=true"), text);
    }
}
