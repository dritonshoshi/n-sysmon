package com.nsysmon.servlet.timedscalars;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AScalarDataPointTimestampComparatorTest {

    private final AScalarDataPointTimestampComparator comparator = new AScalarDataPointTimestampComparator();

    private static AScalarDataPoint at(long timestamp) {
        return new AScalarDataPoint(timestamp, "name", 0, 0);
    }

    @Test void ordersDataPointsByAscendingTimestamp() {
        assertTrue(comparator.compare(at(1), at(2)) < 0);
    }

    @Test void ordersLaterDataPointsAfterEarlierOnes() {
        assertTrue(comparator.compare(at(2), at(1)) > 0);
    }

    @Test void treatsEqualTimestampsAsEqual() {
        assertEquals(0, comparator.compare(at(7), at(7)));
    }

    @Test void ordersNegativeAndPositiveTimestamps() {
        assertTrue(comparator.compare(at(-5), at(5)) < 0);
        assertEquals(0, comparator.compare(at(Long.MIN_VALUE), at(Long.MIN_VALUE)));
    }
}