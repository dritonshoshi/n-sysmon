package com.nsysmon.measure.scalar;

import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.environment.impl.AFileSystemsEnvironmentMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author arno
 */
public class AProcDiskstatsMeasurerContributeTest {

    private static final String KEY_MEMENTO = "disk:";
    private static final long TEN_SECONDS = 10_000L;
    // two 'df' processes are forked between the memento and the snapshot the measurer reads, so the disk counters may
    // have advanced a little. 250 sectors are 128kB of slack.
    private static final long TOLERANCE = 250L;

    private AProcDiskstatsMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new AProcDiskstatsMeasurer();
        data = new HashMap<>();
    }

    private static AProcDiskstatsMeasurer.Snapshot currentSnapshot() throws Exception {
        final Method method = AProcDiskstatsMeasurer.class.getDeclaredMethod("createSnapshot");
        method.setAccessible(true);
        return (AProcDiskstatsMeasurer.Snapshot) method.invoke(new AProcDiskstatsMeasurer());
    }

    private static void setTimestamp(AProcDiskstatsMeasurer.Snapshot snapshot, long timestamp) throws Exception {
        final Field field = AProcDiskstatsMeasurer.Snapshot.class.getDeclaredField("timestamp");
        field.setAccessible(true);
        field.setLong(snapshot, timestamp);
    }

    /**
     * All devices of the current snapshot, with all counters reduced by exactly the given amount, as it would have
     * been ten seconds ago.
     */
    private Map<String, Object> mementoTenSecondsAgo(long sectorDelta) throws Exception {
        final AProcDiskstatsMeasurer.Snapshot current = currentSnapshot();
        final AProcDiskstatsMeasurer.Snapshot previous = new AProcDiskstatsMeasurer.Snapshot();
        current.sectorsRead.forEach((dev, value) -> {
            previous.sectorsRead.put(dev, value - sectorDelta);
            previous.sectorsWritten.put(dev, value - sectorDelta);
            previous.iosInProgress.put(dev, 0);
        });
        setTimestamp(previous, current.timestamp - TEN_SECONDS);
        return Map.of(KEY_MEMENTO, previous);
    }

    @Test
    public void noTrafficIsReportedForDevicesThatWereNotInTheFormerSnapshot() throws Exception {
        measurer.contributeMeasurements(data, 1000L, Map.of(KEY_MEMENTO, new AProcDiskstatsMeasurer.Snapshot()));

        assertTrue(data.keySet().stream().noneMatch(k -> k.endsWith(":read-sectors")), data.toString());
        assertTrue(data.keySet().stream().noneMatch(k -> k.endsWith(":written-sectors")), data.toString());
    }

    @Test
    public void readAndWrittenSectorsAreNormalizedToTheElapsedTime() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoTenSecondsAgo(1000));

        final List<String> readKeys = data.keySet().stream().filter(k -> k.endsWith(":read-sectors")).toList();
        assertFalse(readKeys.isEmpty(), data.toString());
        for (String key : readKeys) {
            assertTrue(Math.abs(data.get(key).getValueRaw() - 1000) <= TOLERANCE, key + " was " + data.get(key).getValueRaw());
        }
    }

    @Test
    public void megabytesAreReportedForEveryKnownDevice() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoTenSecondsAgo(1000));

        final List<String> readKeys = data.keySet().stream().filter(k -> k.endsWith(":read-mbytes")).toList();
        assertEquals(readKeys.size(), data.keySet().stream().filter(k -> k.endsWith(":written-mbytes")).count());
        assertTrue(readKeys.stream().allMatch(k -> data.get(k).getValueRaw() >= 0), data.toString());
    }

    @Test
    public void theNumberOfIosInProgressIsAnAbsoluteValue() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoTenSecondsAgo(1000));

        final List<String> keys = data.keySet().stream().filter(k -> k.endsWith(":ios-in-progress")).toList();
        assertFalse(keys.isEmpty(), data.toString());
        assertTrue(keys.stream().allMatch(k -> data.get(k).getValueRaw() >= 0));
        assertEquals(0, data.get(keys.get(0)).getNumFracDigits());
    }

    @Test
    public void theMountPointOfEveryDeviceIsReportedAsOne() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());
        final Map<String, String> mountPoints = AFileSystemsEnvironmentMeasurer.getMountPoints();

        measurer.contributeMeasurements(data, 1000L, Map.of(KEY_MEMENTO, new AProcDiskstatsMeasurer.Snapshot()));

        for (Map.Entry<String, String> e : mountPoints.entrySet()) {
            final String key = KEY_MEMENTO + e.getKey() + ":mountpoint:" + e.getValue();
            assertEquals(1, data.get(key).getValueRaw(), key);
        }
    }

    @Test
    public void theSizeOfEveryDeviceOfDfIsReported() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());
        final Map<String, String> mountPoints = AFileSystemsEnvironmentMeasurer.getMountPoints();

        measurer.contributeMeasurements(data, 1000L, Map.of(KEY_MEMENTO, new AProcDiskstatsMeasurer.Snapshot()));

        for (String dev : mountPoints.keySet()) {
            assertTrue(data.containsKey(KEY_MEMENTO + dev + ":sizeGB"), dev);
            assertTrue(data.get(KEY_MEMENTO + dev + ":usedGB").getValueRaw() >= 0, dev);
            assertTrue(data.get(KEY_MEMENTO + dev + ":availableGB").getValueRaw() >= 0, dev);
        }
    }

    @Test
    public void theFractionDigitsOfTheTrafficValuesAreDocumented() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoTenSecondsAgo(1000));

        final String key = data.keySet().stream().filter(k -> k.endsWith(":read-sectors")).findFirst().orElseThrow();
        assertEquals(1, data.get(key).getNumFracDigits());
        assertEquals(2, data.get(key.replace(":read-sectors", ":read-mbytes")).getNumFracDigits());
    }

    @Test
    public void anEmptyDiskstatsYieldsAnEmptySnapshot() {
        final AProcDiskstatsMeasurer.Snapshot snapshot = AProcDiskstatsMeasurer.createSnapshot(List.of());

        assertTrue(snapshot.sectorsRead.isEmpty());
        assertTrue(snapshot.sectorsWritten.isEmpty());
        assertTrue(snapshot.iosInProgress.isEmpty());
    }

    @Test
    public void theLastColumnIsNotRequiredToParseADevice() {
        final AProcDiskstatsMeasurer.Snapshot snapshot = AProcDiskstatsMeasurer.createSnapshot(
                List.of(" 8 0 sda 100 0 200 0 300 0 400 0 7 0 8 0"));

        assertEquals(200, (long) snapshot.sectorsRead.get("sda"));
        assertEquals(400, (long) snapshot.sectorsWritten.get("sda"));
        assertEquals(7, (int) snapshot.iosInProgress.get("sda"));
    }

    @Test
    public void aTruncatedLineIsRejected() {
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> AProcDiskstatsMeasurer.createSnapshot(List.of(" 8 0 sda 100 0 200")));
    }

    @Test
    public void prepareMeasurementsStoresASnapshotOfAllDevices() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());
        assumeFalse(com.nsysmon.NSysMon.isMacOS());
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);

        final Object snapshot = mementos.get(KEY_MEMENTO);
        assertTrue(snapshot instanceof AProcDiskstatsMeasurer.Snapshot);
        assertFalse(((AProcDiskstatsMeasurer.Snapshot) snapshot).sectorsRead.isEmpty());
    }

    @Test
    public void noAveragingTimeoutIsConfigured() throws Exception {
        measurer.shutdown();

        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}