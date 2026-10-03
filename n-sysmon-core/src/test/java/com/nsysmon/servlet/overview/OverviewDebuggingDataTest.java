package com.nsysmon.servlet.overview;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.presentation.APresentationPageDefinition;
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

class OverviewDebuggingDataTest {
    private NSysMonConfig config;
    private OverviewDebuggingData page;
    private RecordingJson json;
    private MockedStatic<NSysMon> nsysMonSingleton;

    @BeforeEach void setUp() {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.addPresentationMenuEntry("aMenu", List.of(new DummyPageDefinition("somePage", "Some Page")));
        config = configBuilder.build();

        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(config);
        nsysMonSingleton = Mockito.mockStatic(NSysMon.class);
        nsysMonSingleton.when(NSysMon::get).thenReturn(sysMon);

        page = new OverviewDebuggingData();
        page.init(sysMon);
        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        nsysMonSingleton.close();
    }

    private String servedDataOf(String service) throws Exception {
        assertTrue(page.handleRestCall(service, List.of(), json.helper()));
        return json.compactJson();
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("overviewDebuggingData", page.getId());
        assertEquals("Debugging", page.getShortLabel());
        assertEquals("Debugging Data", page.getFullLabel());
        assertEquals("overviewdebugging.html", page.getHtmlFileName());
        assertEquals("CtrlOverviewDebugging", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesTheConfiguredPageDefinitions() throws Exception {
        assertTrue(servedDataOf("getData").contains(
                        "\"pageDefinitions\":[{\"id\":\"somePage\",\"fullLabel\":\"Some Page\"}]"),
                json.compactJson());
    }

    @Test void servesTheNumericConfigurationParameters() throws Exception {
        final String result = servedDataOf("getData");

        assertTrue(result.contains("{\"key\":\"averagingDelayForScalarsMillis\",\"value\":1000}"), result);
        assertTrue(result.contains("{\"key\":\"dataSinkTimeoutNanos\",\"value\":100000}"), result);
    }

    @Test void servesTheBooleanConfigurationParameters() throws Exception {
        final String result = servedDataOf("getData");

        assertTrue(result.contains("{\"key\":\"collectTooltips (Configuration)\",\"value\":false}"), result);
        assertTrue(result.contains("{\"key\":\"current collectSqlParameters\",\"value\":false}"), result);
    }

    @Test void startOverrideCollectTooltipsEnablesTheCurrentFlag() throws Exception {
        assertTrue(servedDataOf("startOverrideCollectTooltips").contains(
                "{\"key\":\"current collectTooltips\",\"value\":true}"), json.compactJson());

        assertTrue(config.collectTooltips());
    }

    @Test void stopOverrideCollectTooltipsDisablesTheCurrentFlag() throws Exception {
        assertTrue(servedDataOf("stopOverrideCollectTooltips").contains(
                "{\"key\":\"current collectTooltips\",\"value\":false}"), json.compactJson());

        assertFalse(config.collectTooltips());
    }

    @Test void startOverrideSqlParametersEnablesTheCurrentFlag() throws Exception {
        assertTrue(servedDataOf("startOverrideSqlParameters").contains(
                "{\"key\":\"current collectSqlParameters\",\"value\":true}"), json.compactJson());

        assertTrue(config.collectSqlParameters());
    }

    @Test void stopOverrideSqlParametersDisablesTheCurrentFlag() throws Exception {
        assertTrue(servedDataOf("stopOverrideSqlParameters").contains(
                "{\"key\":\"current collectSqlParameters\",\"value\":false}"), json.compactJson());

        assertFalse(config.collectSqlParameters());
    }

    @Test void theOverrideFlagsDoNotChangeTheConfiguredValues() throws Exception {
        page.handleRestCall("startOverrideCollectTooltips", List.of(), new RecordingJson().helper());

        assertTrue(servedDataOf("getData").contains(
                "{\"key\":\"collectTooltips (Configuration)\",\"value\":false}"), json.compactJson());
    }

    /** Only the labels are relevant for the debugging page, all other methods are never called. */
    private static class DummyPageDefinition implements APresentationPageDefinition {
        private final String id;
        private final String fullLabel;

        DummyPageDefinition(String id, String fullLabel) {
            this.id = id;
            this.fullLabel = fullLabel;
        }

        @Override public String getId() {
            return id;
        }

        @Override public String getShortLabel() {
            return fullLabel;
        }

        @Override public String getFullLabel() {
            return fullLabel;
        }

        @Override public String getHtmlFileName() {
            return id + ".html";
        }

        @Override public String getControllerName() {
            return "Ctrl" + id;
        }

        @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) {
            return false;
        }

        @Override public void init(NSysMonApi sysMon) {
        }
    }
}
