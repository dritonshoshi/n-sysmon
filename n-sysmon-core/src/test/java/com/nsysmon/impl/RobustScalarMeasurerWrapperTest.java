package com.nsysmon.impl;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import com.nsysmon.measure.scalar.ScalarMeasurerStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.nsysmon.impl.ImplTestSupport.ALWAYS_TIMES_OUT;
import static com.nsysmon.impl.ImplTestSupport.NEVER_TIMES_OUT;
import static com.nsysmon.impl.ImplTestSupport.read;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RobustScalarMeasurerWrapperTest {

    private static AScalarMeasurer measurerResponsibleForNothing() {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        when(measurer.isResponsibleForMeasurement(anyString())).thenReturn(false);
        return measurer;
    }

    private static AScalarMeasurer responsibleMeasurer() {
        final AScalarMeasurer measurer = mock(AScalarMeasurer.class);
        lenient().when(measurer.isResponsibleForMeasurement("known")).thenReturn(true);
        lenient().when(measurer.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        return measurer;
    }

    @Test
    void statusIsUnknownWhenTheMeasurerIsNotResponsible() throws Exception {
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(measurerResponsibleForNothing(), NEVER_TIMES_OUT, 3);

        assertEquals(ScalarMeasurerStatus.Status.UNKNOWN, wrapper.getStatus("unknown").getStatus());
    }

    @Test
    void statusIsRunningForAResponsibleEnabledMeasurer() throws Exception {
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(responsibleMeasurer(), NEVER_TIMES_OUT, 3);

        assertEquals(ScalarMeasurerStatus.Status.RUNNING, wrapper.getStatus("known").getStatus());
    }

    @Test
    void restartIsRefusedForAnUnrelatedKey() throws Exception {
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(measurerResponsibleForNothing(), NEVER_TIMES_OUT, 3);

        assertFalse(wrapper.restartIfResponsible("unknown"));
    }

    @Test
    void restartIsAcceptedForAResponsibleKey() throws Exception {
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(responsibleMeasurer(), NEVER_TIMES_OUT, 3);

        assertTrue(wrapper.restartIfResponsible("known"));
    }

    @Test
    void restartReEnablesADisabledMeasurer() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        doThrow(new IllegalStateException("boom")).when(inner).prepareMeasurements(any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 0);

        wrapper.prepareMeasurements(new HashMap<>());
        assertEquals(ScalarMeasurerStatus.Status.STOPPED, wrapper.getStatus("known").getStatus());

        wrapper.restartIfResponsible("known");

        assertEquals(ScalarMeasurerStatus.Status.RUNNING, wrapper.getStatus("known").getStatus());
    }

    @Test
    void prepareMeasurementsIsForwarded() throws Exception {
        final AScalarMeasurer inner = mock(AScalarMeasurer.class);
        final Map<String, Object> mementos = new HashMap<>();
        new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).prepareMeasurements(mementos);

        verify(inner).prepareMeasurements(mementos);
    }

    @Test
    void contributeMeasurementsIsForwarded() throws Exception {
        final AScalarMeasurer inner = mock(AScalarMeasurer.class);
        final Map<String, Object> mementos = new HashMap<>();
        final Map<String, AScalarDataPoint> data = new HashMap<>();

        new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).contributeMeasurements(data, 42L, mementos);

        verify(inner).contributeMeasurements(data, 42L, mementos);
    }

    @Test
    void exceptionFromPrepareIsSwallowedWhileToleranceRemains() throws Exception {
        final AScalarMeasurer inner = mock(AScalarMeasurer.class);
        doThrow(new IllegalStateException("boom")).when(inner).prepareMeasurements(any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 1);

        assertDoesNotThrow(() -> wrapper.prepareMeasurements(new HashMap<>()));
    }

    @Test
    void exceptionFromContributeIsSwallowedWhileToleranceRemains() throws Exception {
        final AScalarMeasurer inner = mock(AScalarMeasurer.class);
        doThrow(new IllegalStateException("boom")).when(inner).contributeMeasurements(any(), anyLong(), any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 1);

        assertDoesNotThrow(() -> wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>()));
    }

    @Test
    void measurerIsDisabledAfterMaxNumTimeoutsConsecutiveExceptions() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        doThrow(new IllegalStateException("boom")).when(inner).contributeMeasurements(any(), anyLong(), any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 1);

        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());
        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());

        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());
        verify(inner, times(2)).contributeMeasurements(any(), anyLong(), any());
    }

    @Test
    void aSuccessfulPrepareResetsTheExceptionCounter() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        doThrow(new IllegalStateException("boom")).when(inner).prepareMeasurements(any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 1);

        wrapper.prepareMeasurements(new HashMap<>());
        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());
        wrapper.prepareMeasurements(new HashMap<>());

        verify(inner, times(2)).prepareMeasurements(any());
    }

    @Test
    void aSingleTimeoutSwitchesToTheTimedOutStrategy() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        when(inner.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, ALWAYS_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void maxNumTimeoutsTimeoutsInARowDisableTheMeasurerPermanently() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        when(inner.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, ALWAYS_TIMES_OUT, 2);

        wrapper.prepareMeasurements(new HashMap<>());
        wrapper.prepareMeasurements(new HashMap<>());
        wrapper.prepareMeasurements(new HashMap<>());

        verify(inner, times(2)).prepareMeasurements(any());
        assertEquals(ScalarMeasurerStatus.Status.STOPPED, wrapper.getStatus("known").getStatus());
    }

    @Test
    void aFastPrepareAfterATimeoutRestoresTheInitialStrategy() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        when(inner.getTimeoutInMilliSeconds()).thenReturn(AOption.none());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, ALWAYS_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.prepareMeasurements(new HashMap<>());
        ImplTestSupport.write(wrapper, "timeoutNanos", NEVER_TIMES_OUT);
        wrapper.prepareMeasurements(new HashMap<>());

        assertSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void theMeasurersOwnTimeoutOverridesTheConfiguredOne() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        when(inner.getTimeoutInMilliSeconds()).thenReturn(AOption.some(0L));
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.prepareMeasurements(new HashMap<>());

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void shutdownIsForwarded() throws Exception {
        final AScalarMeasurer inner = mock(AScalarMeasurer.class);
        new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).shutdown();

        verify(inner).shutdown();
    }

    @Test
    void aFailingShutdownIsSwallowed() throws Exception {
        final AScalarMeasurer inner = mock(AScalarMeasurer.class);
        doThrow(new IllegalStateException("boom")).when(inner).shutdown();

        assertDoesNotThrow(() -> new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).shutdown());
    }

    @Test
    void aDisabledMeasurerIsNotCalledAnyMore() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        doThrow(new IllegalStateException("boom")).when(inner).prepareMeasurements(any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, NEVER_TIMES_OUT, 0);

        wrapper.prepareMeasurements(new HashMap<>());
        wrapper.prepareMeasurements(new HashMap<>());
        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());

        verify(inner, times(1)).prepareMeasurements(any());
        verify(inner, never()).contributeMeasurements(any(), anyLong(), any());
    }

    @Test
    void aPrepareExceptionWhileTimedOutDisablesTheMeasurer() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        doNothing().doThrow(new IllegalStateException("boom")).when(inner).prepareMeasurements(any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, ALWAYS_TIMES_OUT, 0);

        wrapper.prepareMeasurements(new HashMap<>()); // times out -> TIMED_OUT
        wrapper.prepareMeasurements(new HashMap<>()); // throws while already timed out
        wrapper.prepareMeasurements(new HashMap<>()); // disabled -> not forwarded

        verify(inner, times(2)).prepareMeasurements(any());
        assertEquals(ScalarMeasurerStatus.Status.STOPPED, wrapper.getStatus("known").getStatus());
    }

    @Test
    void aContributeExceptionWhileTimedOutDisablesTheMeasurer() throws Exception {
        final AScalarMeasurer inner = responsibleMeasurer();
        doThrow(new IllegalStateException("boom")).when(inner).contributeMeasurements(any(), anyLong(), any());
        final RobustScalarMeasurerWrapper wrapper = new RobustScalarMeasurerWrapper(inner, ALWAYS_TIMES_OUT, 1);

        wrapper.prepareMeasurements(new HashMap<>()); // times out -> TIMED_OUT
        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>()); // 1 exception, tolerated
        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>()); // 2nd exception -> disabled
        wrapper.contributeMeasurements(new HashMap<>(), 0L, new HashMap<>());

        verify(inner, times(2)).contributeMeasurements(any(), anyLong(), any());
    }
}
