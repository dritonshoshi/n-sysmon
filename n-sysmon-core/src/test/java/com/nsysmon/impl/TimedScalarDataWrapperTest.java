package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.ajjpj.afoundation.collection.immutable.AOption;
import com.ajjpj.afoundation.collection.mutable.ARingBuffer;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.data.AScalarMeasurementInterceptor;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimedScalarDataWrapperTest {

    private static final int MAX_TOTAL = 5;
    private static final int MAX_MONITORING = 3;

    private final TimedScalarDataWrapper wrapper = new TimedScalarDataWrapper(MAX_TOTAL, MAX_MONITORING);

    @AfterEach
    void after() {
        System.clearProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED);
    }

    private static AScalarDataPoint point(String name, long value) {
        return new AScalarDataPoint(1L, name, value, 0);
    }

    private static List<AScalarDataPoint> drain(ARingBuffer<AScalarDataPoint> buffer) {
        final List<AScalarDataPoint> result = new ArrayList<>();
        buffer.forEach(result::add);
        return result;
    }

    private static AScalarMeasurer measurerContributing(String name) throws Exception {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        lenient().when(measurer.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Map.class).put(name, point(name, 7));
            return null;
        }).when(measurer).contributeMeasurements(any(), anyLong(), any());
        return measurer;
    }

    @Test
    void measurementsAreExposedUnmodifiable() {
        wrapper.addMeasurement(point("a", 1));

        assertThrows(UnsupportedOperationException.class, () -> wrapper.getMeasurements().clear());
    }

    @Test
    void monitoringMeasurementsAreExposedUnmodifiable() {
        wrapper.addMeasurement(point("a", 1));

        assertThrows(UnsupportedOperationException.class, () -> wrapper.getMeasurementsForMonitoring().clear());
    }

    @Test
    void aDataPointIsStoredUnderItsName() {
        final AScalarDataPoint point = point("a", 1);
        wrapper.addMeasurement(point);

        final ARingBuffer<AScalarDataPoint> buffer = wrapper.getMeasurements().get("a");

        assertNotNull(buffer);
        assertEquals(List.of(point), drain(buffer));
    }

    @Test
    void aDataPointIsAlsoStoredForMonitoring() {
        final AScalarDataPoint point = point("a", 1);
        wrapper.addMeasurement(point);

        assertEquals(List.of(point), drain(wrapper.getMeasurementsForMonitoring().get("a")));
    }

    @Test
    void theSameNameReusesTheSameBuffer() {
        wrapper.addMeasurement(point("a", 1));
        wrapper.addMeasurement(point("a", 2));

        assertEquals(1, wrapper.getMeasurements().size());
        assertEquals(2, drain(wrapper.getMeasurements().get("a")).size());
    }

    @Test
    void theTotalBufferDropsTheOldestEntryBeyondItsCapacity() {
        for (int i = 0; i < MAX_TOTAL + 2; i++) {
            wrapper.addMeasurement(point("a", i));
        }

        final List<AScalarDataPoint> stored = drain(wrapper.getMeasurements().get("a"));

        assertEquals(MAX_TOTAL, stored.size());
        assertEquals(2L, stored.get(0).getValueRaw());
    }

    @Test
    void theMonitoringBufferIsSmallerThanTheTotalBuffer() {
        for (int i = 0; i < MAX_TOTAL + 2; i++) {
            wrapper.addMeasurement(point("a", i));
        }

        assertEquals(MAX_MONITORING, drain(wrapper.getMeasurementsForMonitoring().get("a")).size());
    }

    @Test
    void severalDataPointsInOneCallAreAllStored() {
        wrapper.addMeasurement(point("a", 1), point("b", 2));

        assertEquals(2, wrapper.getMeasurements().size());
    }

    @Test
    void withoutAnInterceptorNoDataPointIsPassedOn() {
        wrapper.addMeasurement(point("a", 1));

        assertEquals(1, wrapper.getMeasurements().size());
    }

    @Test
    void aRegisteredInterceptorReceivesEveryDataPoint() {
        final AScalarDataPoint first = point("a", 1);
        final AScalarDataPoint second = point("b", 2);
        final AScalarMeasurementInterceptor interceptor = mock(AScalarMeasurementInterceptor.class);
        wrapper.addTimedScalarMeasurementInterceptor(interceptor);

        wrapper.addMeasurement(first, second);

        verify(interceptor).addMeasurement(first, second);
    }

    @Test
    void theInterceptorAlsoSeesDataPointsProducedByARun() throws Exception {
        final AScalarMeasurementInterceptor interceptor = mock(AScalarMeasurementInterceptor.class);
        wrapper.addTimedScalarMeasurementInterceptor(interceptor);
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurerContributing("a"), 1, 1)));

        wrapper.run();

        verify(interceptor, times(1)).addMeasurement(any(AScalarDataPoint[].class));
    }

    @Test
    void runDoesNothingWhenGloballyDisabled() throws Exception {
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurerContributing("a"), 1, 1)));
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        wrapper.run();

        assertTrue(wrapper.getMeasurements().isEmpty());
    }

    @Test
    void runWithoutMeasurersAddsNothing() {
        wrapper.run();

        assertTrue(wrapper.getMeasurements().isEmpty());
    }

    @Test
    void runCollectsFromTheRegisteredMeasurers() throws Exception {
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurerContributing("a"), 1, 1)));

        wrapper.run();

        assertEquals(1, wrapper.getMeasurements().size());
        assertTrue(wrapper.getMeasurements().containsKey("a"));
    }

    @Test
    void refreshMeasurersPreparesEachOfThem() throws Exception {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        final Map<String, Object> mementos = new HashMap<>();

        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurer, 1, 1)));

        assertEquals(1, wrapper.getTimedScalarMeasurers().size());
        verify(measurer).prepareMeasurements(mementos);
    }

    @Test
    void refreshMeasurersReplacesThePreviousSet() throws Exception {
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurerContributing("a"), 1, 1)));

        wrapper.refreshMeasurers(AList.nil());

        assertEquals(0, wrapper.getTimedScalarMeasurers().size());
    }

    @Test
    void measurersAreEmptyByDefault() {
        assertEquals(0, wrapper.getTimedScalarMeasurers().size());
    }

    @Test
    void aSecondInterceptorReplacesTheFirst() {
        final AScalarMeasurementInterceptor first = mock(AScalarMeasurementInterceptor.class);
        final AScalarMeasurementInterceptor second = mock(AScalarMeasurementInterceptor.class);

        wrapper.addTimedScalarMeasurementInterceptor(first);
        wrapper.addTimedScalarMeasurementInterceptor(second);
        final AScalarDataPoint point = point("a", 1);
        wrapper.addMeasurement(point);

        verify(second).addMeasurement(point);
        verify(first, times(0)).addMeasurement(any(AScalarDataPoint[].class));
    }

    @Test
    void addMeasurementWithNoArgumentsIsANoOp() {
        wrapper.addMeasurement();

        assertEquals(0, wrapper.getMeasurements().size());
    }

    @Test
    void dataPointsOfDifferentNamesGetSeparateBuffers() {
        final List<String> names = new ArrayList<>();
        names.add("a");
        names.add("b");

        wrapper.addMeasurement(point("a", 1), point("b", 2));

        assertEquals(names, new ArrayList<>(wrapper.getMeasurements().keySet()));
    }

    @Test
    void theInterceptorReceivesAnArrayOfTheContributedDataPoints() throws Exception {
        final AScalarMeasurementInterceptor interceptor = mock(AScalarMeasurementInterceptor.class);
        wrapper.addTimedScalarMeasurementInterceptor(interceptor);
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurerContributing("a"), 1, 1)));

        wrapper.run();

        final ArgumentCaptor<AScalarDataPoint[]> captor = ArgumentCaptor.forClass(AScalarDataPoint[].class);
        verify(interceptor).addMeasurement(captor.capture());

        assertEquals(1, captor.getValue().length);
        assertEquals("a", captor.getValue()[0].getName());
    }

    @Test
    void aMeasurerThatThrowsDoesNotBreakTheRun() throws Exception {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        lenient().when(measurer.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        lenient().doThrow(new IllegalStateException("boom")).when(measurer).contributeMeasurements(any(), anyLong(), any());
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurer, 1, 1)));

        wrapper.run();

        assertTrue(wrapper.getMeasurements().isEmpty());
    }

    @Test
    void aMeasurerWithAnUnusedStubDoesNotBreakTheRun() throws Exception {
        final AScalarMeasurer measurer = measurerContributing("a");
        when(measurer.getTimeoutInMilliSeconds()).thenReturn(AOption.some(1_000L));
        wrapper.refreshMeasurers(AList.create(new RobustScalarMeasurerWrapper(measurer, 1, 1)));

        wrapper.run();

        assertEquals(1, wrapper.getMeasurements().size());
    }
}
