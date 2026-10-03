package com.nsysmon.impl;

import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.measure.environment.AEnvironmentMeasurer;
import com.nsysmon.measure.environment.AEnvironmentMeasurer.EnvironmentCollector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static com.nsysmon.impl.ImplTestSupport.ALWAYS_TIMES_OUT;
import static com.nsysmon.impl.ImplTestSupport.NEVER_TIMES_OUT;
import static com.nsysmon.impl.ImplTestSupport.read;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RobustEnvironmentMeasurerWrapperTest {

    private static EnvironmentCollector collector(List<AEnvironmentData> into) {
        return new EnvironmentCollector(into);
    }

    private static EnvironmentCollector collector() {
        return collector(new ArrayList<>());
    }

    /**
     * Fails on exactly one call (1-based), so the strategy transitions can be triggered deterministically.
     */
    private static class FailOnNthCall implements AEnvironmentMeasurer {
        private final int nthCall;
        private int calls;

        FailOnNthCall(int nthCall) {
            this.nthCall = nthCall;
        }

        @Override public void contributeMeasurements(EnvironmentCollector data) {
            calls += 1;
            if (calls == nthCall) {
                throw new IllegalStateException("boom");
            }
        }

        @Override public void shutdown() {
        }
    }

    @Test
    void measurementsAreForwardedToTheInnerMeasurer() throws Exception {
        final AEnvironmentMeasurer inner = mock(AEnvironmentMeasurer.class);
        final EnvironmentCollector collector = collector();
        new RobustEnvironmentMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).contributeMeasurements(collector);

        verify(inner).contributeMeasurements(collector);
    }

    @Test
    void aFastMeasurerKeepsTheInitialStrategy() throws Exception {
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(new FailOnNthCall(-1), NEVER_TIMES_OUT, 3);
        final Object strategy = read(wrapper, "strategy");

        wrapper.contributeMeasurements(collector());

        assertSame(strategy, read(wrapper, "strategy"));
    }

    @Test
    void dataCollectedByTheInnerMeasurerReachesTheCollector() throws Exception {
        final List<AEnvironmentData> data = new ArrayList<>();
        final AEnvironmentMeasurer inner = new FailOnNthCall(-1) {
            @Override public void contributeMeasurements(EnvironmentCollector c) {
                super.contributeMeasurements(c);
                c.add("value", "key");
            }
        };

        new RobustEnvironmentMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).contributeMeasurements(collector(data));

        assertEquals(1, data.size());
        assertEquals("value", data.get(0).getValue());
    }

    @Test
    void anExceptionDisablesTheMeasurerImmediately() throws Exception {
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(new FailOnNthCall(1), NEVER_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        assertDoesNotThrow(() -> wrapper.contributeMeasurements(collector()));

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void aDisabledMeasurerIsNotCalledAnyMore() throws Exception {
        final AEnvironmentMeasurer inner = mock(AEnvironmentMeasurer.class);
        doThrow(new IllegalStateException("boom")).when(inner).contributeMeasurements(any());
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(inner, NEVER_TIMES_OUT, 3);

        wrapper.contributeMeasurements(collector());
        wrapper.contributeMeasurements(collector());
        wrapper.contributeMeasurements(collector());

        verify(inner, times(1)).contributeMeasurements(any());
    }

    @Test
    void aSingleTimeoutSwitchesToTheTimedOutStrategy() {
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(new FailOnNthCall(-1), ALWAYS_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.contributeMeasurements(collector());

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void maxNumTimeoutsTimeoutsInARowDisableTheMeasurerPermanently() throws Exception {
        final AEnvironmentMeasurer inner = mock(AEnvironmentMeasurer.class);
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(inner, ALWAYS_TIMES_OUT, 1);

        wrapper.contributeMeasurements(collector());
        wrapper.contributeMeasurements(collector());
        wrapper.contributeMeasurements(collector());

        verify(inner, times(2)).contributeMeasurements(any());
    }

    @Test
    void aSecondTimeoutBelowTheMaxRestoresTheInitialStrategy() {
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(new FailOnNthCall(-1), ALWAYS_TIMES_OUT, 5);
        final Object enabled = read(wrapper, "strategy");

        wrapper.contributeMeasurements(collector());
        wrapper.contributeMeasurements(collector());

        assertSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void aFastCallWhileTimedOutLeavesTheTimedOutStrategyInPlace() {
        // NB: unlike RobustDataSinkWrapper, the environment measurer wrapper has no fast-path branch here,
        //     so it stays in TIMED_OUT until it either times out again or exhausts maxNumTimeouts
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(new FailOnNthCall(-1), ALWAYS_TIMES_OUT, 5);
        final Object enabled = read(wrapper, "strategy");

        wrapper.contributeMeasurements(collector());
        ImplTestSupport.write(wrapper, "timeoutNanos", NEVER_TIMES_OUT);
        wrapper.contributeMeasurements(collector());

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void anExceptionWhileTimedOutDisablesTheMeasurer() throws Exception {
        // the first call times out into TIMED_OUT, the second one throws
        final RobustEnvironmentMeasurerWrapper wrapper = new RobustEnvironmentMeasurerWrapper(new FailOnNthCall(2), ALWAYS_TIMES_OUT, 5);
        final Object enabled = read(wrapper, "strategy");

        wrapper.contributeMeasurements(collector());
        wrapper.contributeMeasurements(collector());

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void shutdownIsForwarded() throws Exception {
        final AEnvironmentMeasurer inner = mock(AEnvironmentMeasurer.class);
        new RobustEnvironmentMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).shutdown();

        verify(inner).shutdown();
    }

    @Test
    void aFailingShutdownIsSwallowed() throws Exception {
        final AEnvironmentMeasurer inner = mock(AEnvironmentMeasurer.class);
        doThrow(new IllegalStateException("boom")).when(inner).shutdown();

        assertDoesNotThrow(() -> new RobustEnvironmentMeasurerWrapper(inner, NEVER_TIMES_OUT, 3).shutdown());
    }
}