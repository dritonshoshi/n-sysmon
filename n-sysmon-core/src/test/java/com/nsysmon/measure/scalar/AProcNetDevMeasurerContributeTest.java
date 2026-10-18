package com.nsysmon.measure.scalar;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author arno
 */
public class AProcNetDevMeasurerContributeTest {

    private static final String KEY_MEMENTO = "net:";
    private static final String LOOPBACK = "lo";
    private static final long ONE_HOUR = 60L * 60L * 1000L;
    // with an interval of one hour, a millisecond of drift between the two reads of /proc/net/dev moves the result by
    // less than one unit
    private static final long TOLERANCE = 100L;

    private AProcNetDevMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new AProcNetDevMeasurer();
        data = new HashMap<>();
    }

    private static AProcNetDevMeasurer.Snapshot currentSnapshot() throws Exception {
        final Method method = AProcNetDevMeasurer.class.getDeclaredMethod("createSnapshot");
        method.setAccessible(true);
        return (AProcNetDevMeasurer.Snapshot) method.invoke(new AProcNetDevMeasurer());
    }

    private static void setTimestamp(AProcNetDevMeasurer.Snapshot snapshot, long timestamp) throws Exception {
        final Field field = AProcNetDevMeasurer.Snapshot.class.getDeclaredField("timestamp");
        field.setAccessible(true);
        field.setLong(snapshot, timestamp);
    }

    /**
     * A memento for all currently known interfaces, with the loopback counters reduced by exactly the given deltas,
     * as they would have been one hour ago.
     */
    private Map<String, Object> mementoOneHourAgo(long bytes, long packets, AProcNetDevMeasurer.Snapshot current)
            throws Exception {
        final AProcNetDevMeasurer.Snapshot previous = new AProcNetDevMeasurer.Snapshot();
        current.packetsReceived.forEach((iface, receivedPackets) -> previous.add(iface,
                current.bytesReceived.get(iface) - bytes,
                current.packetsReceived.get(iface) - packets,
                current.bytesSent.get(iface) - bytes,
                current.packetsSent.get(iface) - packets,
                current.collisions.get(iface)));
        setTimestamp(previous, current.timestamp - ONE_HOUR);

        return Map.of(KEY_MEMENTO, previous);
    }

    private Map<String, Object> mementoOneHourAgo(long bytes, long packets) throws Exception {
        final AProcNetDevMeasurer.Snapshot current = currentSnapshot();
        assumeTrue(current.bytesReceived.containsKey(LOOPBACK));
        return mementoOneHourAgo(bytes, packets, current);
    }

    @Test
    public void allInterfacesAreReportedWithFiveValuesEach() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoOneHourAgo(0, 0));

        final List<String> interfaces = data.keySet().stream()
                .map(k -> k.substring(KEY_MEMENTO.length()))
                .map(k -> k.substring(0, k.lastIndexOf(':')))
                .distinct().sorted().toList();
        assertFalse(interfaces.isEmpty(), data.toString());
        assertEquals(5 * interfaces.size(), data.size());
    }

    @Test
    public void theLoopbackByteCountersAreNormalizedToTheElapsedTime() throws Exception {
        final long syntheticBytes = 36_000_000L;
        final long syntheticPackets = 3_600_000L;

        final AProcNetDevMeasurer.Snapshot before = currentSnapshot();
        assumeTrue(before.bytesReceived.containsKey(LOOPBACK));
        measurer.contributeMeasurements(data, 1000L, mementoOneHourAgo(syntheticBytes, syntheticPackets, before));
        // contributeMeasurements re-reads /proc/net/dev, so real loopback traffic between the
        // snapshot above and that read lands in the delta. Bracket the call and scale the
        // expectation by the observed drift; loopback is the busiest interface on the box.
        final AProcNetDevMeasurer.Snapshot after = currentSnapshot();

        final double byteDrift = drift(before.bytesReceived.get(LOOPBACK), after.bytesReceived.get(LOOPBACK));
        final double packetDrift = drift(before.packetsReceived.get(LOOPBACK), after.packetsReceived.get(LOOPBACK));

        assertApproximates(100_000L, data.get(KEY_MEMENTO + LOOPBACK + ":received-bytes").getValueRaw(),
                syntheticBytes, byteDrift);
        assertApproximates(100_000L, data.get(KEY_MEMENTO + LOOPBACK + ":sent-bytes").getValueRaw(),
                syntheticBytes, byteDrift);
        assertApproximates(10_000L, data.get(KEY_MEMENTO + LOOPBACK + ":received-pkt").getValueRaw(),
                syntheticPackets, packetDrift);
        assertApproximates(10_000L, data.get(KEY_MEMENTO + LOOPBACK + ":sent-pkt").getValueRaw(),
                syntheticPackets, packetDrift);
    }

    private static double drift(Long before, Long after) {
        return before == null || after == null ? 0 : after - before;
    }

    @Test
    public void theLoopbackCollisionsAreReportedAsDelta() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoOneHourAgo(0, 0));

        final long collisions = data.get(KEY_MEMENTO + LOOPBACK + ":collisions").getValueRaw();
        assertTrue(collisions >= 0, "was " + collisions);
    }

    @Test
    public void allValuesAreReportedWithOneFractionDigit() throws Exception {
        measurer.contributeMeasurements(data, 1000L, mementoOneHourAgo(0, 0));

        assertTrue(data.values().stream().allMatch(d -> d.getNumFracDigits() == 1));
    }

    @Test
    public void theTimestampIsPassedThroughToAllDataPoints() throws Exception {
        measurer.contributeMeasurements(data, 4711L, mementoOneHourAgo(0, 0));

        assertTrue(data.values().stream().allMatch(d -> d.getTimestamp() == 4711L));
    }

    private static void assertApproximates(long expected, long actual) {
        assertTrue(Math.abs(expected - actual) <= TOLERANCE, "expected " + expected + " but was " + actual);
    }

    /**
     * The measurer divides the raw delta by the elapsed time, so a raw delta larger by
     * {@code drift} scales the result proportionally rather than adding to it.
     */
    private static void assertApproximates(long expected, long actual, long synthetic, double drift) {
        final double scaled = expected * (synthetic + drift) / synthetic;
        assertTrue(Math.abs(scaled - actual) <= TOLERANCE,
                "expected " + scaled + " but was " + actual + " (drift " + drift + ")");
    }

    @Test
    public void prepareMeasurementsStoresASnapshotOfAllInterfaces() throws Exception {
        assumeFalse(com.nsysmon.NSysMon.isWindows());
        assumeFalse(com.nsysmon.NSysMon.isMacOS());
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);

        final Object snapshot = mementos.get(KEY_MEMENTO);
        assertTrue(snapshot instanceof AProcNetDevMeasurer.Snapshot);
        assertFalse(((AProcNetDevMeasurer.Snapshot) snapshot).bytesReceived.isEmpty());
    }

    @Test
    public void linesWithoutAColonAreSkipped() {
        final AProcNetDevMeasurer.Snapshot snapshot = AProcNetDevMeasurer.createSnapshot(List.of(
                "some header line",
                "Inter-|   Receive  |  Transmit"));

        assertTrue(snapshot.bytesReceived.isEmpty());
    }

    @Test
    public void linesWithTooFewColumnsAreSkipped() {
        final AProcNetDevMeasurer.Snapshot snapshot = AProcNetDevMeasurer.createSnapshot(List.of(
                "  eth0: 1 2 3"));

        assertTrue(snapshot.bytesReceived.isEmpty());
    }

    @Test
    public void theInterfaceNameIsTrimmed() {
        final AProcNetDevMeasurer.Snapshot snapshot = AProcNetDevMeasurer.createSnapshot(List.of(
                "  eth0: 10 11 0 0 0 0 0 0 20 21 0 0 0 5 0 0"));

        assertEquals(List.of("eth0"), List.copyOf(snapshot.bytesReceived.keySet()));
        assertEquals(10, (long) snapshot.bytesReceived.get("eth0"));
        assertEquals(20, (long) snapshot.bytesSent.get("eth0"));
        assertEquals(5, (long) snapshot.collisions.get("eth0"));
    }

    @Test
    public void noAveragingTimeoutIsConfigured() throws Exception {
        measurer.shutdown();

        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}
