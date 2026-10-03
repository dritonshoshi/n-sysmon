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
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author arno
 */
@ExtendWith(MockitoExtension.class)
public class AJmxGcAvgMeasurerTest {

    @Mock
    private NSysMonApi sysMon;

    private AJmxGcAvgMeasurer measurer;
    private AJmxGcAvgMeasurer.GcNotificationListener listener;
    private Map<String, AScalarDataPoint> data;

    @BeforeEach
    public void setUp() {
        measurer = new AJmxGcAvgMeasurer();
        listener = measurer.new GcNotificationListener();
        measurer.setNSysMon(sysMon);
        data = new HashMap<>();
    }

    @AfterEach
    public void tearDown() throws Exception {
        measurer.shutdown();
    }

    private void notify(Notification notification) {
        listener.handleNotification(notification, null);
    }

    @Test
    public void gcTypeIsShortenedToTheCollectionKind() throws Exception {
        notify(SyntheticGcNotifications.minorGcNotification(1000L, 1050L));
        notify(SyntheticGcNotifications.notification("G1 Old Generation", "end of major GC", "G1 Compaction Pause",
                8L, 1000L, 2000L, SyntheticGcNotifications.memoryUsage("G1 Old Gen", 0, 1, 2, 3),
                SyntheticGcNotifications.memoryUsage("G1 Old Gen", 0, 1, 2, 3)));

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.containsKey("gc-ival:minor:dur:cnt"), data.toString());
        assertTrue(data.containsKey("gc-ival:major:dur:cnt"), data.toString());
    }

    @Test
    public void gcNotificationIsInjectedAsSyntheticMeasurement() throws Exception {
        notify(SyntheticGcNotifications.minorGcNotification(1000L, 1050L));

        final ArgumentCaptor<AHierarchicalDataRoot> captor = ArgumentCaptor.forClass(AHierarchicalDataRoot.class);
        verify(sysMon).injectSyntheticMeasurement(captor.capture());

        assertEquals(AJmxGcAvgMeasurer.IDENT_GC_TRACE_ROOT, captor.getValue().getRootNode().getIdentifier());
        assertEquals("minor", captor.getValue().getRootNode().getChildren().get(0).getIdentifier());
    }

    @Test
    public void durationOfTheNotificationIsConvertedToNanoseconds() throws Exception {
        notify(SyntheticGcNotifications.minorGcNotification(1000L, 1050L));

        final ArgumentCaptor<AHierarchicalDataRoot> captor = ArgumentCaptor.forClass(AHierarchicalDataRoot.class);
        verify(sysMon).injectSyntheticMeasurement(captor.capture());

        assertEquals(50_000L, captor.getValue().getRootNode().getDurationNanos());
    }

    @Test
    public void notificationsOfOtherTypesAreIgnored() throws Exception {
        notify(SyntheticGcNotifications.notification("some.other.notification", "G1 Young Generation", "end of minor GC",
                "cause", 1L, 1000L, 1050L, SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3),
                SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3)));

        verify(sysMon, never()).injectSyntheticMeasurement(any());
    }

    @Test
    public void noGcDurationsAreReportedWithoutAnyNotification() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.keySet().stream().noneMatch(k -> k.startsWith("gc-ival:")), data.toString());
    }

    @Test
    public void minAvgMaxAndCountAreReportedPerGcType() throws Exception {
        notify(SyntheticGcNotifications.notification("G1 Young Generation", "end of minor GC", "cause",
                1L, 1000L, 1001L, SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3),
                SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3)));
        notify(SyntheticGcNotifications.notification("G1 Young Generation", "end of minor GC", "cause",
                2L, 2000L, 2003L, SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3),
                SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3)));
        notify(SyntheticGcNotifications.notification("G1 Young Generation", "end of minor GC", "cause",
                3L, 3000L, 3008L, SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3),
                SyntheticGcNotifications.memoryUsage("Eden Space", 0, 1, 2, 3)));

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(1000, data.get("gc-ival:minor:dur:min").getValueRaw());
        assertEquals(8000, data.get("gc-ival:minor:dur:max").getValueRaw());
        assertEquals((1000 + 3000 + 8000) / 3, data.get("gc-ival:minor:dur:avg").getValueRaw());
        assertEquals(3, data.get("gc-ival:minor:dur:cnt").getValueRaw());
    }

    @Test
    public void gcDurationsAreResetAfterEveryContribution() throws Exception {
        notify(SyntheticGcNotifications.minorGcNotification(1000L, 1050L));
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());
        data.clear();

        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        assertTrue(data.keySet().stream().noneMatch(k -> k.startsWith("gc-ival:")), data.toString());
    }

    @Test
    public void memoryValuesAreTakenFromTheRuntime() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        final long total = data.get("mem-ival:total").getValueRaw();
        final long free = data.get("mem-ival:free").getValueRaw();
        assertTrue(total > 0);
        assertTrue(free > 0);
        assertTrue(data.get("mem-ival:used").getValueRaw() > 0);
        assertTrue(data.get("mem-ival:used").getValueRaw() <= total);
    }

    @Test
    public void fileDescriptorValuesAreTakenFromTheOs() throws Exception {
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertTrue(data.get("fs-ival:open").getValueRaw() >= 0);
        assertTrue(data.get("fs-ival:max").getValueRaw() > 0);
    }

    @Test
    public void allReportedValuesHaveNoFractionDigits() throws Exception {
        notify(SyntheticGcNotifications.minorGcNotification(1000L, 1050L));

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertFalse(data.isEmpty());
        assertTrue(data.values().stream().allMatch(d -> d.getNumFracDigits() == 0));
    }

    @Test
    public void allGcIntervalMeasurementsBelongToTheIntervalGroup() throws Exception {
        notify(SyntheticGcNotifications.minorGcNotification(1000L, 1050L));

        assertEquals("interval", measurer.getGroupnameOfMeasurement("gc-ival:minor:dur:min"));
        assertEquals("interval", measurer.getGroupnameOfMeasurement("mem-ival:total"));
        assertEquals("interval", measurer.getGroupnameOfMeasurement("fs-ival:open"));
        assertNull(measurer.getGroupnameOfMeasurement("unknown"));
    }

    @Test
    public void allGcIntervalMeasurementsAreDescribed() {
        assertEquals("free memory in reserved memory", measurer.getDescriptionOfMeasurement("mem-ival:free"));
        assertEquals("used memory in reserved memory", measurer.getDescriptionOfMeasurement("mem-ival:used"));
        assertEquals("memory the vm uses from system", measurer.getDescriptionOfMeasurement("mem-ival:total"));
        assertEquals("no of calls", measurer.getDescriptionOfMeasurement("gc-ival:minor:dur:cnt"));
        assertEquals("min duration", measurer.getDescriptionOfMeasurement("gc-ival:minor:dur:min"));
        assertEquals("max duration", measurer.getDescriptionOfMeasurement("gc-ival:minor:dur:max"));
        assertEquals("avg duration", measurer.getDescriptionOfMeasurement("gc-ival:minor:dur:avg"));
        assertEquals("min duration", measurer.getDescriptionOfMeasurement("gc-ival:major:dur:min"));
        assertEquals("max duration", measurer.getDescriptionOfMeasurement("gc-ival:major:dur:max"));
        assertEquals("avg duration", measurer.getDescriptionOfMeasurement("gc-ival:major:dur:avg"));
        assertEquals("no of calls", measurer.getDescriptionOfMeasurement("gc-ival:major:dur:cnt"));
        assertEquals("max allowed open files", measurer.getDescriptionOfMeasurement("fs-ival:max"));
        assertEquals("currently open files", measurer.getDescriptionOfMeasurement("fs-ival:open"));
    }

    @Test
    public void unknownMeasurementsAreNotDescribed() {
        assertNull(measurer.getDescriptionOfMeasurement("gc-ival:young:dur:min"));
        assertNull(measurer.getDescriptionOfMeasurement("something else"));
    }

    @Test
    public void measurerIsResponsibleForAllKnownMeasurements() {
        assertTrue(measurer.isResponsibleForMeasurement("mem-ival:free"));
        assertFalse(measurer.isResponsibleForMeasurement("gc-ival:young:dur:min"));
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