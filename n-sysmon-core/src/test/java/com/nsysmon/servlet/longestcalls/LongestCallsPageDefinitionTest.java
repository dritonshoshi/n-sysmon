package com.nsysmon.servlet.longestcalls;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AHierarchicalDataRoot;
import com.nsysmon.measure.scalar.AJmxGcMeasurer;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LongestCallsPageDefinitionTest {
    private NSysMonApi sysMon;
    private RecordingJson json;
    private Locale previousLocale;

    @BeforeEach void setUp() {
        previousLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        sysMon = TestSysMons.create();
        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        Locale.setDefault(previousLocale);
    }

    private static AHierarchicalData node(String identifier, long startTimeMillis, long durationNanos,
                                          Map<String, String> params, AHierarchicalData... children) {
        return new AHierarchicalData(true, startTimeMillis, durationNanos, identifier, params, List.of(children), false);
    }

    private void inject(AHierarchicalData rootNode) {
        sysMon.injectSyntheticMeasurement(new AHierarchicalDataRoot(rootNode,
                Collections.emptyList(), Collections.emptyList()));
    }

    private String servedDataOf(LongestCallsPageDefinition page, String service) throws Exception {
        assertTrue(page.handleRestCall(service, List.of(), json.helper()));
        return json.compactJson();
    }

    private LongestCallsPageDefinition page(boolean initiallyStarted, String findRegEx, String entriesToIgnore) {
        final LongestCallsPageDefinition result = new LongestCallsPageDefinition(10, initiallyStarted, findRegEx, entriesToIgnore);
        result.init(sysMon);
        return result;
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

    @Test void exposesItsLabelsAndFileNames() {
        final LongestCallsPageDefinition page = page(true, "", null);

        assertEquals("longestCalls", page.getId());
        assertEquals("Top 10 Longest Calls ", page.getShortLabel());
        assertEquals("Top 10 Longest Calls ", page.getFullLabel());
        assertEquals("aggregated.html", page.getHtmlFileName());
        assertEquals("CtrlAggregated", page.getControllerName());
    }

    @Test void appendsTheFindRegExToTheIdAndLabels() {
        final LongestCallsPageDefinition page = page(true, "jdbc:.*", null);

        assertEquals("longestCallsjdbc:.*", page.getId());
        assertEquals("Top 10 Longest Calls matching 'jdbc:.*'", page.getShortLabel());
    }

    @Test void toleratesABlankEntriesToIgnoreConfiguration() {
        final LongestCallsPageDefinition page = page(true, "", " , ");

        assertEquals("Top 10 Longest Calls ", page.getShortLabel());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page(true, "", null).handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesAnEmptyResultBeforeAnyCallWasMeasured() throws Exception {
        assertEquals("{\"isStarted\":true,\"columnDefs\":["
                        + "{\"name\":\"%\",\"isPercentage\":true,\"isTimestamp\":false,\"numFracDigits\":1,\"width\":\"Medium\"},"
                        + "{\"name\":\"total \u00b5s\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Long\"},"
                        + "{\"name\":\"self \u00b5s\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Long\"},"
                        + "{\"name\":\"start @\",\"isPercentage\":false,\"isTimestamp\":true,\"numFracDigits\":0,\"width\":\"Long\"}"
                        + "],\"traces\":[]}",
                servedDataOf(page(true, "", null), "getData"));
    }

    @Test void respectsTheInitialStartedFlag() throws Exception {
        assertTrue(servedDataOf(page(false, "", null), "getData").contains("\"isStarted\":false"));
    }

    @Test void doStartStartsCollecting() throws Exception {
        final LongestCallsPageDefinition page = page(false, "", null);

        assertTrue(servedDataOf(page, "doStart").contains("\"isStarted\":true"));
    }

    @Test void doStopStopsCollecting() throws Exception {
        assertTrue(servedDataOf(page(true, "", null), "doStop").contains("\"isStarted\":false"));
    }

    @Test void collectsNothingWhileStopped() throws Exception {
        final LongestCallsPageDefinition page = page(false, "", null);
        inject(node("root", 1_000L, 1_000L, Map.of()));

        assertTrue(servedDataOf(page, "getData").contains("\"traces\":[]"));
    }

    @Test void doClearRemovesTheMeasuredCalls() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        inject(node("root", 1_000L, 1_000L, Map.of()));
        page.handleRestCall("doClear", List.of(), new RecordingJson().helper());

        assertTrue(servedDataOf(page, "getData").contains("\"traces\":[]"));
    }

    @Test void writesPercentageTotalAndSelfTimeOfTheLongestCall() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        inject(node("root", 1_000L, 1_000_000L, Map.of()));

        final String result = servedDataOf(page, "getData");

        assertTrue(result.contains("\"name\":\"root\",\"data\":[100.0,1000,1000,"), result);
    }

    @Test void servesTheLongestCallsInDescendingOrder() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        inject(node("short", 1_000L, 100_000L, Map.of()));
        inject(node("long", 1_000L, 900_000L, Map.of()));

        final String result = servedDataOf(page, "getData");

        assertTrue(result.indexOf("\"name\":\"long\"") < result.indexOf("\"name\":\"short\""), result);
    }

    @Test void addsASelfNodeAndTheChildNodes() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        inject(node("root", 1_000L, 1_000_000L, Map.of(), node("child", 1_000L, 100_000L, Map.of())));

        final String result = servedDataOf(page, "getData");

        assertTrue(result.contains("\"children\":["
                + "{\"name\":\"<self>\",\"data\":[90.0,900,900,0]},"
                + "{\"id\":\"0\",\"name\":\"child\",\"data\":[10.0,100,100,"), result);
    }

    @Test void writesWasKilledForKilledCalls() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        sysMon.injectSyntheticMeasurement(new AHierarchicalDataRoot(
                new AHierarchicalData(true, 1_000L, 1_000L, "killed", Map.of(), List.of(), true),
                Collections.emptyList(), Collections.emptyList()));

        assertTrue(servedDataOf(page, "getData").contains("\"wasKilled\":true"));
    }

    @Test void writesAllParametersAsTooltipRowsSortedByKey() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            final LongestCallsPageDefinition page = page(true, "", null);
            inject(node("root", 1_000L, 1_000L, Map.of("url", "http://localhost", "sql", "SELECT 1")));

            assertTrue(servedDataOf(page, "getData").contains(
                            "\"tooltip\":[{\"id\":\"sql\",\"value\":\"SELECT 1\"},{\"id\":\"url\",\"value\":\"http://localhost\"}]"),
                    json.compactJson());
        }
    }

    @Test void omitsTooltipsWhenTheyAreDisabled() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(false)) {
            final LongestCallsPageDefinition page = page(true, "", null);
            inject(node("root", 1_000L, 1_000L, Map.of("sql", "SELECT 1")));

            assertFalse(servedDataOf(page, "getData").contains("tooltip"));
        }
    }

    @Test void writesGarbageCollectionMemoryValuesInMibibytes() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            final LongestCallsPageDefinition page = page(true, "", null);
            inject(node("Garbage Collection", 1_000L, 1_000L, gcParams()));

            assertTrue(servedDataOf(page, "getData").contains("\"value\":\"1.0MB (+1.0) / 2.0MB (-1.0)\""),
                    json.compactJson());
        }
    }

    @Test void onlyMeasuresTheCallsMatchingTheFindRegEx() throws Exception {
        final LongestCallsPageDefinition page = page(true, "jdbc:.*", null);
        inject(node("service", 1_000L, 1_000L, Map.of(), node("jdbc: SELECT 1", 1_000L, 100L, Map.of())));

        final String result = servedDataOf(page, "getData");

        assertEquals(1, result.split("\\\"name\\\":\\\"jdbc: SELECT 1\\\"", -1).length - 1, result);
    }

    @Test void skipsTheEntriesConfiguredToBeIgnored() throws Exception {
        final LongestCallsPageDefinition page = page(true, "jdbc:.*", "SELECT 2");
        inject(node("service", 1_000L, 1_000L, Map.of(), node("jdbc: SELECT 1", 1_000L, 100L, Map.of())));

        assertTrue(servedDataOf(page, "getData").contains("\"name\":\"jdbc: SELECT 1\""));
    }

    @Test void clampsNegativeSelfTimeToZero() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        inject(node("root", 1_000L, 1_000L, Map.of(), node("child", 1_000L, 5_000L, Map.of())));

        final String result = servedDataOf(page, "getData");

        assertFalse(result.contains("<self>"), result);
        assertTrue(result.contains("\"name\":\"root\",\"data\":[100.0,1,0,"), result);
    }

    @Test void guardsAgainstZeroLengthRootNodes() throws Exception {
        final LongestCallsPageDefinition page = page(true, "", null);
        inject(node("root", 1_000L, 0L, Map.of()));

        assertTrue(servedDataOf(page, "getData").contains("\"name\":\"root\",\"data\":[0.0,0,0,"));
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
