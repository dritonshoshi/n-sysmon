package com.nsysmon.server.upload.preprocess.impl;

import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceNode;
import com.nsysmon.server.data.json.TraceRootNode;
import com.nsysmon.server.processing.EventBus;
import com.nsysmon.server.upload.preprocess.SystemClockCorrector;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InputProcessorImplTest {
    private static final long CLOCK_OFFSET = 1000L;

    private static final InstanceIdentifier INSTANCE = Nodes.instance("app-1", "instance-1");

    @Mock private SystemClockCorrector systemClockCorrector;
    @Mock private EventBus eventBus;

    @Test void updateSystemClockDiffDelegatesToTheCorrector() {
        final InputProcessorImpl underTest = createProcessor();

        underTest.updateSystemClockDiff(INSTANCE, 10L);

        verify(systemClockCorrector).updateSystemClockDiff(INSTANCE, 10L);
    }

    @Test void addScalarEntryStoresTheInstanceAndTheCorrectedTimestamp() {
        final InputProcessorImpl underTest = createProcessor();
        final ScalarNode scalar = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);

        underTest.addScalarEntry(INSTANCE, scalar);

        assertEquals(INSTANCE, scalar.getInstanceIdentifier());
        assertEquals(10L, scalar.getSenderTimestamp());
        assertEquals(10L + CLOCK_OFFSET, scalar.getAdjustedTimestamp());
    }

    @Test void addScalarEntryFiresTheEventBus() {
        final InputProcessorImpl underTest = createProcessor();
        final ScalarNode scalar = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);

        underTest.addScalarEntry(INSTANCE, scalar);

        verify(eventBus).fireNewScalarData(scalar);
    }

    @Test void addEnvironmentEntryStoresTheInstanceAndTheCorrectedTimestamp() {
        final InputProcessorImpl underTest = createProcessor();
        final EnvironmentNode environment = Nodes.environment("env-uuid", "os.name", "Linux", 20L);

        underTest.addEnvironmentEntry(INSTANCE, environment);

        assertEquals(INSTANCE, environment.getInstanceIdentifier());
        assertEquals(20L + CLOCK_OFFSET, environment.getAdjustedTimestamp());
    }

    @Test void addEnvironmentEntryFiresTheEventBus() {
        final InputProcessorImpl underTest = createProcessor();
        final EnvironmentNode environment = Nodes.environment("env-uuid", "os.name", "Linux", 20L);

        underTest.addEnvironmentEntry(INSTANCE, environment);

        verify(eventBus).fireNewEnvironmentData(environment);
    }

    @Test void addTraceEntryCorrectsTheRootTraceTimestamp() {
        final InputProcessorImpl underTest = createProcessor();
        final TraceNode trace = Nodes.trace("root", 30L);

        underTest.addTraceEntry(INSTANCE, Nodes.traceRoot("trace-uuid", trace));

        assertEquals(30L + CLOCK_OFFSET, trace.getAdjustedStartTimeMillis());
    }

    @Test void addTraceEntryCorrectsTheTimestampsOfAllDescendants() {
        final InputProcessorImpl underTest = createProcessor();
        final TraceNode grandChild = Nodes.trace("grand-child", 50L);
        final TraceNode child = Nodes.trace("child", 40L, grandChild);
        final TraceNode root = Nodes.trace("root", 30L, child);

        underTest.addTraceEntry(INSTANCE, Nodes.traceRoot("trace-uuid", root));

        assertEquals(40L + CLOCK_OFFSET, child.getAdjustedStartTimeMillis());
        assertEquals(50L + CLOCK_OFFSET, grandChild.getAdjustedStartTimeMillis());
    }

    @Test void addTraceEntryStoresTheInstanceAndFiresTheEventBus() {
        final InputProcessorImpl underTest = createProcessor();
        final TraceRootNode traceRoot = Nodes.traceRoot("trace-uuid", Nodes.trace("root", 30L));

        underTest.addTraceEntry(INSTANCE, traceRoot);

        assertEquals(INSTANCE, traceRoot.getInstanceIdentifier());
        verify(eventBus).fireNewTrace(traceRoot);
    }

    @Test void correctedTimestampsAreDerivedFromTheSenderTimestamp() {
        final InputProcessorImpl underTest = createProcessor();
        final TraceNode trace = Nodes.trace("root", 30L);

        underTest.addTraceEntry(INSTANCE, Nodes.traceRoot("trace-uuid", trace));

        verify(systemClockCorrector).correctedTimestamp(eq(INSTANCE), eq(30L));
    }

    private InputProcessorImpl createProcessor() {
        when(systemClockCorrector.correctedTimestamp(any(), anyLong()))
                .thenAnswer(call -> call.getArgument(1, Long.class) + CLOCK_OFFSET);
        return new InputProcessorImpl(systemClockCorrector, eventBus);
    }
}