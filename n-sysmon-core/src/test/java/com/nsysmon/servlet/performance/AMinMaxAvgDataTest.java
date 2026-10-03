package com.nsysmon.servlet.performance;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AMinMaxAvgDataTest {

    @Test void initialisesAllStatisticsFromASingleDataPoint() {
        final AMinMaxAvgData data = new AMinMaxAvgData(true, 500L);

        assertTrue(data.isSerial());
        assertEquals(1, data.getTotalNumInContext());
        assertEquals(500L, data.getMinNanos());
        assertEquals(500L, data.getMaxNanos());
        assertEquals(500L, data.getAvgNanos());
        assertEquals(500L, data.getTotalNanos());
        assertTrue(data.getChildren().isEmpty());
    }

    @Test void countsTheAdditionalDataPoint() {
        final AMinMaxAvgData data = new AMinMaxAvgData(true, 500L).withDataPoint(true, 100L);

        assertEquals(2, data.getTotalNumInContext());
    }

    @Test void keepsTheSmallestDurationAsMinimum() {
        final AMinMaxAvgData data = new AMinMaxAvgData(true, 500L).withDataPoint(true, 100L);

        assertEquals(100L, data.getMinNanos());
    }

    @Test void keepsTheLargestDurationAsMaximum() {
        final AMinMaxAvgData data = new AMinMaxAvgData(true, 100L).withDataPoint(true, 500L);

        assertEquals(500L, data.getMaxNanos());
    }

    @Test void computesTheWeightedAverage() {
        final AMinMaxAvgData data = new AMinMaxAvgData(true, 100L).withDataPoint(true, 300L);

        assertEquals(200L, data.getAvgNanos());
    }

    @Test void sumsUpTheDurations() {
        final AMinMaxAvgData data = new AMinMaxAvgData(true, 100L)
                .withDataPoint(true, 300L).withDataPoint(true, 800L);

        assertEquals(1_200L, data.getTotalNanos());
        assertEquals(3, data.getTotalNumInContext());
    }

    @Test void ignoresDataPointsWithADifferentParallelFlag() {
        final AMinMaxAvgData original = new AMinMaxAvgData(true, 100L);

        final AMinMaxAvgData result = original.withDataPoint(false, 9_999L);

        assertSame(original, result);
        assertEquals(100L, result.getTotalNanos());
    }

    @Test void sharesTheChildrenWithAllDerivedInstances() {
        final AMinMaxAvgData original = new AMinMaxAvgData(true, 100L);

        final AMinMaxAvgData derived = original.withDataPoint(true, 300L);

        assertSame(original.getChildren(), derived.getChildren());
    }

    @Test void theFullConstructorTakesOverAllValues() {
        final Map<String, AMinMaxAvgData> children = new HashMap<>();
        children.put("child", new AMinMaxAvgData(true, 1L));

        final AMinMaxAvgData data = new AMinMaxAvgData(false, 7, 1L, 9L, 5L, 40L, children);

        assertFalse(data.isSerial());
        assertEquals(7, data.getTotalNumInContext());
        assertEquals(1L, data.getMinNanos());
        assertEquals(9L, data.getMaxNanos());
        assertEquals(5L, data.getAvgNanos());
        assertEquals(40L, data.getTotalNanos());
        assertSame(children, data.getChildren());
    }
}
