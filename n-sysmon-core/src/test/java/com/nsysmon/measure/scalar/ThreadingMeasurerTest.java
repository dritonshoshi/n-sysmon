package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author arno
 */
public class ThreadingMeasurerTest {

    private static final String THREAD_COUNT = "Current Thread Count";
    private static final String DAEMON_THREAD_COUNT = "Daemon Thread Count";
    private static final String THREADS_CREATED = "Threads Created";

    private ThreadingMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new ThreadingMeasurer();
        data = new HashMap<>();
    }

    @Test
    public void threadCountsAreTakenFromTheThreadMxBean() throws Exception {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(3, data.size());
        assertTrue(data.get(THREAD_COUNT).getValueRaw() > 0);
        assertTrue(data.get(DAEMON_THREAD_COUNT).getValueRaw() >= 0);
        assertTrue(data.get(THREAD_COUNT).getValueRaw() >= data.get(DAEMON_THREAD_COUNT).getValueRaw());
        assertTrue(threadMXBean.getThreadCount() > 0);
    }

    @Test
    public void createdThreadsIsADeltaToThePreviousMeasurement() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());
        data.clear();
        final long beforeSecond = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();

        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        final long afterSecond = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();
        assertTrue(data.get(THREADS_CREATED).getValueRaw() >= 0);
        assertTrue(data.get(THREADS_CREATED).getValueRaw() <= afterSecond,
                data.get(THREADS_CREATED).getValueRaw() + " > " + afterSecond + " (" + beforeSecond + ")");
    }

    @Test
    public void timestampIsPassedThroughToEveryDataPoint() throws Exception {
        measurer.contributeMeasurements(data, 123456L, new HashMap<>());

        assertTrue(data.values().stream().allMatch(d -> d.getTimestamp() == 123456L));
    }

    @Test
    public void allThreadCountsAreReportedWithoutFractionDigits() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.values().stream().allMatch(d -> d.getNumFracDigits() == 0));
    }

    @Test
    public void allThreadCountsBelongToTheJvmGroup() {
        assertEquals("JVM", measurer.getGroupnameOfMeasurement(THREAD_COUNT));
        assertEquals("JVM", measurer.getGroupnameOfMeasurement(DAEMON_THREAD_COUNT));
        assertEquals("JVM", measurer.getGroupnameOfMeasurement(THREADS_CREATED));
        assertNull(measurer.getGroupnameOfMeasurement("something else"));
    }

    @Test
    public void allThreadCountsAreDescribed() {
        assertTrue(measurer.getDescriptionOfMeasurement(THREAD_COUNT).contains("threads"));
        assertTrue(measurer.getDescriptionOfMeasurement(DAEMON_THREAD_COUNT).contains("daemon"));
        assertTrue(measurer.getDescriptionOfMeasurement(THREADS_CREATED).contains("created"));
        assertNull(measurer.getDescriptionOfMeasurement("something else"));
    }

    @Test
    public void allThreadCountsAreConfigurable() {
        assertEquals(List.of(THREAD_COUNT, DAEMON_THREAD_COUNT, THREADS_CREATED), measurer.getConfigurationParameters());
    }

    @Test
    public void measurerIsResponsibleForAllItsThreadCounts() {
        assertTrue(measurer.isResponsibleForMeasurement(THREAD_COUNT));
        assertTrue(measurer.isResponsibleForMeasurement(DAEMON_THREAD_COUNT));
        assertTrue(measurer.isResponsibleForMeasurement(THREADS_CREATED));
    }

    @Test
    public void prepareMeasurementsAndShutdownAreNoops() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);
        measurer.shutdown();

        assertTrue(mementos.isEmpty());
    }

    @Test
    public void noAveragingTimeoutIsConfigured() {
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}
