package com.nsysmon.servlet.trace;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.scalar.AJmxGcMeasurer;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ATracePageDefinitionTest {
    /** Matches a "data" array of four numeric columns and captures the wall-clock one. */
    private static final Pattern START_TIMESTAMP =
            Pattern.compile("\"data\":\\[([^,\\]]*,[^,\\]]*,[^,\\]]*),(-?\\d+)]");

    private NSysMonApi sysMon;
    private ATracePageDefinition page;
    private RecordingJson json;
    private Locale previousLocale;

    @BeforeEach void setUp() {
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        sysMon = new NSysMonImpl(TestSysMons.configBuilder().build());
        page = new ATracePageDefinition(ATraceFilter.ALL, 10);
        page.init(sysMon);
        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        Locale.setDefault(previousLocale);
    }

    private static AHierarchicalData node(String identifier, long startTimeMillis, long durationNanos,
                                          Map<String, String> params, AHierarchicalData... children) {
        return new AHierarchicalData(true, startTimeMillis, durationNanos, identifier, params, List.of(children), false);
    }

    private void inject(AHierarchicalData... rootNodes) {
        for(AHierarchicalData rootNode: rootNodes) {
            sysMon.injectSyntheticMeasurement(new AHierarchicalDataRoot(rootNode,
                    Collections.emptyList(), Collections.emptyList()));
        }
    }

    private String servedDataOf(String service) throws Exception {
        assertTrue(page.handleRestCall(service, List.of(), json.helper()));
        return json.compactJson();
    }

    private static MockedStatic<NSysMon> withTooltipsEnabled(boolean enabled) {
        final NSysMonConfig config = mock(NSysMonConfig.class);
        when(config.collectTooltips()).thenReturn(enabled);
        final NSysMonApi mockApi = mock(NSysMonApi.class);
        when(mockApi.getConfig()).thenReturn(config);

        final MockedStatic<NSysMon> result = Mockito.mockStatic(NSysMon.class);
        result.when(NSysMon::get).thenReturn(mockApi);
        return result;
    }

    @Test void delegatesItsLabelsToTheFilter() {
        final ATracePageDefinition httpPage = new ATracePageDefinition(ATraceFilter.HTTP, 10);

        assertEquals("tracesAll", page.getId());
        assertEquals("All Traces", page.getShortLabel());
        assertEquals("All Traces", page.getFullLabel());
        assertEquals("tracesHttp", httpPage.getId());
        assertEquals("HTTP Requests", httpPage.getShortLabel());
        assertEquals("aggregated.html", page.getHtmlFileName());
        assertEquals("CtrlAggregated", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesAnEmptyResultBeforeAnyTrace() throws Exception {
        assertEquals("{\"isStarted\":true,\"columnDefs\":["
                        + "{\"name\":\"%\",\"isPercentage\":true,\"isTimestamp\":false,\"numFracDigits\":1,\"width\":\"Medium\"},"
                        + "{\"name\":\"total &mu;s\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Long\"},"
                        + "{\"name\":\"self &mu;s\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Long\"},"
                        + "{\"name\":\"start @\",\"isPercentage\":false,\"isTimestamp\":true,\"numFracDigits\":0,\"width\":\"Long\"}"
                        + "],\"traces\":[]}",
                servedDataOf("getData"));
    }

    @Test void collectsByDefault() throws Exception {
        assertTrue(servedDataOf("getData").contains("\"isStarted\":true"));
    }

    @Test void doStopStopsCollecting() throws Exception {
        assertTrue(servedDataOf("doStop").contains("\"isStarted\":false"));
    }

    @Test void collectsNothingWhileStopped() throws Exception {
        page.handleRestCall("doStop", List.of(), new RecordingJson().helper());

        inject(node("root", 1_000L, 1_000L, Map.of()));

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    @Test void doStartResumesCollecting() throws Exception {
        page.handleRestCall("doStop", List.of(), new RecordingJson().helper());
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        inject(node("root", 1_000L, 1_000L, Map.of()));

        assertTrue(servedDataOf("getData").contains("\"name\":\"root\""));
    }

    @Test void doClearRemovesTheTraces() throws Exception {
        inject(node("root", 1_000L, 1_000L, Map.of()));
        page.handleRestCall("doClear", List.of(), new RecordingJson().helper());

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    @Test void writesPercentageTotalSelfAndStartTimeOfATrace() throws Exception {
        inject(node("root", 1_000L, 1_000_000L, Map.of()));

        final String result = servedDataOf("getData");

        assertTrue(result.contains("\"name\":\"root\",\"data\":[100.0,1000,1000,"), result);
    }

    @Test void addsASelfNodeAndTheChildNodes() throws Exception {
        inject(node("root", 1_000L, 1_000_000L, Map.of(), node("child", 1_000L, 100_000L, Map.of())));

        final String result = servedDataOf("getData");

        assertTrue(result.contains("\"children\":["
                + "{\"name\":\"<self>\",\"data\":[90.0,900,900,0]},"
                + "{\"id\":\"0\",\"name\":\"child\",\"data\":[10.0,100,100,"), result);
    }

    @Test void doesNotAddASelfNodeForLeafTraces() throws Exception {
        inject(node("root", 1_000L, 1_000L, Map.of()));

        assertFalse(servedDataOf("getData").contains("<self>"));
    }

    @Test void writesWasKilledForKilledTraces() throws Exception {
        sysMon.injectSyntheticMeasurement(new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1_000L, 1_000L, "killed", Map.of(), List.of(), false),
                Collections.emptyList(), Collections.emptyList(), true));

        assertTrue(servedDataOf("getData").contains("\"wasKilled\":true"));
    }

    @Test void writesPlainParametersAsTooltipRows() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            inject(node("root", 1_000L, 1_000L, Map.of("sql", "SELECT 1")));

            assertTrue(servedDataOf("getData").contains("\"tooltip\":[{\"id\":\"sql\",\"value\":\"SELECT 1\"}]"));
        }
    }

    @Test void omitsTooltipsWhenTheyAreDisabled() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(false)) {
            inject(node("root", 1_000L, 1_000L, Map.of("sql", "SELECT 1")));

            assertFalse(servedDataOf("getData").contains("tooltip"));
        }
    }

    @Test void writesGarbageCollectionMemoryValuesInMibibytes() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            inject(node("Garbage Collection", 1_000L, 1_000L, gcParams()));

            assertTrue(servedDataOf("getData").contains("\"value\":\"1.0MB (+1.0) / 2.0MB (-1.0)\""),
                    json.compactJson());
        }
    }

    @Test void keepsOnlyOneNonNumericParameterBecauseTheKeySorterCannotOrderThem() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            inject(node("root", 1_000L, 1_000L, Map.of("sql", "SELECT 1", "url", "http://localhost")));

            assertEquals(1, countOccurrences(servedDataOf("getData"), "\"tooltip\":[{"));
        }
    }

    private static int countOccurrences(String haystack, String needle) {
        int result = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + 1)) {
            result++;
        }
        return result;
    }

    @Test void exportsTheSameDataAsTheRestCall() throws Exception {
        inject(node("root", 1_000L, 1_000_000L, Map.of()));
        final String expected = servedDataOf("getData");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        page.getDataForExport(out);

        final String actual = out.toString(StandardCharsets.UTF_8).replace("\n", "");
        // Both code paths stamp the trace with System.currentTimeMillis() independently, so the
        // wall-clock "start @" column may differ by a millisecond between the two calls.
        assertEquals(normalizeStartTimestamps(expected), normalizeStartTimestamps(actual));
        assertTrue(Math.abs(startTimestampOf(expected) - startTimestampOf(actual)) < 5_000,
                "the two renderings must describe the same point in time");
    }

    /** Replaces the 4th element of every "data" array, which holds the wall-clock start time. */
    private static String normalizeStartTimestamps(String json) {
        return START_TIMESTAMP.matcher(json).replaceAll("\"data\":[$1,<ts>]");
    }

    private static long startTimestampOf(String json) {
        final Matcher matcher = START_TIMESTAMP.matcher(json);
        if(!matcher.find()) {
            throw new AssertionError("no trace data array in: " + json);
        }
        return Long.parseLong(matcher.group(2));
    }

    @Test void sortsTracesByStartTimeDescending() throws Exception {
        inject(node("earlier", 1_000L, 1_000L, Map.of()));
        inject(node("later", 2_000L, 1_000L, Map.of()));

        final String result = servedDataOf("getData");

        assertTrue(result.indexOf("\"name\":\"later\"") < result.indexOf("\"name\":\"earlier\""), result);
    }

    @Test void sortsNumericTooltipKeysNumerically() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            inject(node("root", 1_000L, 1_000L, Map.of("10", "ten", "2", "two", "1", "one")));

            assertTrue(servedDataOf("getData").contains(
                            "\"tooltip\":["
                                    + "{\"id\":\"1\",\"value\":\"one\"},"
                                    + "{\"id\":\"2\",\"value\":\"two\"},"
                                    + "{\"id\":\"10\",\"value\":\"ten\"}]"),
                    json.compactJson());
        }
    }

    @Test void clampsNegativeSelfTimeToZero() throws Exception {
        inject(node("root", 1_000L, 1_000L, Map.of(), node("child", 1_000L, 5_000L, Map.of())));

        final String result = servedDataOf("getData");

        assertFalse(result.contains("<self>"), result);
        assertTrue(result.contains("\"name\":\"root\",\"data\":[100.0,1,0,"), result);
    }

    @Test void guardsAgainstZeroLengthRootNodes() throws Exception {
        inject(node("root", 1_000L, 0L, Map.of()));

        final String result = servedDataOf("getData");

        assertTrue(result.contains("\"name\":\"root\",\"data\":[0.0,0,0,"), result);
    }

    private static Map<String, String> gcParams() {
        final Map<String, String> params = new HashMap<>();
        params.put(AJmxGcMeasurer.KEY_ID, "42");
        params.put(AJmxGcMeasurer.KEY_CAUSE, "G1 Evacuation Pause");
        params.put(AJmxGcMeasurer.getUsedAfterKey("heap"), String.valueOf(1_048_576L));
        params.put(AJmxGcMeasurer.getCommittedAfterKey("heap"), String.valueOf(2_097_152L));
        params.put(AJmxGcMeasurer.getUsedDeltaKey("heap"), String.valueOf(1_048_576L));
        params.put(AJmxGcMeasurer.getCommittedDeltaKey("heap"), String.valueOf(-1_048_576L));
        return params;
    }
}
