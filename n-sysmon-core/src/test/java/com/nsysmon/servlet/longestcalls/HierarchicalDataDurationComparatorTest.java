package com.nsysmon.servlet.longestcalls;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HierarchicalDataDurationComparatorTest {

    private static final HierarchicalDataDurationComparator COMPARATOR = new HierarchicalDataDurationComparator();

    private static AHierarchicalDataRoot root(long durationNanos) {
        return new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1L, durationNanos, "a", Map.of(), List.of(), false),
                Collections.emptyList(), Collections.emptyList());
    }

    private static AHierarchicalDataRoot rootWithoutNode() {
        return new AHierarchicalDataRoot(null, Collections.emptyList(), Collections.emptyList());
    }

    @Test void sortsLongerDurationsFirst() {
        assertTrue(COMPARATOR.compare(root(200L), root(100L)) < 0);
        assertTrue(COMPARATOR.compare(root(100L), root(200L)) > 0);
    }

    @Test void treatsEqualDurationsAsEqual() {
        assertEquals(0, COMPARATOR.compare(root(100L), root(100L)));
    }

    @Test void sortsNullFirst() {
        assertEquals(-1, COMPARATOR.compare(null, root(100L)));
    }

    @Test void sortsNullBeforeNull() {
        assertEquals(-1, COMPARATOR.compare(null, null));
    }

    @Test void sortsRootsWithoutNodeFirst() {
        assertEquals(-1, COMPARATOR.compare(rootWithoutNode(), root(100L)));
    }

    @Test void sortsRootsWithoutNodeLastWhenTheyAreTheSecondArgument() {
        assertEquals(1, COMPARATOR.compare(root(100L), rootWithoutNode()));
    }

    @Test void ordersAWholeDescendingSequence() {
        final SortedSet<AHierarchicalDataRoot> data = new TreeSet<>(COMPARATOR);
        data.add(root(100L));
        data.add(root(300L));
        data.add(root(200L));

        final List<Long> durations = new ArrayList<>();
        data.forEach(d -> durations.add(d.getRootNode().getDurationNanos()));

        assertEquals(List.of(300L, 200L, 100L), durations);
    }
}
