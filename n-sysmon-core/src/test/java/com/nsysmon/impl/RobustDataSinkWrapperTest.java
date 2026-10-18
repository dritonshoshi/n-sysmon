package com.nsysmon.impl;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.datasink.ADataSink;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static com.nsysmon.impl.ImplTestSupport.ALWAYS_TIMES_OUT;
import static com.nsysmon.impl.ImplTestSupport.NEVER_TIMES_OUT;
import static com.nsysmon.impl.ImplTestSupport.read;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RobustDataSinkWrapperTest {

    private static ADataSink sink() {
        return mock(ADataSink.class);
    }

    /**
     * Fails on exactly one call (1-based), so the strategy transitions can be triggered deterministically.
     */
    private static final class FailOnNthStartedCall implements ADataSink {
        private final int nthCall;
        private int calls;

        private FailOnNthStartedCall(int nthCall) {
            this.nthCall = nthCall;
        }

        @Override public void onStartedHierarchicalMeasurement(String identifier) {
            calls += 1;
            if (calls == nthCall) {
                throw new IllegalStateException("boom");
            }
        }

        @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
        }

        @Override public void shutdown() {
        }
    }

    private static final class FailOnNthFinishedCall implements ADataSink {
        private final int nthCall;
        private int calls;

        private FailOnNthFinishedCall(int nthCall) {
            this.nthCall = nthCall;
        }

        @Override public void onStartedHierarchicalMeasurement(String identifier) {
        }

        @Override public void onFinishedHierarchicalMeasurement(AHierarchicalDataRoot data) {
            calls += 1;
            if (calls == nthCall) {
                throw new IllegalStateException("boom");
            }
        }

        @Override public void shutdown() {
        }
    }

    private static AHierarchicalDataRoot data() {
        final AHierarchicalData root = new AHierarchicalData(true, 0, 0, "id", Map.of(), List.of(), false);
        return new AHierarchicalDataRoot(root, List.of(), List.of());
    }

    @Test
    void startedMeasurementIsForwardedToTheInnerSink() {
        final ADataSink inner = sink();
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 3);

        wrapper.onStartedHierarchicalMeasurement("the-id");

        verify(inner).onStartedHierarchicalMeasurement("the-id");
    }

    @Test
    void finishedMeasurementIsForwardedToTheInnerSink() {
        final ADataSink inner = sink();
        final AHierarchicalDataRoot data = data();
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 3);

        wrapper.onFinishedHierarchicalMeasurement(data);

        verify(inner).onFinishedHierarchicalMeasurement(data);
    }

    @Test
    void fastSinkKeepsTheInitialStrategy() {
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(sink(), NEVER_TIMES_OUT, 3);
        final Object strategy = read(wrapper, "strategy");

        wrapper.onStartedHierarchicalMeasurement("a");
        wrapper.onFinishedHierarchicalMeasurement(data());

        assertSame(strategy, read(wrapper, "strategy"));
    }

    @Test
    void exceptionsAreSwallowedWhileTheToleranceIsNotYetExhausted() {
        final ADataSink inner = sink();
        doThrow(new IllegalStateException("boom")).when(inner).onStartedHierarchicalMeasurement(any());
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 1);

        assertDoesNotThrow(() -> wrapper.onStartedHierarchicalMeasurement("a"));

        verify(inner).onStartedHierarchicalMeasurement("a");
    }

    @Test
    void sinkIsDisabledAfterMaxNumTimeoutsConsecutiveExceptions() {
        final ADataSink inner = sink();
        doThrow(new IllegalStateException("boom")).when(inner).onStartedHierarchicalMeasurement(any());
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 1);

        wrapper.onStartedHierarchicalMeasurement("a");
        wrapper.onStartedHierarchicalMeasurement("b");

        wrapper.onStartedHierarchicalMeasurement("c");

        verify(inner, times(2)).onStartedHierarchicalMeasurement(any());
    }

    @Test
    void aSuccessfulCallResetsTheExceptionCounter() {
        final ADataSink inner = sink();
        doThrow(new IllegalStateException("boom")).when(inner).onStartedHierarchicalMeasurement(any());
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 1);

        wrapper.onStartedHierarchicalMeasurement("a");
        wrapper.onFinishedHierarchicalMeasurement(data()); // succeeds -> counter reset
        wrapper.onStartedHierarchicalMeasurement("b");

        verify(inner, times(2)).onStartedHierarchicalMeasurement(any());
    }

    @Test
    void exceptionsFromFinishedMeasurementsAlsoDisableTheSink() {
        final ADataSink inner = sink();
        doThrow(new IllegalStateException("boom")).when(inner).onFinishedHierarchicalMeasurement(any());
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 0);

        wrapper.onFinishedHierarchicalMeasurement(data());
        wrapper.onFinishedHierarchicalMeasurement(data());

        verify(inner, times(1)).onFinishedHierarchicalMeasurement(any());
    }

    @Test
    void aSingleTimeoutSwitchesToTheTimedOutStrategy() {
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(sink(), ALWAYS_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.onStartedHierarchicalMeasurement("a");

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void maxNumTimeoutsTimeoutsInARowDisableTheSinkPermanently() {
        final ADataSink inner = sink();
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, ALWAYS_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.onStartedHierarchicalMeasurement("a");
        wrapper.onStartedHierarchicalMeasurement("b");
        wrapper.onStartedHierarchicalMeasurement("c");

        assertNotSame(enabled, read(wrapper, "strategy"));
        wrapper.onStartedHierarchicalMeasurement("d");
        verify(inner, times(3)).onStartedHierarchicalMeasurement(any());
    }

    @Test
    void aFastCallAfterATimeoutRestoresTheInitialStrategy() {
        final ADataSink inner = sink();
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, ALWAYS_TIMES_OUT, 3);
        final Object enabled = read(wrapper, "strategy");

        wrapper.onStartedHierarchicalMeasurement("a");
        ImplTestSupport.write(wrapper, "timeoutNanos", NEVER_TIMES_OUT);
        wrapper.onStartedHierarchicalMeasurement("b");

        assertSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void finishedMeasurementsAlsoTriggerTheTimeoutHandling() {
        final ADataSink inner = sink();
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, ALWAYS_TIMES_OUT, 2);

        wrapper.onFinishedHierarchicalMeasurement(data());
        wrapper.onFinishedHierarchicalMeasurement(data());
        wrapper.onFinishedHierarchicalMeasurement(data());

        verify(inner, times(2)).onFinishedHierarchicalMeasurement(any());
    }

    @Test
    void shutdownIsForwardedToTheInnerSink() throws Exception {
        final ADataSink inner = sink();
        new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 3).shutdown();

        verify(inner).shutdown();
    }

    @Test
    void aFailingShutdownIsSwallowed() throws Exception {
        final ADataSink inner = sink();
        doThrow(new IllegalStateException("boom")).when(inner).shutdown();

        assertDoesNotThrow(() -> new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 3).shutdown());
    }

    @Test
    void aDisabledSinkIsNotCalledAnyMore() {
        final ADataSink inner = mock(ADataSink.class);
        doThrow(new IllegalStateException("boom")).when(inner).onStartedHierarchicalMeasurement(eq("kill"));
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, NEVER_TIMES_OUT, 0);

        wrapper.onStartedHierarchicalMeasurement("kill");
        verify(inner).onStartedHierarchicalMeasurement("kill");

        wrapper.onStartedHierarchicalMeasurement("kill");
        wrapper.onFinishedHierarchicalMeasurement(data());

        verify(inner, times(1)).onStartedHierarchicalMeasurement("kill");
        verify(inner, never()).onFinishedHierarchicalMeasurement(any());
    }

    @Test
    void anExceptionWhileTimedOutDisablesTheSink() {
        final ADataSink inner = new FailOnNthStartedCall(2);
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, ALWAYS_TIMES_OUT, 0);
        final Object enabled = read(wrapper, "strategy");

        wrapper.onStartedHierarchicalMeasurement("a"); // times out -> TIMED_OUT
        wrapper.onStartedHierarchicalMeasurement("b"); // throws while already timed out

        assertNotSame(enabled, read(wrapper, "strategy"));
    }

    @Test
    void anExceptionFromAFinishedMeasurementWhileTimedOutDisablesTheSink() {
        final ADataSink inner = new FailOnNthFinishedCall(2);
        final RobustDataSinkWrapper wrapper = new RobustDataSinkWrapper(inner, ALWAYS_TIMES_OUT, 0);
        final Object enabled = read(wrapper, "strategy");

        wrapper.onFinishedHierarchicalMeasurement(data());
        wrapper.onFinishedHierarchicalMeasurement(data());

        assertNotSame(enabled, read(wrapper, "strategy"));
    }
}
