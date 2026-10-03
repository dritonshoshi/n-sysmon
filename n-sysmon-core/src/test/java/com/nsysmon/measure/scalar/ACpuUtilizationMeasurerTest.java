package com.nsysmon.measure.scalar;

import com.nsysmon.NSysMon;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author arno
 */
public class ACpuUtilizationMeasurerTest {

    private ACpuUtilizationMeasurer measurer;
    private Map<String, AScalarDataPoint> data;
    private Map<String, Object> mementos;

    @BeforeEach
    public void setUp() {
        measurer = new ACpuUtilizationMeasurer();
        data = new HashMap<>();
        mementos = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Map<String, ACpuUtilizationMeasurer.Snapshot> currentSnapshot() throws Exception {
        final Method method = ACpuUtilizationMeasurer.class.getDeclaredMethod("createSnapshot");
        method.setAccessible(true);
        return (Map<String, ACpuUtilizationMeasurer.Snapshot>) method.invoke(measurer);
    }

    private static void setTimestamp(ACpuUtilizationMeasurer.Snapshot snapshot, long timestamp) throws Exception {
        final Field field = ACpuUtilizationMeasurer.Snapshot.class.getDeclaredField("timestamp");
        field.setAccessible(true);
        field.setLong(snapshot, timestamp);
    }

    /**
     * Builds the memento a call to prepareMeasurements would have created one second earlier.
     */
    private void prepareMementoOneSecondAgo() throws Exception {
        final ACpuUtilizationMeasurer.Snapshot current = currentSnapshot().get("cpu");
        final ACpuUtilizationMeasurer.Snapshot previous = new ACpuUtilizationMeasurer.Snapshot(0, current.stolen);
        setTimestamp(previous, current.timestamp - 1000);
        mementos.put(ACpuUtilizationMeasurer.KEY_MEMENTO, Map.of("cpu", previous));
    }

    private Map<String, Object> mementoWith(long idle, long stolen) throws Exception {
        final ACpuUtilizationMeasurer.Snapshot current = currentSnapshot().get("cpu");
        final ACpuUtilizationMeasurer.Snapshot previous = new ACpuUtilizationMeasurer.Snapshot(idle, stolen);
        setTimestamp(previous, current.timestamp - 1000);
        return Map.of(ACpuUtilizationMeasurer.KEY_MEMENTO, Map.of("cpu", previous));
    }

    @Test
    public void prepareMeasurementsStoresASnapshotOfAllCpus() throws Exception {
        assumeFalse(NSysMon.isWindows());
        assumeFalse(NSysMon.isMacOS());

        measurer.prepareMeasurements(mementos);

        final Object snapshot = mementos.get(ACpuUtilizationMeasurer.KEY_MEMENTO);
        assertTrue(snapshot instanceof Map);
        assertTrue(((Map<?, ?>) snapshot).size() >= 2);
        assertTrue(((Map<?, ?>) snapshot).containsKey("cpu"));
    }

    @Test
    public void availableAndUsedCpuCapacityAreReported() throws Exception {
        prepareMementoOneSecondAgo();

        measurer.contributeMeasurements(data, 1000L, mementos);

        assertTrue(data.containsKey(ACpuUtilizationMeasurer.KEY_AVAILABLE), data.toString());
        assertTrue(data.containsKey(ACpuUtilizationMeasurer.KEY_ALL_USED), data.toString());
        assertEquals(1, data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getNumFracDigits());
        assertEquals(1, data.get(ACpuUtilizationMeasurer.KEY_ALL_USED).getNumFracDigits());
    }

    @Test
    public void availableCapacityIsReportedPerCpu() throws Exception {
        prepareMementoOneSecondAgo();

        measurer.contributeMeasurements(data, 1000L, mementos);

        final long numCpus = currentSnapshot().size() - 1;
        assertEquals(numCpus * 1000, data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getValueRaw());
    }

    @Test
    public void theAvailableCapacityIsAMultipleOfTheNumberOfCpus() throws Exception {
        prepareMementoOneSecondAgo();

        measurer.contributeMeasurements(data, 1000L, mementos);

        final long available = data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getValueRaw();
        assertTrue(available > 0, "was " + available);
        assertEquals(0, available % 1000, "was " + available);
    }

    @Test
    public void nothingIsReportedIfTheMeasurementIntervalIsNotPositive() throws Exception {
        final ACpuUtilizationMeasurer.Snapshot inTheFuture = new ACpuUtilizationMeasurer.Snapshot(0, 0);
        setTimestamp(inTheFuture, System.currentTimeMillis() + 100_000);

        measurer.contributeMeasurements(data, 1000L,
                Map.of(ACpuUtilizationMeasurer.KEY_MEMENTO, Map.of("cpu", inTheFuture)));

        assertTrue(data.isEmpty(), data.toString());
    }

    @Test
    public void allCapacityIsUsedIfNeitherIdleNorStolenCyclesElapsed() throws Exception {
        final ACpuUtilizationMeasurer.Snapshot current = currentSnapshot().get("cpu");

        measurer.contributeMeasurements(data, 1000L, mementoWith(current.idle, current.stolen));

        final long used = data.get(ACpuUtilizationMeasurer.KEY_ALL_USED).getValueRaw();
        assertTrue(used > 0, "was " + used);
        assertTrue(used < 10_000_000, "was " + used);
    }

    @Test
    public void aFakeIdleDeltaOfOneMillionJiffiesAddsTenMillionToTheUsedCapacity() throws Exception {
        final ACpuUtilizationMeasurer.Snapshot current = currentSnapshot().get("cpu");

        measurer.contributeMeasurements(data, 1000L, mementoWith(current.idle + 1_000_000, current.stolen));

        final long used = data.get(ACpuUtilizationMeasurer.KEY_ALL_USED).getValueRaw();
        assertTrue(used >= 10_000_000, "was " + used);
        assertTrue(used < 10_500_000, "was " + used);
    }

    @Test
    public void theAvailableCapacityGrowsWithTheNumberOfStolenJiffies() throws Exception {
        final ACpuUtilizationMeasurer.Snapshot current = currentSnapshot().get("cpu");
        measurer.contributeMeasurements(data, 1000L, mementoWith(current.idle, current.stolen));
        final long withoutStolenJiffies = data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getValueRaw();
        data.clear();

        measurer.contributeMeasurements(data, 2000L, mementoWith(current.idle, current.stolen + 1000));

        final long withStolenJiffies = data.get(ACpuUtilizationMeasurer.KEY_AVAILABLE).getValueRaw();
        assertTrue(withStolenJiffies > withoutStolenJiffies, withoutStolenJiffies + " vs " + withStolenJiffies);
    }

    @Test
    public void cpuFrequenciesAreReportedPerCpu() throws Exception {
        assumeFalse(NSysMon.isWindows());
        assumeFalse(NSysMon.isMacOS());
        prepareMementoOneSecondAgo();

        measurer.contributeMeasurements(data, 1000L, mementos);

        final List<String> freqKeys = data.keySet().stream().filter(k -> k.startsWith(ACpuUtilizationMeasurer.KEY_PREFIX_MHZ)).toList();
        assertFalse(freqKeys.isEmpty(), data.toString());
        assertTrue(freqKeys.stream().allMatch(k -> data.get(k).getValueRaw() > 0), data.toString());
    }

    @Test
    public void availableAndUsedCapacityBelongToTheCpuGroup() throws Exception {
        assertEquals("CPU", measurer.getGroupnameOfMeasurement(ACpuUtilizationMeasurer.KEY_AVAILABLE));
        assertEquals("CPU", measurer.getGroupnameOfMeasurement(ACpuUtilizationMeasurer.KEY_ALL_USED));
        assertNull(measurer.getGroupnameOfMeasurement("something else"));
    }

    @Test
    public void allCpuMeasurementsAreDescribed() {
        assertTrue(measurer.getDescriptionOfMeasurement(ACpuUtilizationMeasurer.KEY_SELF_KERNEL).contains("NSysMon"));
        assertTrue(measurer.getDescriptionOfMeasurement(ACpuUtilizationMeasurer.KEY_AVAILABLE).contains("available"));
        assertTrue(measurer.getDescriptionOfMeasurement(ACpuUtilizationMeasurer.KEY_ALL_USED).contains("utilization"));
        assertEquals("CPU frequency in MHz", measurer.getDescriptionOfMeasurement(ACpuUtilizationMeasurer.KEY_PREFIX_MHZ + "0"));
        assertNull(measurer.getDescriptionOfMeasurement("something else"));
    }

    @Test
    public void theSelfKernelValueIsConfigurable() {
        assertEquals(List.of(ACpuUtilizationMeasurer.KEY_SELF_KERNEL, ACpuUtilizationMeasurer.KEY_ALL_USED),
                measurer.getConfigurationParameters());
    }

    @Test
    public void noAveragingTimeoutIsConfigured() throws Exception {
        measurer.shutdown();

        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }

    @Test
    public void measurerIsResponsibleForAllCpuMeasurements() {
        assertTrue(measurer.isResponsibleForMeasurement(ACpuUtilizationMeasurer.KEY_AVAILABLE));
        assertTrue(measurer.isResponsibleForMeasurement(ACpuUtilizationMeasurer.KEY_PREFIX_MHZ + "3"));
        assertFalse(measurer.isResponsibleForMeasurement("something else"));
    }
}