package com.nsysmon.server.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RootNodeTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void listsAreEmptyNotNullByDefault() {
        final RootNode underTest = new RootNode();

        assertNotNull(underTest.getTraces());
        assertNotNull(underTest.getScalars());
        assertNotNull(underTest.getEnvironment());
        assertTrue(underTest.getTraces().isEmpty());
        assertTrue(underTest.getScalars().isEmpty());
        assertTrue(underTest.getEnvironment().isEmpty());
    }

    @Test void exposesSenderFields() {
        final RootNode underTest = new RootNode();

        underTest.setSender("app-1");
        underTest.setSenderInstance("instance-1");
        underTest.setSenderTimestamp(1234L);
        underTest.setAdjustedTimestamp(5678L);

        assertEquals("app-1", underTest.getSender());
        assertEquals("instance-1", underTest.getSenderInstance());
        assertEquals(1234L, underTest.getSenderTimestamp());
        assertEquals(5678L, underTest.getAdjustedTimestamp());
    }

    @Test void exposesAllThreeDataLists() {
        final RootNode underTest = new RootNode();
        final List<TraceRootNode> traces = List.of(new TraceRootNode());
        final List<ScalarNode> scalars = List.of(new ScalarNode());
        final List<EnvironmentNode> environment = List.of(new EnvironmentNode());

        underTest.setTraces(traces);
        underTest.setScalars(scalars);
        underTest.setEnvironment(environment);

        assertEquals(traces, underTest.getTraces());
        assertEquals(scalars, underTest.getScalars());
        assertEquals(environment, underTest.getEnvironment());
    }

    @Test void toStringMentionsSenderAndAllLists() {
        final RootNode underTest = Nodes.root("app-1", "instance-1", 7L);

        final String result = underTest.toString();

        assertTrue(result.startsWith("RootNode{sender='app-1'"), result);
        assertTrue(result.contains("senderTimestamp=7"), result);
        assertTrue(result.contains("traces=[]"), result);
        assertTrue(result.contains("scalars=[]"), result);
        assertTrue(result.contains("environment=[]"), result);
    }

    @Test void deserializesAnUploadedPayload() throws Exception {
        final RootNode underTest = om.readValue(Nodes.ROOT_NODE_JSON, RootNode.class);

        assertEquals("app-1", underTest.getSender());
        assertEquals("instance-1", underTest.getSenderInstance());
        assertEquals(1700000000000L, underTest.getSenderTimestamp());
        assertEquals(1, underTest.getEnvironment().size());
        assertEquals(1, underTest.getScalars().size());
        assertEquals(1, underTest.getTraces().size());
    }

    @Test void deserializesAPayloadWithoutAnyData() throws Exception {
        final RootNode underTest = om.readValue("{\"sender\":\"app-1\"}", RootNode.class);

        assertEquals("app-1", underTest.getSender());
        assertTrue(underTest.getScalars().isEmpty());
        assertTrue(underTest.getTraces().isEmpty());
        assertTrue(underTest.getEnvironment().isEmpty());
    }

    @Test void serializesAllFields() throws Exception {
        final RootNode underTest = Nodes.root("app-1", "instance-1", 7L);

        final String json = om.writeValueAsString(underTest);

        assertEquals("{\"sender\":\"app-1\",\"senderInstance\":\"instance-1\",\"senderTimestamp\":7,"
                + "\"adjustedTimestamp\":0,\"traces\":[],\"scalars\":[],\"environment\":[]}", json);
    }

    @Test void survivesJsonRoundTrip() throws Exception {
        final RootNode underTest = om.readValue(Nodes.ROOT_NODE_JSON, RootNode.class);

        final RootNode result = om.readValue(om.writeValueAsString(underTest), RootNode.class);

        assertEquals(underTest.toString(), result.toString());
    }
}