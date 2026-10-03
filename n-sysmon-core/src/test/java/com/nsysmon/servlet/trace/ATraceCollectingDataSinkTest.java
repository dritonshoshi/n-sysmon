package com.nsysmon.servlet.trace;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ATraceCollectingDataSinkTest {
    private ATraceFilter filter;
    private ATraceCollectingDataSink sink;

    @BeforeEach void setUp() {
        filter = new ATraceFilter() {
            @Override public String getId() {
                return "testFilter";
            }

            @Override public String getShortLabel() {
                return "Test";
            }

            @Override public String getFullLabel() {
                return "Test Filter";
            }

            @Override public boolean shouldCollect(AHierarchicalDataRoot trace) {
                return true;
            }
        };
        sink = new ATraceCollectingDataSink(filter, 3);
    }

    private AHierarchicalDataRoot trace(String identifier) {
        return new AHierarchicalDataRoot(new AHierarchicalData(true, 1L, 1L, identifier,
                Map.of(), Collections.emptyList(), false), Collections.emptyList(), Collections.emptyList());
    }

    private static ATraceFilter neverCollectingFilter() {
        return new ATraceFilter() {
            @Override public String getId() {
                return "never";
            }

            @Override public String getShortLabel() {
                return "Never";
            }

            @Override public String getFullLabel() {
                return "Never";
            }

            @Override public boolean shouldCollect(AHierarchicalDataRoot trace) {
                return false;
            }
        };
    }

    private List<String> collectedIdentifiers() {
        return collectedIdentifiersOf(sink);
    }

    private static List<String> collectedIdentifiersOf(ATraceCollectingDataSink someSink) {
        final List<String> result = new ArrayList<>();
        for(AHierarchicalDataRoot t: someSink.getData()) {
            result.add(t.getRootNode().getIdentifier());
        }
        return result;
    }

    @Test void collectsByDefault() {
        assertTrue(sink.isStarted);
    }

    @Test void storesAcceptedTraces() {
        sink.onFinishedHierarchicalMeasurement(trace("a"));

        assertEquals(List.of("a"), collectedIdentifiers());
    }

    @Test void discardsRejectedTraces() {
        final ATraceCollectingDataSink rejectingSink = new ATraceCollectingDataSink(neverCollectingFilter(), 3);

        rejectingSink.onFinishedHierarchicalMeasurement(trace("a"));

        assertTrue(collectedIdentifiersOf(rejectingSink).isEmpty());
    }

    @Test void discardsTracesWhileStopped() {
        sink.isStarted = false;

        sink.onFinishedHierarchicalMeasurement(trace("a"));

        assertTrue(collectedIdentifiers().isEmpty());
    }

    @Test void keepsTheOldestTraceInTheBuffer() {
        for(int i = 0; i < 3; i++) {
            sink.onFinishedHierarchicalMeasurement(trace("t" + i));
        }

        assertEquals(List.of("t0", "t1", "t2"), collectedIdentifiers());
    }

    @Test void overwritesTheOldestTraceWhenTheBufferIsFull() {
        for(int i = 0; i < 4; i++) {
            sink.onFinishedHierarchicalMeasurement(trace("t" + i));
        }

        assertEquals(List.of("t1", "t2", "t3"), collectedIdentifiers());
    }

    @Test void clearRemovesAllTraces() {
        sink.onFinishedHierarchicalMeasurement(trace("a"));

        sink.clear();

        assertTrue(collectedIdentifiers().isEmpty());
    }

    @Test void toleratesTheLifecycleCallbacks() throws Exception {
        sink.onStartedHierarchicalMeasurement("whatever");
        sink.shutdown();

        assertTrue(collectedIdentifiers().isEmpty());
    }

    @Test void getDataIteratesTheStoredTracesInInsertionOrder() {
        sink.onFinishedHierarchicalMeasurement(trace("a"));
        sink.onFinishedHierarchicalMeasurement(trace("b"));

        assertEquals(2, collectedIdentifiers().size());
        assertEquals(List.of("a", "b"), collectedIdentifiers());
    }
}
