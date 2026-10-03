package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author arno
 */
public class CpuTotalAndUserMeasurerTest {

    private static final String CPU_TOTAL = "CPU Total";
    private static final String CPU_USER = "CPU User";
    private static final String CPU_AVAILABLE = "cpu:available";
    private static final String CPU_SELF_KERNEL = "cpu:self-kernel";

    private CpuTotalAndUserMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new CpuTotalAndUserMeasurer();
        data = new HashMap<>();
    }

    @Test
    public void totalAndUserCpuUsageIsReportedAsPercentWithTwoDigits() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(2, data.size());
        assertEquals(2, data.get(CPU_TOTAL).getNumFracDigits());
        assertEquals(2, data.get(CPU_USER).getNumFracDigits());
    }

    @Test
    public void reportedValuesStayWithinTheValidRange() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        for (String key : Map.of(CPU_TOTAL, CPU_USER).keySet()) {
            final long value = data.get(key).getValueRaw();
            assertTrue(value >= 0, key + " was " + value);
            assertTrue(value <= 100_00, key + " was " + value);
        }
    }

    @Test
    public void theTimestampIsPassedThroughToTheDataPoints() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());

        measurer.contributeMeasurements(data, 4711L, new HashMap<>());

        assertTrue(data.values().stream().allMatch(d -> d.getTimestamp() == 4711L));
    }

    @Test
    public void allCpuMeasurementsBelongToTheSystemGroup() {
        assertEquals("System", measurer.getGroupnameOfMeasurement(CPU_TOTAL));
        assertEquals("System", measurer.getGroupnameOfMeasurement(CPU_USER));
        assertEquals("System", measurer.getGroupnameOfMeasurement(CPU_AVAILABLE));
        assertEquals("System", measurer.getGroupnameOfMeasurement(CPU_SELF_KERNEL));
        assertNull(measurer.getGroupnameOfMeasurement("something else"));
    }

    @Test
    public void allCpuMeasurementsAreDescribed() {
        assertTrue(measurer.getDescriptionOfMeasurement(CPU_TOTAL).contains("Total"));
        assertTrue(measurer.getDescriptionOfMeasurement(CPU_USER).contains("this user"));
        assertEquals("CPU available overall.", measurer.getDescriptionOfMeasurement(CPU_AVAILABLE));
        assertEquals("CPU usage of kernel-threads.", measurer.getDescriptionOfMeasurement(CPU_SELF_KERNEL));
        assertNull(measurer.getDescriptionOfMeasurement("something else"));
    }

    @Test
    public void measurerIsResponsibleForAllItsMeasurements() {
        assertTrue(measurer.isResponsibleForMeasurement(CPU_TOTAL));
        assertTrue(measurer.isResponsibleForMeasurement(CPU_SELF_KERNEL));
        assertFalse(measurer.isResponsibleForMeasurement("something else"));
    }

    @Test
    public void prepareMeasurementsAndShutdownAreNoops() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);
        measurer.shutdown();

        assertTrue(mementos.isEmpty());
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}
