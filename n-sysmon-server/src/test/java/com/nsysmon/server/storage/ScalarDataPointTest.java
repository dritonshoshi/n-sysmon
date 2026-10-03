package com.nsysmon.server.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScalarDataPointTest {
    @Test void exposesInstanceTimestampAndValue() {
        final ScalarDataPoint underTest = new ScalarDataPoint("instance-1", 1000L, 0.75);

        assertEquals("instance-1", underTest.instance);
        assertEquals(1000L, underTest.timestamp);
        assertEquals(0.75, underTest.value);
    }

    @Test void toStringMentionsInstanceTimestampAndValue() {
        assertEquals("ScalarDataPoint{instance='instance-1', timestamp=1000, value=0.75}",
                new ScalarDataPoint("instance-1", 1000L, 0.75).toString());
    }
}