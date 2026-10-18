package com.nsysmon.servlet.environment;

import com.ajjpj.afoundation.collection.immutable.AList;
import com.nsysmon.NSysMonApi;
import com.nsysmon.measure.environment.AEnvironmentData;
import com.nsysmon.testutil.RecordingJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AEnvVarPageDefinitionTest {
    @Mock private NSysMonApi sysMon;

    private RecordingJson json;

    @BeforeEach void setUp() {
        json = new RecordingJson();
    }

    private static AEnvironmentData data(String path, String value) {
        final String[] segments = path.split("\\.");
        return new AEnvironmentData(AList.create(segments), value);
    }

    private void givenEnvironmentData(AEnvironmentData... data) throws Exception {
        when(sysMon.getEnvironmentMeasurements()).thenReturn(List.of(data));
    }

    private AEnvVarPageDefinition page(String unsortedPaths) {
        final AEnvVarPageDefinition result = new AEnvVarPageDefinition(unsortedPaths);
        result.init(sysMon);
        return result;
    }

    @Test void exposesItsLabelsAndFileNames() {
        final AEnvVarPageDefinition page = page("");

        assertEquals("env", page.getId());
        assertEquals("Environment", page.getShortLabel());
        assertEquals("System Environment", page.getFullLabel());
        assertEquals("envvar.html", page.getHtmlFileName());
        assertEquals("CtrlEnvVar", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page("").handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void writesAnEmptyTreeWithoutMeasurements() throws Exception {
        givenEnvironmentData();

        assertTrue(page("").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":[]}", json.compactJson());
    }

    @Test void omitsValueForNodesWithoutOne() throws Exception {
        givenEnvironmentData(data("os", null));

        assertTrue(page("").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":[{\"name\":\"os\"}]}", json.compactJson());
    }

    @Test void writesAFlatNameAsLeaf() throws Exception {
        givenEnvironmentData(data("os.name", "Linux"));

        assertTrue(page("").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":[{\"name\":\"os\",\"children\":[{\"name\":\"name\",\"value\":\"Linux\"}]}]}",
                json.compactJson());
    }

    @Test void mergesEntriesSharingAPrefix() throws Exception {
        givenEnvironmentData(data("os.name", "Linux"), data("java.version", "17"));

        assertTrue(page("").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":["
                + "{\"name\":\"java\",\"children\":[{\"name\":\"version\",\"value\":\"17\"}]},"
                + "{\"name\":\"os\",\"children\":[{\"name\":\"name\",\"value\":\"Linux\"}]}"
                + "]}", json.compactJson());
    }

    @Test void lastValueWinsForTheSamePath() throws Exception {
        givenEnvironmentData(data("os.name", "first"), data("os.name", "second"));

        assertTrue(page("").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":[{\"name\":\"os\",\"children\":[{\"name\":\"name\",\"value\":\"second\"}]}]}",
                json.compactJson());
    }

    @Test void keepsInsertionOrderBelowPathsConfiguredAsUnsorted() throws Exception {
        givenEnvironmentData(data("os.zebra", "1"), data("os.alpha", "2"));

        assertTrue(page("os.*").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":[{\"name\":\"os\",\"children\":["
                + "{\"name\":\"zebra\",\"value\":\"1\"},{\"name\":\"alpha\",\"value\":\"2\"}]}]}",
                json.compactJson());
    }

    @Test void sortsChildrenOfPathsThatAreNotConfiguredAsUnsorted() throws Exception {
        givenEnvironmentData(data("os.zebra", "1"), data("os.alpha", "2"));

        assertTrue(page("").handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"envTree\":[{\"name\":\"os\",\"children\":["
                + "{\"name\":\"alpha\",\"value\":\"2\"},{\"name\":\"zebra\",\"value\":\"1\"}]}]}",
                json.compactJson());
    }
}
