package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author arno
 */
public class AProcSelfStatMeasurerTest {

    private static final long TEN_SECONDS = 10_000L;
    private static final long ONE_HOUR = 60L * 60L * 1000L;
    // the values are per thousandth of a second, so a millisecond of drift between the two reads of /proc/self/stat
    // shows up as value * millis / elapsedMillis. With an interval of one hour that is below one unit.
    private static final long TOLERANCE = 1_000L;

    private AProcSelfStatMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new AProcSelfStatMeasurer();
        data = new HashMap<>();
    }

    private static void setField(Object snapshot, String name, long value) throws Exception {
        final Field field = snapshot.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.setLong(snapshot, value);
    }

    private static long getField(Object snapshot, String name) throws Exception {
        final Field field = snapshot.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.getLong(snapshot);
    }

    /**
     * The jiffies are read from a static file, so the only way to get deterministic numbers into
     * contributeMeasurements is to hand-craft the memento that prepareMeasurements would have produced. The jiffies of
     * the real snapshot are used as a base, with a huge delta subtracted, so that even a jiffy or two ticking by
     * between the two reads stays within the tolerance.
     */
    private Map<String, Object> memento(long millisBefore, long userDelta, long kernelDelta) throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());
        assumeFalse(com.nsysmon.NSysMon.isMacOS());

        final Map<String, Object> mementos = new HashMap<>();
        measurer.prepareMeasurements(mementos);
        final Object snapshot = mementos.get(AProcSelfStatMeasurer.KEY_MEMENTO);
        setField(snapshot, "timestamp", getField(snapshot, "timestamp") - millisBefore);
        setField(snapshot, "userJiffies", getField(snapshot, "userJiffies") - userDelta);
        setField(snapshot, "kernelJiffies", getField(snapshot, "kernelJiffies") - kernelDelta);

        return Map.of(AProcSelfStatMeasurer.KEY_MEMENTO, snapshot);
    }

    @Test
    public void prepareMeasurementsStoresASnapshot() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());
        assumeFalse(com.nsysmon.NSysMon.isMacOS());
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);

        assertNotNull(mementos.get(AProcSelfStatMeasurer.KEY_MEMENTO));
    }

    @Test
    public void userAndKernelJiffiesAreReportedPerSecond() throws Exception {
        measurer.contributeMeasurements(data, 1000L, memento(ONE_HOUR, 7_200_000_000L, 14_400_000_000L));

        final long user = data.get(AProcSelfStatMeasurer.KEY_SELF_USER).getValueRaw();
        final long kernel = data.get(AProcSelfStatMeasurer.KEY_SELF_KERNEL).getValueRaw();
        assertTrue(Math.abs(user - 20_000_000L) <= TOLERANCE, "user was " + user);
        assertTrue(Math.abs(kernel - 40_000_000L) <= TOLERANCE, "kernel was " + kernel);
    }

    @Test
    public void theValuesAreNormalizedToTheElapsedTime() throws Exception {
        measurer.contributeMeasurements(data, 1000L, memento(2 * ONE_HOUR, 7_200_000_000L, 7_200_000_000L));

        final long user = data.get(AProcSelfStatMeasurer.KEY_SELF_USER).getValueRaw();
        assertTrue(Math.abs(user - 10_000_000L) <= TOLERANCE, "user was " + user);
    }

    @Test
    public void bothValuesAreReportedWithOneFractionDigit() throws Exception {
        measurer.contributeMeasurements(data, 1000L, memento(TEN_SECONDS, 1_000_000L, 1_000_000L));

        assertEquals(2, data.size());
        assertEquals(1, data.get(AProcSelfStatMeasurer.KEY_SELF_USER).getNumFracDigits());
        assertEquals(1, data.get(AProcSelfStatMeasurer.KEY_SELF_KERNEL).getNumFracDigits());
    }

    @Test
    public void theTimestampIsPassedThroughToTheDataPoints() throws Exception {
        measurer.contributeMeasurements(data, 4711L, memento(TEN_SECONDS, 1_000_000L, 1_000_000L));

        assertTrue(data.values().stream().allMatch(d -> d.getTimestamp() == 4711L));
    }

    @Test
    public void theSelfJiffiesAreSharedWithTheCpuUtilizationMeasurer() {
        assertEquals("cpu:self-user", AProcSelfStatMeasurer.KEY_SELF_USER);
        assertEquals(ACpuUtilizationMeasurer.KEY_SELF_KERNEL, AProcSelfStatMeasurer.KEY_SELF_KERNEL);
    }

    @Test
    public void noAveragingTimeoutIsConfigured() throws Exception {
        measurer.shutdown();

        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }

    @Test
    public void theRealJiffiesOfThisProcessArePositive() throws Exception {
        assumeTrue(new File("/proc/self/stat").canRead());
        final Map<String, Object> mementos = new HashMap<>();
        measurer.prepareMeasurements(mementos);
        final Object snapshot = mementos.get(AProcSelfStatMeasurer.KEY_MEMENTO);

        assertTrue(getField(snapshot, "userJiffies") >= 0);
        assertTrue(getField(snapshot, "kernelJiffies") >= 0);
    }
}