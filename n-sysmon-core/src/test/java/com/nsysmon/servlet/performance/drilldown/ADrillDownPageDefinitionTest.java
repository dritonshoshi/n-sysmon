package com.nsysmon.servlet.performance.drilldown;

import com.nsysmon.NSysMonApi;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.ExplicitTimer;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ADrillDownPageDefinitionTest {
    private static final String COL_DEFS_JSON =
            "\"columnDefs\":["
                    + "{\"name\":\"%\",\"isPercentage\":true,\"isTimestamp\":false,\"numFracDigits\":1,\"width\":\"Medium\"},"
                    + "{\"name\":\"#\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":2,\"width\":\"Long\"},"
                    + "{\"name\":\"total ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Long\"},"
                    + "{\"name\":\"avg ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"},"
                    + "{\"name\":\"min ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"},"
                    + "{\"name\":\"max ms\",\"isPercentage\":false,\"isTimestamp\":false,\"numFracDigits\":0,\"width\":\"Medium\"}"
                    + "]";

    private ExplicitTimer timer;
    private NSysMonApi sysMon;
    private ADrillDownPageDefinition page;
    private RecordingJson json;

    @BeforeEach void setUp() {
        timer = new ExplicitTimer();
        sysMon = new NSysMonImpl(TestSysMons.configBuilder().setTimer(timer).build());
        page = new ADrillDownPageDefinition();
        page.init(sysMon);
        json = new RecordingJson();
    }

    private String servedDataOf(String service) throws Exception {
        assertTrue(page.handleRestCall(service, List.of(), json.helper()));
        return json.compactJson();
    }

    /** "a" takes 4000ns and contains two serial children of 1000ns each, i.e. 2000ns of "self" time. */
    private void givenMeasurementWithSelfTime() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        final ASimpleMeasurement a = sysMon.start("a");
        timer.curNanos += 1_000;
        final ASimpleMeasurement b = sysMon.start("b");
        timer.curNanos += 1_000;
        b.finish();
        final ASimpleMeasurement c = sysMon.start("c");
        timer.curNanos += 1_000;
        c.finish();
        timer.curNanos += 1_000;
        a.finish();
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("drilldown", page.getId());
        assertEquals("Hierarchy", page.getShortLabel());
        assertEquals("Hierarchical Performance Statistics", page.getFullLabel());
        assertEquals("aggregated.html", page.getHtmlFileName());
        assertEquals("CtrlAggregated", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
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

    @Test void doClearRemovesTheMeasurements() throws Exception {
        givenMeasurementWithSelfTime();
        page.handleRestCall("doClear", List.of(), new RecordingJson().helper());

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    @Test void collectsNothingWhileStopped() throws Exception {
        sysMon.start("a").finish();

        assertTrue(servedDataOf("getData").contains("\"traces\":[]"));
    }

    @Test void writesTheRootMeasurementWithItsPercentage() throws Exception {
        givenMeasurementWithSelfTime();

        final String result = servedDataOf("getData");

        assertTrue(result.contains("\"traces\":[{\"name\":\"a\",\"data\":[100.0,1.00,0,0,0,0],"), result);
    }

    @Test void addsASelfNodeForTimeNotSpentInChildren() throws Exception {
        givenMeasurementWithSelfTime();

        final String result = servedDataOf("getData");

        assertTrue(result.contains("{\"name\":\"<self>\",\"data\":[50.0,1.00,0,0,0,0]}"), result);
    }

    @Test void sortsChildrenByDescendingTotalTime() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        final ASimpleMeasurement a = sysMon.start("a");
        final ASimpleMeasurement slow = sysMon.start("slow");
        timer.curNanos += 1_000;
        slow.finish();
        final ASimpleMeasurement fast = sysMon.start("fast");
        timer.curNanos += 100;
        fast.finish();
        timer.curNanos += 100;
        a.finish();

        final String result = servedDataOf("getData");

        assertTrue(result.indexOf("\"name\":\"slow\"") < result.indexOf("\"name\":\"fast\""), result);
    }

    @Test void sortsRootMeasurementsByDescendingTotalTime() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        final ASimpleMeasurement slow = sysMon.start("slow");
        timer.curNanos += 1_000;
        slow.finish();
        sysMon.start("fast").finish();

        final String result = servedDataOf("getData");

        assertTrue(result.indexOf("\"name\":\"slow\"") < result.indexOf("\"name\":\"fast\""), result);
    }

    @Test void breaksTiesAlphabetically() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        sysMon.start("beta").finish();
        sysMon.start("alpha").finish();

        final String result = servedDataOf("getData");

        assertTrue(result.indexOf("\"name\":\"alpha\"") < result.indexOf("\"name\":\"beta\""), result);
    }

    @Test void doesNotSubtractParallelMeasurementsFromTheSelfTime() throws Exception {
        page.handleRestCall("doStart", List.of(), new RecordingJson().helper());

        final ASimpleMeasurement a = sysMon.start("a");
        final ASimpleMeasurement parallel = sysMon.start("parallel", false);
        timer.curNanos += 1_000;
        parallel.finish();
        timer.curNanos += 1_000;
        a.finish();

        final String result = servedDataOf("getData");

        assertTrue(result.contains("{\"name\":\"<self>\",\"data\":[100.0,"), result);
        assertTrue(result.contains("\"isNotSerial\":true"), result);
    }

    @Test void startsCollectingImmediatelyIfConfiguredThatWay() throws Exception {
        final NSysMonApi initiallyStartedSysMon = new NSysMonImpl(TestSysMons.configBuilder().setTimer(timer).build());
        final ADrillDownPageDefinition initiallyStartedPage = new ADrillDownPageDefinition(true);
        initiallyStartedPage.init(initiallyStartedSysMon);

        assertTrue(initiallyStartedPage.handleRestCall("getData", List.of(), json.helper()));

        assertTrue(json.compactJson().contains("\"isStarted\":true"));
    }
}
