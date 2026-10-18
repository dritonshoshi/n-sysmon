package com.nsysmon.servlet.performance.bottomup;

import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.ExplicitTimer;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AJdbcPageDefinitionTest {
    private static final String COL_DEFS_JSON =
            "\"columnDefs\":["
                    + "{\"name\":\"%\",\"isPercentage\":true,\"isTimestamp\":false,\"numFracDigits\":1,\"width\":\"Medium\"},"
                    + "{\"name\":\"%local\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":1,\"width\":\"Medium\"},"
                    + "{\"name\":\"#calls\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"},"
                    + "{\"name\":\"avg ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"},"
                    + "{\"name\":\"min ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"},"
                    + "{\"name\":\"max ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"}"
                    + "]";

    private ExplicitTimer timer;
    private NSysMonApi sysMon;
    private AJdbcPageDefinition page;
    private RecordingJson json;

    @BeforeEach void setUp() {
        timer = new ExplicitTimer();
        sysMon = new NSysMonImpl(TestSysMons.configBuilder().setTimer(timer).build());
        page = new AJdbcPageDefinition();
        page.init(sysMon);
        json = new RecordingJson();
    }

    private String servedDataOf(String service) throws Exception {
        assertTrue(page.handleRestCall(service, List.of(), json.helper()));
        return json.compactJson();
    }

    private void givenTwoJdbcCallsOfDifferentDuration() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        final ASimpleMeasurement service = sysMon.start("service");
        final ASimpleMeasurement a = sysMon.start("jdbc: SELECT A");
        timer.curNanos += 300;
        a.finish();
        final ASimpleMeasurement b = sysMon.start("jdbc: SELECT B");
        timer.curNanos += 700;
        b.finish();
        service.finish();
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("jdbc", page.getId());
        assertEquals("JDBC", page.getShortLabel());
        assertEquals("JDBC Performance Statistics", page.getFullLabel());
        assertEquals("aggregated.html", page.getHtmlFileName());
        assertEquals("CtrlAggregated", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void leafFilterAcceptsJdbcStatements() {
        assertTrue(page.createLeafFilter().isLeaf(node("jdbc: SELECT 1")));
    }

    @Test void leafFilterRejectsJdbcConnections() {
        assertFalse(page.createLeafFilter().isLeaf(node("jdbc:connection getConnection")));
    }

    @Test void leafFilterRejectsEverythingElse() {
        assertFalse(page.createLeafFilter().isLeaf(node("service")));
    }

    @Test void servesAnEmptyResultBeforeAnyMeasurement() throws Exception {
        assertEquals("{\"isStarted\":false," + COL_DEFS_JSON + ",\"traces\":[]}", servedDataOf("getData"));
    }

    @Test void doStartActivatesTheCollector() throws Exception {
        assertTrue(servedDataOf("doStart").contains("\"isStarted\":true"));
    }

    @Test void doStopDeactivatesTheCollector() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        assertTrue(servedDataOf("doStop").contains("\"isStarted\":false"));
    }

    @Test void collectsNothingWhileStopped() throws Exception {
        sysMon.start("jdbc: SELECT 1").finish();

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    @Test void doClearRemovesTheMeasurements() throws Exception {
        givenTwoJdbcCallsOfDifferentDuration();
        page.handleRestCall("doClear", List.of(), new RecordingJson().helper());

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    @Test void servesJdbcStatementsWithTheirCallersAndGlobalPercentage() throws Exception {
        givenTwoJdbcCallsOfDifferentDuration();

        final String result = servedDataOf("getData");

        assertEquals("{\"isStarted\":true," + COL_DEFS_JSON + ",\"traces\":["
                        + "{\"name\":\"jdbc: SELECT B\",\"data\":[70.0,70.0,1,0,0,0],"
                        + "\"children\":[{\"name\":\"service\",\"data\":[70.0,100.0,1,0,0,0]}]},"
                        + "{\"name\":\"jdbc: SELECT A\",\"data\":[30.0,30.0,1,0,0,0],"
                        + "\"children\":[{\"name\":\"service\",\"data\":[30.0,100.0,1,0,0,0]}]}"
                        + "]}",
                result);
    }

    @Test void sortsChildrenByCallCountAndBreaksTiesAlphabetically() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        final ASimpleMeasurement entryPoint = sysMon.start("entryPoint");
        givenOneSharedJdbcCallIn("serviceA", 2, 100);
        givenOneSharedJdbcCallIn("serviceB", 1, 200);
        givenOneSharedJdbcCallIn("serviceC", 1, 300);
        entryPoint.finish();

        final String result = servedDataOf("getData");

        assertTrue(result.endsWith("{\"name\":\"jdbc: SHARED\",\"data\":[100.0,100.0,4,0,0,0],\"children\":["
                + "{\"name\":\"serviceA\",\"data\":[50.0,50.0,2,0,0,0],"
                + "\"children\":[{\"name\":\"entryPoint\",\"data\":[50.0,100.0,2,0,0,0]}]},"
                + "{\"name\":\"serviceB\",\"data\":[25.0,25.0,1,0,0,0],"
                + "\"children\":[{\"name\":\"entryPoint\",\"data\":[25.0,100.0,1,0,0,0]}]},"
                + "{\"name\":\"serviceC\",\"data\":[25.0,25.0,1,0,0,0],"
                + "\"children\":[{\"name\":\"entryPoint\",\"data\":[25.0,100.0,1,0,0,0]}]}"
                + "]}]}"), result);
    }

    /** Opens a service with the given number of calls of a shared JDBC statement, each taking the same time. */
    private void givenOneSharedJdbcCallIn(String serviceName, int numCalls, int nanosPerCall) throws Exception {
        final ASimpleMeasurement service = sysMon.start(serviceName);
        for (int i = 0; i < numCalls; i++) {
            final ASimpleMeasurement call = sysMon.start("jdbc: SHARED");
            timer.curNanos += nanosPerCall;
            call.finish();
        }
        service.finish();
    }

    @Test void ignoresMeasurementsWithoutJdbcStatements() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());
        sysMon.start("onlyService").finish();

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    private static AHierarchicalData node(String identifier) {
        return new AHierarchicalData(true, 1L, 1L, identifier, Map.of(), List.of(), false);
    }
}
