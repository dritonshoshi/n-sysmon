package com.nsysmon.server.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraceRootNodeTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void flowListsAreEmptyNotNullByDefault() {
        final TraceRootNode underTest = new TraceRootNode();

        assertTrue(underTest.getStartedFlows().isEmpty());
        assertTrue(underTest.getJoinedFlows().isEmpty());
    }

    @Test void exposesAllProperties() {
        final TraceRootNode underTest = new TraceRootNode();
        final TraceNode trace = Nodes.trace("root", 10L);

        underTest.setInstanceIdentifier(Nodes.instance("app-1", "instance-1"));
        underTest.setUuid("trace-uuid");
        underTest.setTrace(trace);
        underTest.setStartedFlows(Nodes.correlations("jms"));
        underTest.setJoinedFlows(Nodes.correlations("jdbc"));

        assertEquals(Nodes.instance("app-1", "instance-1"), underTest.getInstanceIdentifier());
        assertEquals("trace-uuid", underTest.getUuid());
        assertEquals(trace, underTest.getTrace());
        assertEquals("jms", underTest.getStartedFlows().get(0).getKind());
        assertEquals("jdbc", underTest.getJoinedFlows().get(0).getKind());
    }

    @Test void toStringMentionsAllProperties() {
        final TraceRootNode underTest = Nodes.traceRoot("trace-uuid", Nodes.trace("root", 10L));

        assertEquals("TraceRootNode{instanceIdentifier=null, uuid='trace-uuid', "
                + "trace=TraceNode{isSerial=false, senderStartTimeMillis=10, adjustedStartTimeMillis=0, "
                + "durationNanos=0, identifier='root', parameters={}, children=[]}, "
                + "startedFlows=[], joinedFlows=[]}", underTest.toString());
    }

    @Test void deserializesAnUploadedTrace() throws Exception {
        final TraceRootNode underTest = om.readValue(
                "{\"uuid\":\"trace-uuid\",\"trace\":{\"identifier\":\"root\",\"senderStartTimeMillis\":10},"
                        + "\"startedFlows\":[{\"kind\":\"jms\",\"ident\":\"flow-1\"}]}",
                TraceRootNode.class);

        assertEquals("trace-uuid", underTest.getUuid());
        assertEquals("root", underTest.getTrace().getIdentifier());
        assertEquals(10L, underTest.getTrace().getSenderStartTimeMillis());
        assertEquals(1, underTest.getStartedFlows().size());
        assertEquals("jms", underTest.getStartedFlows().get(0).getKind());
        assertEquals("flow-1", underTest.getStartedFlows().get(0).getIdent());
        assertTrue(underTest.getJoinedFlows().isEmpty());
        assertNull(underTest.getInstanceIdentifier());
    }

    @Test void serializesAllProperties() throws Exception {
        final TraceRootNode underTest = Nodes.traceRoot("trace-uuid", Nodes.trace("root", 10L));

        assertEquals("{\"instanceIdentifier\":null,\"uuid\":\"trace-uuid\","
                + "\"trace\":{\"isSerial\":false,\"senderStartTimeMillis\":10,\"adjustedStartTimeMillis\":0,"
                + "\"durationNanos\":0,\"identifier\":\"root\",\"parameters\":{},\"children\":[]},"
                + "\"startedFlows\":[],\"joinedFlows\":[]}", om.writeValueAsString(underTest));
    }

    @Test void survivesJsonRoundTrip() throws Exception {
        final TraceRootNode underTest = Nodes.traceRoot("trace-uuid",
                Nodes.trace("root", 10L, Nodes.trace("child", 11L)));
        underTest.setStartedFlows(Nodes.correlations("jms"));

        final TraceRootNode result = om.readValue(om.writeValueAsString(underTest), TraceRootNode.class);

        assertEquals(underTest.toString(), result.toString());
    }
}
