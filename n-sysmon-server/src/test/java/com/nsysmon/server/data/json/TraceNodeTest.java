package com.nsysmon.server.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceNodeTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void parametersAndChildrenAreEmptyNotNullByDefault() {
        final TraceNode underTest = new TraceNode();

        assertTrue(underTest.getParameters().isEmpty());
        assertTrue(underTest.getChildren().isEmpty());
    }

    @Test void exposesAllProperties() {
        final TraceNode underTest = new TraceNode();
        final TraceNode child = Nodes.trace("child", 30L);

        underTest.setIsSerial(true);
        underTest.setSenderStartTimeMillis(10L);
        underTest.setAdjustedStartTimeMillis(20L);
        underTest.setDurationNanos(1000L);
        underTest.setIdentifier("root");
        underTest.setParameters(Map.of("sql", "select 1"));
        underTest.setChildren(List.of(child));

        assertTrue(underTest.getIsSerial());
        assertEquals(10L, underTest.getSenderStartTimeMillis());
        assertEquals(20L, underTest.getAdjustedStartTimeMillis());
        assertEquals(1000L, underTest.getDurationNanos());
        assertEquals("root", underTest.getIdentifier());
        assertEquals(Map.of("sql", "select 1"), underTest.getParameters());
        assertEquals(List.of(child), underTest.getChildren());
    }

    @Test void toStringMentionsAllProperties() {
        final TraceNode underTest = Nodes.trace("root", 10L);
        underTest.setIsSerial(true);
        underTest.setDurationNanos(1000L);

        assertEquals("TraceNode{isSerial=true, senderStartTimeMillis=10, adjustedStartTimeMillis=0, "
                + "durationNanos=1000, identifier='root', parameters={}, children=[]}", underTest.toString());
    }

    @Test void deserializesANestedTrace() throws Exception {
        final TraceNode underTest = om.readValue(
                "{\"isSerial\":true,\"senderStartTimeMillis\":10,\"durationNanos\":1000,\"identifier\":\"root\","
                        + "\"parameters\":{\"sql\":\"select 1\"},"
                        + "\"children\":[{\"senderStartTimeMillis\":11,\"identifier\":\"child\"}]}",
                TraceNode.class);

        assertTrue(underTest.getIsSerial());
        assertEquals(10L, underTest.getSenderStartTimeMillis());
        assertEquals(1000L, underTest.getDurationNanos());
        assertEquals("root", underTest.getIdentifier());
        assertEquals(Map.of("sql", "select 1"), underTest.getParameters());
        assertEquals(1, underTest.getChildren().size());
        assertEquals("child", underTest.getChildren().get(0).getIdentifier());
        assertEquals(11L, underTest.getChildren().get(0).getSenderStartTimeMillis());
    }

    @Test void serializesAllProperties() throws Exception {
        final TraceNode underTest = Nodes.trace("root", 10L);

        assertEquals("{\"isSerial\":false,\"senderStartTimeMillis\":10,\"adjustedStartTimeMillis\":0,"
                + "\"durationNanos\":0,\"identifier\":\"root\",\"parameters\":{},\"children\":[]}",
                om.writeValueAsString(underTest));
    }

    @Test void survivesJsonRoundTrip() throws Exception {
        final TraceNode underTest = Nodes.trace("root", 10L, Nodes.trace("child", 11L));
        underTest.setParameters(Map.of("sql", "select 1"));
        underTest.setAdjustedStartTimeMillis(12L);

        final TraceNode result = om.readValue(om.writeValueAsString(underTest), TraceNode.class);

        assertEquals(underTest.toString(), result.toString());
    }
}
