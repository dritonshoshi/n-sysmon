package com.nsysmon.server.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EnvironmentNodeTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void exposesAllProperties() {
        final EnvironmentNode underTest = new EnvironmentNode();

        underTest.setInstanceIdentifier(Nodes.instance("app-1", "instance-1"));
        underTest.setUuid("env-uuid");
        underTest.setSenderTimestamp(10L);
        underTest.setAdjustedTimestamp(20L);
        underTest.setKey("os.name");
        underTest.setValue("Linux");

        assertEquals(Nodes.instance("app-1", "instance-1"), underTest.getInstanceIdentifier());
        assertEquals("env-uuid", underTest.getUuid());
        assertEquals(10L, underTest.getSenderTimestamp());
        assertEquals(20L, underTest.getAdjustedTimestamp());
        assertEquals("os.name", underTest.getKey());
        assertEquals("Linux", underTest.getValue());
    }

    @Test void toStringMentionsAllProperties() {
        final EnvironmentNode underTest = Nodes.environment("env-uuid", "os.name", "Linux", 10L);

        assertEquals("EnvironmentNode{instanceIdentifier=null, uuid='env-uuid', senderTimestamp=10, "
                + "adjustedTimestamp=0, key='os.name', value='Linux'}", underTest.toString());
    }

    @Test void deserializesAnUploadedEnvironmentEntry() throws Exception {
        final EnvironmentNode underTest = om.readValue(
                "{\"uuid\":\"env-uuid\",\"key\":\"os.name\",\"value\":\"Linux\",\"senderTimestamp\":10}",
                EnvironmentNode.class);

        assertEquals("env-uuid", underTest.getUuid());
        assertEquals("os.name", underTest.getKey());
        assertEquals("Linux", underTest.getValue());
        assertEquals(10L, underTest.getSenderTimestamp());
        assertNull(underTest.getInstanceIdentifier());
    }

    @Test void serializesAllProperties() throws Exception {
        final EnvironmentNode underTest = Nodes.environment("env-uuid", "os.name", "Linux", 10L);

        assertEquals("{\"instanceIdentifier\":null,\"uuid\":\"env-uuid\",\"senderTimestamp\":10,"
                + "\"adjustedTimestamp\":0,\"key\":\"os.name\",\"value\":\"Linux\"}",
                om.writeValueAsString(underTest));
    }

    @Test void survivesJsonRoundTrip() throws Exception {
        final EnvironmentNode underTest = Nodes.environment("env-uuid", "os.name", "Linux", 10L);
        underTest.setAdjustedTimestamp(11L);

        final EnvironmentNode result = om.readValue(om.writeValueAsString(underTest), EnvironmentNode.class);

        assertEquals(underTest.toString(), result.toString());
    }
}
