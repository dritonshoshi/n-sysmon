package com.nsysmon.measure.environment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AEnvironmentMeasurerTest {

    private List<AEnvironmentData> collected;
    private AEnvironmentMeasurer.EnvironmentCollector collector;

    @BeforeEach
    public void setUp() {
        collected = new ArrayList<>();
        collector = new AEnvironmentMeasurer.EnvironmentCollector(collected);
    }

    @Test
    public void addedValueIsCollectedUnderTheGivenKeySegments() {
        collector.add("some value", "root", "child", "leaf");

        assertEquals(1, collected.size());
        assertEquals(List.of("root", "child", "leaf"), collected.get(0).getName().asJavaUtilList());
        assertEquals("some value", collected.get(0).getValue());
    }

    @Test
    public void collectorAppendsToTheExistingCollection() {
        collector.add("first", "k1");
        collector.add("second", "k2");

        assertEquals(2, collected.size());
        assertEquals("first", collected.get(0).getValue());
        assertEquals("second", collected.get(1).getValue());
    }

    @Test
    public void valueCanBeAddedWithoutAnyKeySegment() {
        collector.add("nameless");

        assertEquals(1, collected.size());
        assertTrue(collected.get(0).getName().asJavaUtilList().isEmpty());
    }

    @Test
    public void theSuppliedListIsTheOneBeingWrittenTo() {
        assertEquals(collected, collector.data);
    }
}