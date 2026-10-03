package com.nsysmon.server.data.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CorrelationIdTest {
    private final ObjectMapper om = new ObjectMapper();

    @Test void exposesKindAndIdent() {
        final CorrelationId underTest = new CorrelationId();

        underTest.setKind("jms");
        underTest.setIdent("flow-1");

        assertEquals("jms", underTest.getKind());
        assertEquals("flow-1", underTest.getIdent());
    }

    @Test void toStringMentionsKindAndIdent() {
        assertEquals("CorrelationId{kind='jms', ident='flow-1'}",
                Nodes.correlationId("jms", "flow-1").toString());
    }

    @Test void serializesBothProperties() throws Exception {
        assertEquals("{\"kind\":\"jms\",\"ident\":\"flow-1\"}",
                om.writeValueAsString(Nodes.correlationId("jms", "flow-1")));
    }

    @Test void survivesJsonRoundTrip() throws Exception {
        final CorrelationId underTest = Nodes.correlationId("jms", "flow-1");

        final CorrelationId result = om.readValue(om.writeValueAsString(underTest), CorrelationId.class);

        assertEquals("jms", result.getKind());
        assertEquals("flow-1", result.getIdent());
    }
}