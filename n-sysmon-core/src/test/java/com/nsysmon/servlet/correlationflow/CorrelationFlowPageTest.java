package com.nsysmon.servlet.correlationflow;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CorrelationFlowPageTest {
    private NSysMonApi sysMon;
    private CorrelationFlowPage page;
    private RecordingJson json;
    private MockedStatic<NSysMon> correlationFlowEnabled;

    @BeforeEach void setUp() {
        final NSysMonApi mockApi = mock(NSysMonApi.class);
        when(mockApi.getConfig()).thenReturn(TestSysMons.config(false));
        correlationFlowEnabled = Mockito.mockStatic(NSysMon.class);
        correlationFlowEnabled.when(NSysMon::get).thenReturn(mockApi);

        sysMon = TestSysMons.create();
        page = new CorrelationFlowPage(10);
        page.init(sysMon);
        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        correlationFlowEnabled.close();
    }

    private String servedDataOf(String service) throws Exception {
        assertTrue(page.handleRestCall(service, List.of(), json.helper()));
        return json.compactJson();
    }

    private void givenFlow(String qualifier, String id, String parentId) {
        final ASimpleMeasurement measurement = sysMon.start("m");
        if (parentId == null) {
            sysMon.startFlow(new ACorrelationId(qualifier, id, null));
        } else {
            sysMon.joinFlow(new ACorrelationId(qualifier, id, parentId));
        }
        measurement.finish();
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("correlationFlow", page.getId());
        assertEquals("CorrelationFlow", page.getShortLabel());
        assertEquals("Correlation Flow", page.getFullLabel());
        assertEquals("correlationflow.html", page.getHtmlFileName());
        assertEquals("CtrlCorrelationFlow", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesAnEmptyTreeBeforeAnyFlowWasSeen() throws Exception {
        assertEquals("{\"tree\":[]}", servedDataOf("getData"));
    }

    @Test void servesARootFlowAsATreeEntry() throws Exception {
        givenFlow("rootFlow", "1", null);

        assertEquals("{\"tree\":[{\"text\":\"rootFlow\"}]}", servedDataOf("getData"));
    }

    @Test void servesNestedFlowsAsChildNodes() throws Exception {
        givenFlow("parent", "1", null);
        givenFlow("child", "2", "1");

        assertEquals("{\"tree\":[{\"text\":\"parent\",\"tags\":[\"1\"],\"nodes\":[{\"text\":\"child\"}]}]}",
                servedDataOf("getData"));
    }

    @Test void countsAllDescendantsInTheTags() throws Exception {
        givenFlow("parent", "1", null);
        givenFlow("child", "2", "1");
        givenFlow("grandChild", "3", "2");

        assertTrue(servedDataOf("getData").contains("\"tags\":[\"2\"]"), json.compactJson());
    }

    @Test void doesNotWriteTagsForLeafNodes() throws Exception {
        givenFlow("parent", "1", null);
        givenFlow("child", "2", "1");

        assertFalse(servedDataOf("getData").contains("\"text\":\"child\",\"tags\""), json.compactJson());
    }

    @Test void sortsTheTreeEntriesByQualifier() throws Exception {
        givenFlow("zebra", "1", null);
        givenFlow("alpha", "2", null);

        final String result = servedDataOf("getData");

        assertTrue(result.indexOf("\"text\":\"alpha\"") < result.indexOf("\"text\":\"zebra\""), result);
    }

    @Test void sortsTheChildNodesByQualifier() throws Exception {
        givenFlow("parent", "1", null);
        givenFlow("zebra", "2", "1");
        givenFlow("alpha", "3", "1");

        final String result = servedDataOf("getData");

        assertTrue(result.indexOf("\"text\":\"alpha\"") < result.indexOf("\"text\":\"zebra\""), result);
    }

    @Test void doClearRemovesAllFlowsAndServesTheEmptyTree() throws Exception {
        givenFlow("parent", "1", null);
        givenFlow("child", "2", "1");

        assertEquals("{\"tree\":[]}", servedDataOf("doClear"));
    }

    @Test void doClearLeavesThePageUsable() throws Exception {
        givenFlow("parent", "1", null);
        page.handleRestCall("doClear", List.of(), new RecordingJson().helper());

        givenFlow("afterClear", "3", null);

        assertEquals("{\"tree\":[{\"text\":\"afterClear\"}]}", servedDataOf("getData"));
    }
}
