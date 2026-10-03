package com.nsysmon.server.processing.impl;

import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceRootNode;
import com.nsysmon.server.processing.NewDataListener;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EventBusImplTest {
    private final EventBusImpl underTest = new EventBusImpl();

    @Test void addedListenerReceivesNewScalarData() {
        final RecordingListener listener = new RecordingListener();
        final ScalarNode scalar = Nodes.scalar("uuid", "cpu.load", 1.0, 10L);

        underTest.addListener(listener);
        underTest.fireNewScalarData(scalar);

        assertSame(scalar, listener.scalarData);
    }

    @Test void addedListenerReceivesNewEnvironmentData() {
        final RecordingListener listener = new RecordingListener();
        final EnvironmentNode environment = Nodes.environment("uuid", "os.name", "Linux", 10L);

        underTest.addListener(listener);
        underTest.fireNewEnvironmentData(environment);

        assertSame(environment, listener.environmentData);
    }

    @Test void addedListenerReceivesNewTrace() {
        final RecordingListener listener = new RecordingListener();
        final TraceRootNode trace = Nodes.traceRoot("uuid", Nodes.trace("root", 10L));

        underTest.addListener(listener);
        underTest.fireNewTrace(trace);

        assertSame(trace, listener.traceData);
    }

    @Test void allListenersAreNotifiedInRegistrationOrder() {
        final List<String> notifications = new ArrayList<>();
        final NewDataListener first = listenerRecording(notifications, "first");
        final NewDataListener second = listenerRecording(notifications, "second");

        underTest.addListener(first);
        underTest.addListener(second);
        underTest.fireNewScalarData(Nodes.scalar("uuid", "cpu.load", 1.0, 10L));

        assertEquals(List.of("first", "second"), notifications);
    }

    @Test void removedListenerIsNoLongerNotified() {
        final RecordingListener listener = new RecordingListener();

        underTest.addListener(listener);
        underTest.removeListener(listener);
        underTest.fireNewScalarData(Nodes.scalar("uuid", "cpu.load", 1.0, 10L));

        assertEquals(null, listener.scalarData);
    }

    @Test void firingWithoutListenersIsHarmless() {
        underTest.fireNewScalarData(Nodes.scalar("uuid", "cpu.load", 1.0, 10L));
        underTest.fireNewTrace(Nodes.traceRoot("uuid", Nodes.trace("root", 10L)));
        underTest.fireNewEnvironmentData(Nodes.environment("uuid", "os.name", "Linux", 10L));
    }

    @Test void listenerIsNotifiedOnceEvenWhenAddedTwice() {
        final List<String> notifications = new ArrayList<>();
        final NewDataListener listener = listenerRecording(notifications, "dup");

        underTest.addListener(listener);
        underTest.addListener(listener);
        underTest.fireNewScalarData(Nodes.scalar("uuid", "cpu.load", 1.0, 10L));

        assertEquals(List.of("dup", "dup"), notifications);
    }

    private static NewDataListener listenerRecording(List<String> notifications, String name) {
        return new NewDataListener() {
            @Override public void onNewScalarData(ScalarNode scalarData) {
                notifications.add(name);
            }
            @Override public void onNewTrace(TraceRootNode traceData) {
            }
            @Override public void onNewEnvironmentData(EnvironmentNode environmentData) {
            }
        };
    }

    private static class RecordingListener implements NewDataListener {
        ScalarNode scalarData;
        TraceRootNode traceData;
        EnvironmentNode environmentData;

        @Override public void onNewScalarData(ScalarNode scalarData) {
            this.scalarData = scalarData;
        }
        @Override public void onNewTrace(TraceRootNode traceData) {
            this.traceData = traceData;
        }
        @Override public void onNewEnvironmentData(EnvironmentNode environmentData) {
            this.environmentData = environmentData;
        }
    }
}