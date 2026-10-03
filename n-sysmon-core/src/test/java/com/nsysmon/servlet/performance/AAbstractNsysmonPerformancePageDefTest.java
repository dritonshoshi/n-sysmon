package com.nsysmon.servlet.performance;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.servlet.performance.AAbstractNsysmonPerformancePageDef.ColDef;
import com.nsysmon.servlet.performance.AAbstractNsysmonPerformancePageDef.ColWidth;
import com.nsysmon.servlet.performance.AAbstractNsysmonPerformancePageDef.TreeNode;
import com.nsysmon.testutil.RecordingJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AAbstractNsysmonPerformancePageDefTest {
    private static final List<ColDef> COL_DEFS = Arrays.asList(
            new ColDef("%", true, 1, ColWidth.Medium, false),
            new ColDef("total µs", false, 0, ColWidth.Long, false));

    private static final String COL_DEFS_JSON =
            "\"columnDefs\":["
                    + "{\"name\":\"%\",\"isPercentage\":true,\"isTimestamp\":false,\"numFracDigits\":1,\"width\":\"Medium\"},"
                    + "{\"name\":\"total µs\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Long\"}"
                    + "]";

    private RecordingJson json;

    @BeforeEach void setUp() {
        json = new RecordingJson();
    }

    /** Minimal concrete page, so that the REST layer of the abstract base class can be driven in isolation. */
    private static final class TestPage extends AAbstractNsysmonPerformancePageDef {
        private boolean started = false;
        private int numStarts = 0;
        private int numStops = 0;
        private int numClears = 0;
        private List<TreeNode> data = new ArrayList<>();

        @Override public String getId() {
            return "test";
        }

        @Override public String getShortLabel() {
            return "Test";
        }

        @Override public String getFullLabel() {
            return "Test Page";
        }

        @Override protected void doStartMeasurements() {
            started = true;
            numStarts++;
        }

        @Override protected void doStopMeasurements() {
            started = false;
            numStops++;
        }

        @Override protected void doClearMeasurements() {
            numClears++;
        }

        @Override protected boolean isStarted() {
            return started;
        }

        @Override protected List<ColDef> getColDefs() {
            return COL_DEFS;
        }

        @Override protected List<TreeNode> getData() {
            return data;
        }
    }

    private static MockedStatic<NSysMon> withTooltipsEnabled(boolean enabled) {
        final NSysMonConfig config = mock(NSysMonConfig.class);
        when(config.collectTooltips()).thenReturn(enabled);
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(config);

        final MockedStatic<NSysMon> result = Mockito.mockStatic(NSysMon.class);
        result.when(NSysMon::get).thenReturn(sysMon);
        return result;
    }

    private String servedDataOf(TestPage page) throws Exception {
        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));
        return json.compactJson();
    }

    @Test void servesTheAggregatedHtmlPage() {
        final TestPage page = new TestPage();

        assertEquals("aggregated.html", page.getHtmlFileName());
        assertEquals("CtrlAggregated", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(new TestPage().handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void unknownServiceHasNoSideEffects() throws Exception {
        final TestPage page = new TestPage();

        page.handleRestCall("thereIsNoSuchService", List.of(), json.helper());

        assertEquals(0, page.numStarts);
        assertFalse(page.started);
    }

    @Test void servesTheColumnDefinitions() throws Exception {
        assertEquals("{\"isStarted\":false," + COL_DEFS_JSON + ",\"traces\":[]}", servedDataOf(new TestPage()));
    }

    @Test void doStartStartsCollectingAndServesData() throws Exception {
        final TestPage page = new TestPage();

        assertTrue(page.handleRestCall("doStart", List.of(), json.helper()));

        assertEquals(1, page.numStarts);
        assertTrue(json.compactJson().contains("\"isStarted\":true"));
    }

    @Test void doStopStopsCollectingAndServesData() throws Exception {
        final TestPage page = new TestPage();
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        assertTrue(page.handleRestCall("doStop", List.of(), json.helper()));

        assertEquals(1, page.numStops);
        assertTrue(json.compactJson().contains("\"isStarted\":false"));
    }

    @Test void doClearClearsAndServesData() throws Exception {
        final TestPage page = new TestPage();

        assertTrue(page.handleRestCall("doClear", List.of(), json.helper()));

        assertEquals(1, page.numClears);
        assertTrue(json.compactJson().contains("\"traces\":[]"));
    }

    @Test void writesIdNameAndDataOfATraceNode() throws Exception {
        final TestPage page = new TestPage();
        page.data = List.of(new TreeNode("the-id", "the-label", null, true,
                new long[]{1_500L, 2_000L}, List.of(), false));

        assertEquals("{\"isStarted\":false," + COL_DEFS_JSON + ","
                        + "\"traces\":[{\"id\":\"the-id\",\"name\":\"the-label\",\"data\":[150.0,2000]}]}",
                servedDataOf(page));
    }

    @Test void omitsTheIdForNodesThatHaveNone() throws Exception {
        final TestPage page = new TestPage();
        page.data = List.of(new TreeNode("the-label", true, new long[]{1_500L, 2_000L}, List.of()));

        assertTrue(servedDataOf(page).contains("\"traces\":[{\"name\":\"the-label\""));
    }

    @Test void marksParallelNodesAsNotSerial() throws Exception {
        final TestPage page = new TestPage();
        page.data = List.of(new TreeNode("parallel", false, new long[]{1_500L, 2_000L}, List.of()));

        assertTrue(servedDataOf(page).contains("\"isNotSerial\":true,\"data\":"));
    }

    @Test void writesWasKilledOnlyForKilledNodes() throws Exception {
        final TestPage page = new TestPage();
        page.data = List.of(new TreeNode("killed", "killed", null, true,
                new long[]{1_500L, 2_000L}, List.of(), true));

        assertTrue(servedDataOf(page).contains("\"wasKilled\":true,\"data\":"));
    }

    @Test void writesChildrenRecursively() throws Exception {
        final TestPage page = new TestPage();
        final TreeNode grandChild = new TreeNode("grandChild", true, new long[]{100L, 200L}, List.of());
        final TreeNode child = new TreeNode("child", true, new long[]{1_000L, 1_000L}, List.of(grandChild));
        page.data = List.of(new TreeNode("root", true, new long[]{1_500L, 2_000L}, List.of(child)));

        assertTrue(servedDataOf(page).contains(
                        "\"children\":[{\"name\":\"child\",\"data\":[100.0,1000],"
                                + "\"children\":[{\"name\":\"grandChild\",\"data\":[10.0,200]}]}]"),
                json.compactJson());
    }

    @Test void writesTwoColumnTooltipsAsObjects() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            final TestPage page = new TestPage();
            page.data = List.of(new TreeNode("node", "label", List.of(List.of("key", "value")),
                    true, new long[]{1_500L, 2_000L}, List.of(), false));

            assertTrue(servedDataOf(page).contains("\"tooltip\":[{\"id\":\"key\",\"value\":\"value\"}]"),
                    json.compactJson());
        }
    }

    @Test void writesTooltipsWithOtherColumnCountsAsArrays() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(true)) {
            final TestPage page = new TestPage();
            page.data = List.of(new TreeNode("node", "label", List.of(List.of("a", "b", "c")),
                    true, new long[]{1_500L, 2_000L}, List.of(), false));

            assertTrue(servedDataOf(page).contains("\"tooltip\":[[\"a\",\"b\",\"c\"]]"), json.compactJson());
        }
    }

    @Test void omitsTooltipsWhenTheyAreDisabled() throws Exception {
        try (MockedStatic<NSysMon> ignored = withTooltipsEnabled(false)) {
            final TestPage page = new TestPage();
            page.data = List.of(new TreeNode("node", "label", List.of(List.of("key", "value")),
                    true, new long[]{1_500L, 2_000L}, List.of(), false));

            assertFalse(servedDataOf(page).contains("tooltip"), json.compactJson());
        }
    }

    @Test void writesNullForNodesWithoutALabel() throws Exception {
        final TestPage page = new TestPage();
        page.data = List.of(new TreeNode(null, (String) null, null, true, new long[]{1_500L, 2_000L}, List.of(), false));

        assertTrue(servedDataOf(page).contains("\"traces\":[{\"name\":null,\"data\":[150.0,2000]}"),
                json.compactJson());
    }

    @Test void initDoesNotRequireAnApiInstance() {
        new TestPage().init(null);
    }
}
