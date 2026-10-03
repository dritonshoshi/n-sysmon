package com.nsysmon.servlet.timedscalars;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AScalarDataPointValueComparatorTest {

    private final AScalarDataPointValueComparator comparator = new AScalarDataPointValueComparator();

    private static AScalarDataPoint withValue(long value) {
        return new AScalarDataPoint(0, "name", value, 0);
    }

    @Test void ordersDataPointsByAscendingValue() {
        assertTrue(comparator.compare(withValue(1), withValue(2)) < 0);
    }

    @Test void ordersHigherValuesAfterLowerOnes() {
        assertTrue(comparator.compare(withValue(2), withValue(1)) > 0);
    }

    @Test void treatsEqualValuesAsEqual() {
        assertEquals(0, comparator.compare(withValue(7), withValue(7)));
    }

    @Test void ordersValuesWhoseDifferenceExceedsTheIntRange() {
        assertTrue(comparator.compare(withValue(0), withValue(5_000_000_000L)) < 0);
        assertTrue(comparator.compare(withValue(5_000_000_000L), withValue(0)) > 0);
    }
}