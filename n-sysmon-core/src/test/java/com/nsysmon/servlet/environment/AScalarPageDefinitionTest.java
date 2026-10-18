package com.nsysmon.servlet.environment;

import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.testutil.RecordingJson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AScalarPageDefinitionTest {
    @Mock private NSysMonApi sysMon;

    private AScalarPageDefinition page;
    private RecordingJson json;

    @BeforeEach void setUp() {
        page = new AScalarPageDefinition();
        page.init(sysMon);
        json = new RecordingJson();
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("scalars", page.getId());
        assertEquals("Scalars", page.getShortLabel());
        assertEquals("Scalar Measurements", page.getFullLabel());
        assertEquals("scalars.html", page.getHtmlFileName());
        assertEquals("CtrlScalars", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void handlesGetData() throws Exception {
        when(sysMon.getScalarMeasurements()).thenReturn(Collections.emptyMap());

        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));
        assertEquals("{\"scalars\":{}}", json.compactJson());
    }

    @Test void writesOneEntryPerScalar() throws Exception {
        final Map<String, AScalarDataPoint> scalars = new LinkedHashMap<>();
        scalars.put("key1", new AScalarDataPoint(1_000L, "key1", 1_500L, 1));
        scalars.put("key2", new AScalarDataPoint(2_000L, "key2", 42L, 0));
        when(sysMon.getScalarMeasurements()).thenReturn(scalars);

        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));

        assertEquals("{\"scalars\":{"
                + "\"key1\":{\"value\":150.0,\"numFracDigits\":1},"
                + "\"key2\":{\"value\":42,\"numFracDigits\":0}"
                + "}}", json.compactJson());
    }

    @Test void usesTheScalarNameRatherThanItsMapKey() throws Exception {
        when(sysMon.getScalarMeasurements())
                .thenReturn(Collections.singletonMap("someKey", new AScalarDataPoint(1_000L, "actualName", 7L, 0)));

        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));

        assertEquals("{\"scalars\":{\"actualName\":{\"value\":7,\"numFracDigits\":0}}}", json.compactJson());
    }
}
