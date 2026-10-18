package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class AJmxMemMeasurerTest {

    private AJmxMemMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new AJmxMemMeasurer();
        data = new HashMap<>();
    }

    private static List<MemoryPoolMXBean> memoryPools() {
        return ManagementFactory.getMemoryPoolMXBeans();
    }

    @Test
    public void everyMemoryPoolIsReported() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertFalse(memoryPools().isEmpty());
        for (MemoryPoolMXBean pool : memoryPools()) {
            final String prefix = AJmxMemMeasurer.PREFIX_MEM + pool.getName();
            assertTrue(data.containsKey(prefix + AJmxMemMeasurer.SUFFIX_COMMITTED), prefix);
            assertTrue(data.containsKey(prefix + AJmxMemMeasurer.SUFFIX_USED), prefix);
            assertTrue(data.containsKey(prefix + AJmxMemMeasurer.SUFFIX_INITIAL), prefix);
        }
    }

    @Test
    public void usedMemoryNeverExceedsTheCommittedMemory() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        for (MemoryPoolMXBean pool : memoryPools()) {
            final String prefix = AJmxMemMeasurer.PREFIX_MEM + pool.getName();
            final long used = data.get(prefix + AJmxMemMeasurer.SUFFIX_USED).getValueRaw();
            final long committed = data.get(prefix + AJmxMemMeasurer.SUFFIX_COMMITTED).getValueRaw();
            assertTrue(used <= committed, prefix + ": " + used + " > " + committed);
        }
    }

    @Test
    public void allMemoryDataPointsArePositive() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertFalse(data.isEmpty());
        assertTrue(data.values().stream().allMatch(d -> d.getValueRaw() >= 0));
        assertTrue(data.entrySet().stream().anyMatch(e -> e.getKey().endsWith(AJmxMemMeasurer.SUFFIX_COMMITTED)
                && e.getValue().getValueRaw() > 0), data.toString());
    }

    @Test
    public void unknownMaximumIsNotReportedAtAll() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        for (String key : data.keySet()) {
            assertTrue(key.endsWith(AJmxMemMeasurer.SUFFIX_MAX)
                    || key.endsWith(AJmxMemMeasurer.SUFFIX_USED)
                    || key.endsWith(AJmxMemMeasurer.SUFFIX_INITIAL)
                    || key.endsWith(AJmxMemMeasurer.SUFFIX_COMMITTED), key);
        }
    }

    @Test
    public void allDataPointsBelongToTheMemoryGroup() {
        assertEquals("Memory", measurer.getGroupnameOfMeasurement(AJmxMemMeasurer.PREFIX_MEM + "Eden Space:used"));
        assertNull(measurer.getGroupnameOfMeasurement("cpu:all-used"));
    }

    @Test
    public void nullMeasurementHasNoGroup() {
        assertNull(measurer.getGroupnameOfMeasurement(null));
    }

    @Test
    public void noMeasurementIsDescribedSoNothingIsItsResponsibility() {
        assertNull(measurer.getDescriptionOfMeasurement(AJmxMemMeasurer.PREFIX_MEM + "Eden Space:used"));
        assertFalse(measurer.isResponsibleForMeasurement(AJmxMemMeasurer.PREFIX_MEM + "Eden Space:used"));
    }

    @Test
    public void noAveragingTimeoutIsConfigured() {
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }

    @Test
    public void prepareMeasurementsIsANoop() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);
        measurer.shutdown();

        assertTrue(mementos.isEmpty());
        assertTrue(data.isEmpty());
    }

    @Test
    public void prefixesAndSuffixesAreStable() {
        assertEquals("mem:", AJmxMemMeasurer.PREFIX_MEM);
        assertEquals(List.of(":initial", ":committed", ":max", ":used"),
                List.of(AJmxMemMeasurer.SUFFIX_INITIAL, AJmxMemMeasurer.SUFFIX_COMMITTED,
                        AJmxMemMeasurer.SUFFIX_MAX, AJmxMemMeasurer.SUFFIX_USED));
    }
}
