package com.nsysmon.datasink.offloadhttpjson;

import com.ajjpj.afoundation.util.AUUID;
import com.nsysmon.NSysMonApi;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.data.AScalarDataPoint;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Drives {@link AHttpJsonOffloadingDataSink} against a mocked {@link CloseableHttpClient}. No HTTP request ever
 * leaves the JVM: the static {@code HttpClients.createDefault()} factory is stubbed while the sink is constructed, and
 * the mocked {@code execute} parks the offloading thread so queue behaviour can be observed deterministically.
 */
class AHttpJsonOffloadingDataSinkTest {

    private static final String ENDPOINT = "http://localhost:1/upload";
    private static final long AWAIT_TIMEOUT_SECONDS = 20;
    /** Long enough that the scalar poller cannot contribute during a trace-only test. */
    private static final int SCALAR_POLLING_NEVER = 3_600_000;
    private static final long PARK_TIMEOUT_MILLIS = 200;

    private CloseableHttpClient httpClient;
    private CloseableHttpResponse httpResponse;
    private BlockingQueue<HttpPost> requests;
    private BlockingQueue<Object> releases;

    @BeforeEach void createHttpStubs() {
        httpClient = mock(CloseableHttpClient.class);
        httpResponse = mock(CloseableHttpResponse.class);
        requests = new LinkedBlockingQueue<>();
        releases = new LinkedBlockingQueue<>();
    }

    private MockedStatic<HttpClients> stubHttpClientFactory() throws IOException {
        when(httpClient.execute(any(HttpPost.class))).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(0));
            releases.poll(PARK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
            return httpResponse;
        });
        final MockedStatic<HttpClients> factory = mockStatic(HttpClients.class);
        factory.when(HttpClients::createDefault).thenReturn(httpClient);
        return factory;
    }

    private static AHttpJsonOffloadingDataSink createSink(NSysMonApi sysMon, int traceQueueSize, int scalarPollMillis) {
        return new AHttpJsonOffloadingDataSink(sysMon, ENDPOINT, "the-sender", "the-instance", traceQueueSize, 10, 1, scalarPollMillis);
    }

    private static NSysMonApi sysMonWithoutMeasurements() {
        return mock(NSysMonApi.class);
    }

    private static NSysMonApi sysMonWith(AScalarDataPoint... points) {
        final Map<String, AScalarDataPoint> measurements = new TreeMap<>();
        for (AScalarDataPoint point : points) {
            measurements.put(point.getName(), point);
        }
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getScalarMeasurements()).thenReturn(measurements);
        return sysMon;
    }

    private HttpPost awaitRequest() throws InterruptedException {
        final HttpPost post = requests.poll(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertNotNull(post, "the sink did not send any HTTP request");
        return post;
    }

    private void releaseParkedThread() {
        releases.add(Boolean.TRUE);
    }

    private static String payloadOf(HttpPost post) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        post.getEntity().writeTo(buffer);
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /**
     * NB: the uuid of a real trace cannot be serialized here - {@code AUUID.toString()} needs
     * {@code javax.xml.bind.DatatypeConverter}, which this project does not ship for modern JDKs.
     */
    private static AUUID stubbedUuid() {
        final AUUID uuid = mock(AUUID.class);
        when(uuid.toString()).thenReturn("<uuid>");
        return uuid;
    }

    private static AHierarchicalDataRoot traceOf(String identifier) {
        final AUUID uuid = stubbedUuid();
        final AHierarchicalData rootNode = new AHierarchicalData(true, 1000, 500, identifier, Collections.emptyMap(), Collections.emptyList(), false);
        final AHierarchicalDataRoot trace = mock(AHierarchicalDataRoot.class);
        when(trace.getUuid()).thenReturn(uuid);
        when(trace.getRootNode()).thenReturn(rootNode);
        when(trace.getStartedFlows()).thenReturn(List.of(new ACorrelationId("kind", "id", null)));
        when(trace.getJoinedFlows()).thenReturn(Collections.emptyList());
        return trace;
    }

    private static AScalarDataPoint scalarOf(String name, long value) {
        final AUUID uuid = stubbedUuid();
        final AScalarDataPoint scalar = mock(AScalarDataPoint.class);
        when(scalar.getUuid()).thenReturn(uuid);
        when(scalar.getTimestamp()).thenReturn(1700L);
        when(scalar.getName()).thenReturn(name);
        when(scalar.getValueRaw()).thenReturn(value);
        when(scalar.getNumFracDigits()).thenReturn(0);
        return scalar;
    }

    @Test void postsCollectedTracesAsJson() throws Exception {
        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            final AHttpJsonOffloadingDataSink sink = createSink(sysMonWithoutMeasurements(), 10, SCALAR_POLLING_NEVER);
            try {
                sink.onFinishedHierarchicalMeasurement(traceOf("collected"));

                final String payload = payloadOf(awaitRequest());
                assertTrue(payload.contains("\"identifier\":\"collected\""), payload);
                assertTrue(payload.contains("\"sender\":\"the-sender\""), payload);
                assertTrue(payload.contains("\"kind\":\"kind\""), payload);
            } finally {
                sink.shutdown();
            }
        }
    }

    @Test void postsScalarMeasurementsCollectedByThePoller() throws Exception {
        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            final AHttpJsonOffloadingDataSink sink = createSink(sysMonWith(scalarOf("load", 42)), 10, 20);
            try {
                final String payload = payloadOf(awaitRequest());
                assertTrue(payload.contains("\"name\":\"load\""), payload);
                assertTrue(payload.contains("\"traces\":[]"), payload);
            } finally {
                sink.shutdown();
            }
        }
    }

    @Test void postsEveryRequestToTheConfiguredUri() throws Exception {
        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            final AHttpJsonOffloadingDataSink sink = createSink(sysMonWithoutMeasurements(), 10, SCALAR_POLLING_NEVER);
            try {
                sink.onFinishedHierarchicalMeasurement(traceOf("collected"));

                assertEquals(ENDPOINT, awaitRequest().getURI().toString());
            } finally {
                sink.shutdown();
            }
        }
    }

    @Test void closesTheHttpResponseOfEveryRequest() throws Exception {
        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            final AHttpJsonOffloadingDataSink sink = createSink(sysMonWithoutMeasurements(), 10, SCALAR_POLLING_NEVER);
            try {
                sink.onFinishedHierarchicalMeasurement(traceOf("collected"));
                awaitRequest();
                releaseParkedThread();
            } finally {
                sink.shutdown();
            }
        }
        verify(httpResponse, timeout(AWAIT_TIMEOUT_SECONDS * 1000)).close();
    }

    @Test void closesTheHttpClientOnShutdown() throws Exception {
        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            createSink(sysMonWithoutMeasurements(), 10, SCALAR_POLLING_NEVER).shutdown();
        }
        verify(httpClient).close();
    }

    @Test void postsNothingWhileNoDataWasCollected() throws Exception {
        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            final AHttpJsonOffloadingDataSink sink = createSink(sysMonWithoutMeasurements(), 10, SCALAR_POLLING_NEVER);
            try {
                // neither queue can ever hold an element, so no request can be built at all
                sink.onStartedHierarchicalMeasurement("not-a-trace");
                assertTrue(requests.isEmpty());
            } finally {
                sink.shutdown();
            }
        }
    }

    @Test void discardsTheOldestTraceWhenTheTraceQueueOverflows() throws Exception {
        final List<String> payloads = new ArrayList<>();

        try (MockedStatic<HttpClients> ignored = stubHttpClientFactory()) {
            final AHttpJsonOffloadingDataSink sink = createSink(sysMonWithoutMeasurements(), 1, SCALAR_POLLING_NEVER);
            try {
                sink.onFinishedHierarchicalMeasurement(traceOf("first"));
                payloads.add(payloadOf(awaitRequest()));

                // the offloading thread is parked inside execute() now, so these two traces cannot be picked
                // up before the second one overflows the queue of size 1
                sink.onFinishedHierarchicalMeasurement(traceOf("second"));
                sink.onFinishedHierarchicalMeasurement(traceOf("third"));
                releaseParkedThread();

                payloads.add(payloadOf(awaitRequest()));
            } finally {
                releaseParkedThread();
                sink.shutdown();
            }
        }

        assertEquals(2, payloads.size());
        assertTrue(payloads.get(0).contains("\"identifier\":\"first\""), payloads.get(0));
        assertTrue(payloads.get(1).contains("\"identifier\":\"third\""), payloads.get(1));
        assertFalse(payloads.get(1).contains("\"identifier\":\"second\""), payloads.get(1));
    }
}
