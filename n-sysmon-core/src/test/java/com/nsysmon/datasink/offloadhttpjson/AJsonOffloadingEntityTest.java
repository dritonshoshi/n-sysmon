package com.nsysmon.datasink.offloadhttpjson;

import com.ajjpj.afoundation.util.AUUID;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NB: {@code AUUID.toString()} relies on {@code javax.xml.bind.DatatypeConverter}, which is not on the class path of
 * this project on a modern JDK. Every serialized uuid is therefore backed by a stubbed {@link AUUID} here - see
 * N-SysMon issue: the offloading sink cannot serialize real data on Java 11+.
 */
class AJsonOffloadingEntityTest {

    private static final String SENDER = "the-sender";
    private static final String SENDER_INSTANCE = "the-instance";

    private static AUUID stubbedUuid() {
        final AUUID uuid = mock(AUUID.class);
        when(uuid.toString()).thenReturn("<uuid>");
        return uuid;
    }

    private static AHierarchicalDataRoot traceOf(AHierarchicalData rootNode, List<ACorrelationId> startedFlows, List<ACorrelationId> joinedFlows) {
        final AUUID uuid = stubbedUuid();
        final AHierarchicalDataRoot trace = mock(AHierarchicalDataRoot.class);
        when(trace.getUuid()).thenReturn(uuid);
        when(trace.getRootNode()).thenReturn(rootNode);
        when(trace.getStartedFlows()).thenReturn(startedFlows);
        when(trace.getJoinedFlows()).thenReturn(joinedFlows);
        return trace;
    }

    private static AScalarDataPoint scalarAt(long timestamp, String name, long rawValue, int numFracDigits) {
        final AUUID uuid = stubbedUuid();
        final AScalarDataPoint scalar = mock(AScalarDataPoint.class);
        when(scalar.getUuid()).thenReturn(uuid);
        when(scalar.getTimestamp()).thenReturn(timestamp);
        when(scalar.getName()).thenReturn(name);
        when(scalar.getValueRaw()).thenReturn(rawValue);
        when(scalar.getNumFracDigits()).thenReturn(numFracDigits);
        return scalar;
    }

    private static AHierarchicalData node(boolean serial, long startTimeMillis, long durationNanos, String identifier, Map<String, String> parameters, AHierarchicalData... children) {
        return new AHierarchicalData(serial, startTimeMillis, durationNanos, identifier, parameters, Arrays.asList(children), false);
    }

    private static String serialize(AJsonOffloadingEntity entity) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        entity.writeTo(buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static AJsonOffloadingEntity entityOf(List<AHierarchicalDataRoot> traces, Collection<AScalarDataPoint> scalars) {
        return new AJsonOffloadingEntity(traces, scalars, SENDER, SENDER_INSTANCE);
    }

    private static AJsonOffloadingEntity emptyEntity() {
        return entityOf(Collections.emptyList(), Collections.emptyList());
    }

    /** A rendered scalar. The JSON helper terminates every object with a line break. */
    private static String scalarEntry(String name, long timestamp, String serializedValue) {
        return "{\"uuid\":\"<uuid>\",\"senderTimestamp\":" + timestamp + ",\"name\":\"" + name + "\",\"value\":" + serializedValue + "}\n";
    }

    /** A rendered correlation flow entry, terminated by a line break like every other JSON object. */
    private static String flowEntry(String kind, String ident) {
        return "{\"kind\":\"" + kind + "\",\"ident\":\"" + ident + "\"}\n";
    }

    /** A rendered trace node. Nested nodes are separated by the comma the JSON helper writes before each value. */
    private static String traceNode(String identifier, boolean serial, long startTimeMillis, long durationNanos, String parameters, String... children) {
        return "{\"isSerial\":" + serial + ",\"senderStartTimeMillis\":" + startTimeMillis + ",\"durationNanos\":" + durationNanos
                + ",\"identifier\":\"" + identifier + "\",\"parameters\":" + parameters + "\n,\"children\":["
                + String.join(",", children) + "]}\n";
    }

    /** Everything the payload starts with, up to (excluding) the "traces" key. */
    private static String payloadHead(long senderTimestamp) {
        return "{\"sender\":\"" + SENDER + "\",\"senderInstance\":\"" + SENDER_INSTANCE + "\",\"senderTimestamp\":" + senderTimestamp + ",";
    }

    /** A rendered trace including its flows, terminated by a line break like every other JSON object. */
    private static String traceEntry(String traceNode) {
        return "{\"uuid\":\"<uuid>\",\"startedFlows\":[],\"joinedFlows\":[],\"trace\":" + traceNode + "}\n";
    }

    private static long senderTimestampOf(String json) {
        final String key = "\"senderTimestamp\":";
        final int start = json.indexOf(key) + key.length();
        return Long.parseLong(json.substring(start, json.indexOf(',', start)));
    }

    @Test void isAStreamingChunkedEntity() {
        final AJsonOffloadingEntity entity = emptyEntity();

        assertTrue(entity.isStreaming());
        assertTrue(entity.isChunked());
        assertFalse(entity.isRepeatable());
        assertEquals(-1, entity.getContentLength());
    }

    @Test void doesNotSupportReadingItsContentBack() {
        assertThrows(UnsupportedOperationException.class, () -> emptyEntity().getContent());
    }

    @Test void writesEmptyArraysWhenNothingWasCollected() throws IOException {
        final String json = serialize(emptyEntity());

        assertTrue(json.matches("\\{\"sender\":\"" + SENDER + "\",\"senderInstance\":\"" + SENDER_INSTANCE
                + "\",\"senderTimestamp\":\\d+,\"traces\":\\[\\],\"scalars\":\\[\\]\\}\n"), json);
    }

    @Test void writesSenderSenderInstanceAndATimestampFromNow() throws IOException {
        final long before = System.currentTimeMillis();
        final String json = serialize(emptyEntity());
        final long after = System.currentTimeMillis();

        assertTrue(json.contains("\"sender\":\"" + SENDER + "\","), json);
        assertTrue(json.contains("\"senderInstance\":\"" + SENDER_INSTANCE + "\","), json);
        assertTrue(senderTimestampOf(json) >= before && senderTimestampOf(json) <= after, json);
    }

    @Test void writesScalarDataPointsWithTheirFractionDigits() throws IOException {
        final String json = serialize(entityOf(Collections.emptyList(),
                List.of(scalarAt(1700, "whole", 42, 0), scalarAt(1701, "fractional", 1500, 3))));

        assertEquals(payloadHead(senderTimestampOf(json)) + "\"traces\":[],\"scalars\":["
                + scalarEntry("whole", 1700, "42") + "," + scalarEntry("fractional", 1701, "1.500") + "]}\n", json);
    }

    @Test void writesACompleteTraceWithNestedChildrenAndParameters() throws IOException {
        final Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("jdbc.url", "jdbc:h2:mem:demo");
        final AHierarchicalDataRoot trace = traceOf(
                node(true, 1000, 500, "root", parameters,
                        node(true, 1400, 100, "child", Collections.emptyMap(),
                                node(false, 1500, 25, "grandChild", Collections.emptyMap()))),
                Collections.emptyList(), Collections.emptyList());

        final String json = serialize(entityOf(List.of(trace), Collections.emptyList()));

        final String renderedGrandChild = traceNode("grandChild", false, 1500, 25, "{}");
        final String renderedChild = traceNode("child", true, 1400, 100, "{}", renderedGrandChild);
        final String renderedRoot = traceNode("root", true, 1000, 500, "{\"jdbc.url\":\"jdbc:h2:mem:demo\"}", renderedChild);

        assertEquals(payloadHead(senderTimestampOf(json)) + "\"traces\":[" + traceEntry(renderedRoot) + "],\"scalars\":[]}\n", json);
    }

    @Test void writesStartedAndJoinedCorrelationFlows() throws IOException {
        final AHierarchicalDataRoot trace = traceOf(
                node(true, 1000, 500, "root", Collections.emptyMap()),
                List.of(new ACorrelationId("mainReason", "id-1", null)),
                List.of(new ACorrelationId("subReason", "id-2", "id-1")));

        final String json = serialize(entityOf(List.of(trace), Collections.emptyList()));

        assertTrue(json.contains("\"startedFlows\":[" + flowEntry("mainReason", "id-1") + "],"), json);
        assertTrue(json.contains("\"joinedFlows\":[" + flowEntry("subReason", "id-2") + "],"), json);
    }

    @Test void writesSeveralTracesAndScalarsInOnePayload() throws IOException {
        final AHierarchicalDataRoot first = traceOf(node(true, 1, 2, "first", Collections.emptyMap()), Collections.emptyList(), Collections.emptyList());
        final AHierarchicalDataRoot second = traceOf(node(true, 3, 4, "second", Collections.emptyMap()), Collections.emptyList(), Collections.emptyList());

        final String json = serialize(entityOf(List.of(first, second), List.of(scalarAt(5, "s", 6, 0))));

        assertEquals(3, countOccurrences(json, "\"uuid\":\"<uuid>\""));
        assertTrue(json.contains("\"identifier\":\"first\""), json);
        assertTrue(json.contains("\"identifier\":\"second\""), json);
        assertTrue(json.contains("\"scalars\":[" + scalarEntry("s", 5, "6") + "]}"), json);
    }

    @Test void escapesSpecialCharactersInIdentifiersAndParameters() throws IOException {
        final Map<String, String> parameters = Map.of("quote\"key", "back\\slash");
        final AHierarchicalDataRoot trace = traceOf(node(true, 1, 2, "we\"ird\\id", parameters), Collections.emptyList(), Collections.emptyList());

        final String json = serialize(entityOf(List.of(trace), Collections.emptyList()));

        assertTrue(json.contains("\"identifier\":\"we\\\"ird\\\\id\""), json);
        assertTrue(json.contains("\"quote\\\"key\":\"back\\\\slash\""), json);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for(int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count += 1;
        }
        return count;
    }
}