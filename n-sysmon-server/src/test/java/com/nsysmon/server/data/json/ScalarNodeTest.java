package com.nsysmon.server.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScalarNodeTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void exposesAllProperties() {
        final ScalarNode underTest = new ScalarNode();

        underTest.setInstanceIdentifier(Nodes.instance("app-1", "instance-1"));
        underTest.setUuid("scalar-uuid");
        underTest.setSenderTimestamp(10L);
        underTest.setAdjustedTimestamp(20L);
        underTest.setName("cpu.load");
        underTest.setValue(0.75);

        assertEquals(Nodes.instance("app-1", "instance-1"), underTest.getInstanceIdentifier());
        assertEquals("scalar-uuid", underTest.getUuid());
        assertEquals(10L, underTest.getSenderTimestamp());
        assertEquals(20L, underTest.getAdjustedTimestamp());
        assertEquals("cpu.load", underTest.getName());
        assertEquals(0.75, underTest.getValue());
    }

    @Test void toStringMentionsAllProperties() {
        final ScalarNode underTest = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);

        final String result = underTest.toString();

        assertEquals("ScalarNode{instanceIdentifier=null, uuid='scalar-uuid', senderTimestamp=10, "
                + "adjustedTimestamp=0, name='cpu.load', value=0.75}", result);
    }

    @Test void deserializesAnUploadedScalar() throws Exception {
        final ScalarNode underTest = om.readValue(
                "{\"uuid\":\"scalar-uuid\",\"name\":\"cpu.load\",\"value\":0.75,\"senderTimestamp\":10}",
                ScalarNode.class);

        assertEquals("scalar-uuid", underTest.getUuid());
        assertEquals("cpu.load", underTest.getName());
        assertEquals(0.75, underTest.getValue());
        assertEquals(10L, underTest.getSenderTimestamp());
        assertNull(underTest.getInstanceIdentifier());
    }

    @Test void serializesAllProperties() throws Exception {
        final ScalarNode underTest = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);

        assertEquals("{\"instanceIdentifier\":null,\"uuid\":\"scalar-uuid\",\"senderTimestamp\":10,"
                + "\"adjustedTimestamp\":0,\"name\":\"cpu.load\",\"value\":0.75}",
                om.writeValueAsString(underTest));
    }

    @Test void survivesJsonRoundTrip() throws Exception {
        final ScalarNode underTest = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);
        underTest.setAdjustedTimestamp(11L);

        final ScalarNode result = om.readValue(om.writeValueAsString(underTest), ScalarNode.class);

        assertEquals(underTest.toString(), result.toString());
        assertTrue(underTest.toString().contains("adjustedTimestamp=11"));
    }
}