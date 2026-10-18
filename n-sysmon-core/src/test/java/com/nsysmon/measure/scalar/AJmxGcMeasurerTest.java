package com.nsysmon.measure.scalar;

import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.Notification;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author arno
 */
@ExtendWith(MockitoExtension.class)
public class AJmxGcMeasurerTest {

    private static final String GC_TYPE = "end of minor GC";

    @Mock
    private NSysMonApi sysMon;

    private AJmxGcMeasurer measurer;
    private AJmxGcMeasurer.GcNotificationListener listener;

    @BeforeEach
    public void setUp() {
        measurer = new AJmxGcMeasurer();
        listener = measurer.new GcNotificationListener();
        measurer.setNSysMon(sysMon);
    }

    @AfterEach
    public void tearDown() throws Exception {
        measurer.shutdown();
    }

    private AHierarchicalDataRoot capturedRoot() throws Exception {
        final ArgumentCaptor<AHierarchicalDataRoot> captor = ArgumentCaptor.forClass(AHierarchicalDataRoot.class);
        verify(sysMon).injectSyntheticMeasurement(captor.capture());
        return captor.getValue();
    }

    @Test
    public void gcNotificationIsInjectedAsSyntheticMeasurement() throws Exception {
        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(1000L, 1050L), null);

        assertEquals(AJmxGcMeasurer.IDENT_GC_TRACE_ROOT, capturedRoot().getRootNode().getIdentifier());
    }

    @Test
    public void gcTypeIsUsedAsTheOnlyChildNode() throws Exception {
        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(1000L, 1050L), null);

        assertEquals(1, capturedRoot().getRootNode().getChildren().size());
        assertEquals(GC_TYPE, capturedRoot().getRootNode().getChildren().get(0).getIdentifier());
    }

    @Test
    public void idCauseAndAlgorithmAreTakenFromTheNotification() throws Exception {
        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(1000L, 1050L), null);

        final Map<String, String> parameters = capturedRoot().getRootNode().getParameters();
        assertEquals("7", parameters.get(AJmxGcMeasurer.KEY_ID));
        assertEquals("G1 Evacuation Pause", parameters.get(AJmxGcMeasurer.KEY_CAUSE));
        assertEquals("G1 Young Generation", parameters.get(AJmxGcMeasurer.KEY_ALGORITHM));
    }

    @Test
    public void memoryUsageAfterAndDeltasAreTakenFromTheNotification() throws Exception {
        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(1000L, 1050L), null);

        final Map<String, String> parameters = capturedRoot().getRootNode().getParameters();
        assertEquals("40", parameters.get(AJmxGcMeasurer.getUsedAfterKey("Eden Space")));
        assertEquals("150", parameters.get(AJmxGcMeasurer.getCommittedAfterKey("Eden Space")));
        assertEquals("-60", parameters.get(AJmxGcMeasurer.getUsedDeltaKey("Eden Space")));
        assertEquals("-50", parameters.get(AJmxGcMeasurer.getCommittedDeltaKey("Eden Space")));
    }

    @Test
    public void durationIsConvertedToNanoseconds() throws Exception {
        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(1000L, 1050L), null);

        assertEquals(50_000L, capturedRoot().getRootNode().getDurationNanos());
    }

    @Test
    public void irrelevantMemoryKindsAreSkipped() throws Exception {
        final Map<String, MemoryUsage> after = new LinkedHashMap<>();
        after.put("Eden Space", new MemoryUsage(0, 100, 200, 300));
        after.put("Code Cache", new MemoryUsage(1, 7, 8, 9));
        final Notification notification = SyntheticGcNotifications.notification("G1 Young Generation", GC_TYPE, "cause",
                1L, 1000L, 1050L, SyntheticGcNotifications.memoryUsage("Eden Space", 0, 100, 200, 300), after);

        listener.handleNotification(notification, null);

        final Map<String, String> parameters = capturedRoot().getRootNode().getParameters();
        assertFalse(parameters.containsKey(AJmxGcMeasurer.getUsedAfterKey("Code Cache")));
        assertTrue(parameters.containsKey(AJmxGcMeasurer.getUsedAfterKey("Eden Space")));
    }

    @Test
    public void notificationsOfOtherTypesAreIgnored() throws Exception {
        listener.handleNotification(SyntheticGcNotifications.notification("some.other.notification", "G1 Young Generation",
                GC_TYPE, "cause", 1L, 1000L, 1050L, SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3),
                SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3)), null);

        verify(sysMon, never()).injectSyntheticMeasurement(any());
    }

    @Test
    public void noGcFrequencyIsReportedBeforeTwoNotificationsOfTheSameType() throws Exception {
        final Map<String, AScalarDataPoint> data = new HashMap<>();

        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(1000L, 1050L), null);
        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        assertTrue(data.isEmpty());
    }

    @Test
    public void gcFrequencyIsReportedAfterASecondNotificationOfTheSameType() throws Exception {
        final Map<String, AScalarDataPoint> data = new HashMap<>();
        final long now = System.currentTimeMillis();

        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(now - 120_000L, now - 60_000L), null);
        listener.handleNotification(SyntheticGcNotifications.minorGcNotification(now - 60_000L, now), null);
        measurer.contributeMeasurements(data, now, new HashMap<>());

        assertEquals(1, data.size());
        final AScalarDataPoint frequency = data.get("gc-per-minute:" + GC_TYPE);
        assertTrue(frequency.getValueRaw() > 0, "was " + frequency.getValueRaw());
        assertEquals(2, frequency.getNumFracDigits());
        assertEquals(now, frequency.getTimestamp());
    }

    @Test
    public void keysAreBuiltFromTheMemoryKindAndTheSuffix() {
        assertEquals("memgc:tenured:used-after", AJmxGcMeasurer.getUsedAfterKey("tenured"));
        assertEquals("memgc:tenured:committed-after", AJmxGcMeasurer.getCommittedAfterKey("tenured"));
        assertEquals("memgc:tenured:used-delta", AJmxGcMeasurer.getUsedDeltaKey("tenured"));
        assertEquals("memgc:tenured:committed-delta", AJmxGcMeasurer.getCommittedDeltaKey("tenured"));
    }

    @Test
    public void noAveragingTimeoutIsConfigured() {
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }

    @Test
    public void repeatedShutdownIsToleratedEvenThoughTheListenerIsAlreadyGone() throws Exception {
        measurer.shutdown();

        measurer.shutdown();
    }
}
