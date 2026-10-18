package com.nsysmon.servlet.timedscalars;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.scalar.AScalarMeasurer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimedScalarsPageDefinitionTest {

    private static final String ALPHA = "alpha";
    private static final String BETA = "beta";
    private static final String UNKNOWN = "unknown";
    private static final long TEN_MINUTES_MILLIS = 10 * 60 * 1000L;

    /**
     * Knows about exactly one measurement and contributes metadata only. Leaving the contribution empty keeps the
     * engine's own scheduled collection thread from adding data points that the assertions would have to account for.
     */
    private static final class MetadataMeasurer implements AScalarMeasurer {
        private final String name;
        private final String group;
        private final String description;

        MetadataMeasurer(String name, String group, String description) {
            this.name = name;
            this.group = group;
            this.description = description;
        }

        @Override public void contributeMeasurements(Map<String, AScalarDataPoint> data, long timestamp, Map<String, Object> mementos) {
        }

        @Override public AOption<Long> getTimeoutInMilliSeconds() {
            return AOption.none();
        }

        @Override public void shutdown() {
        }

        @Override public String getGroupnameOfMeasurement(String measurement) {
            return name.equals(measurement) ? group : null;
        }

        @Override public String getDescriptionOfMeasurement(String measurement) {
            return name.equals(measurement) ? description : null;
        }

        @Override public boolean isResponsibleForMeasurement(String key) {
            return name.equals(key);
        }
    }

    private TimedScalarsPageDefinition pageDefinition;
    private ByteArrayOutputStream jsonBuffer;
    private long now;

    @BeforeEach void createPageDefinition() throws Exception {
        final NSysMonConfigBuilder configBuilder = new NSysMonConfigBuilder(new ADefaultApplicationInfoProvider("dummy", "version"));
        configBuilder.addScalarTimedMeasurer(new MetadataMeasurer(ALPHA, "Group A", "The alpha measurement"));
        configBuilder.addScalarTimedMeasurer(new MetadataMeasurer(BETA, "Group B", null));
        final NSysMonApi sysMon = new NSysMonImpl(configBuilder.build());

        now = System.currentTimeMillis();
        sysMon.addTimedScalarMeasurement(
                new AScalarDataPoint(now - TEN_MINUTES_MILLIS, ALPHA, 42, 0),
                new AScalarDataPoint(now, ALPHA, 43_000, 3),
                new AScalarDataPoint(now, BETA, 7, 0),
                new AScalarDataPoint(now, UNKNOWN, 1, 0));

        pageDefinition = new TimedScalarsPageDefinition();
        pageDefinition.init(sysMon);
        jsonBuffer = new ByteArrayOutputStream();
    }

    private AJsonSerHelperForNSysmon newJson() {
        return new AJsonSerHelperForNSysmon(jsonBuffer);
    }

    private String json() {
        return jsonBuffer.toString(StandardCharsets.UTF_8);
    }

    /** A rendered listing entry. The JSON helper terminates every object with a line break. */
    /** Everything up to (and excluding) the closing brace of one rendered timed scalar. */
    private static String metaInfosOf(String key, String description, String group, String status) {
        final String descriptionPart = description == null ? "" : "\"description\":\"" + description + "\",";
        return "{\"key\":\"" + key + "\"," + descriptionPart + "\"group\":\"" + group + "\",\"status\":\"" + status + "\"";
    }

    private static String listingEntry(String key, String description, String group, String status, boolean selected) {
        return metaInfosOf(key, description, group, status) + ",\"selected\":" + selected + "}\n";
    }

    /** A rendered graph entry. Values are separated by the comma the JSON helper writes before each new value. */
    private static String graphEntry(String key, String description, String group, String status, String... values) {
        return metaInfosOf(key, description, group, status) + ",\"values\":[" + String.join(",", values) + "]}\n";
    }

    private static String valueOf(long timestamp, String value) {
        return "{\"x\":" + timestamp + ",\"y\":" + value + "}\n";
    }

    @Test void identifiesItselfAsTheTimedScalarsPage() {
        assertEquals("timedScalars", pageDefinition.getId());
        assertEquals("TimedScalars", pageDefinition.getShortLabel());
        assertEquals("Timed Scalar Measurements", pageDefinition.getFullLabel());
        assertEquals("timedscalars.html", pageDefinition.getHtmlFileName());
        assertEquals("CtrlTimedScalars", pageDefinition.getControllerName());
    }

    @Test void getDataListsEveryTimedScalarWithItsMetainfos() throws IOException {
        pageDefinition.handleRestCall("getData", List.of(ALPHA), newJson());

        assertEquals("{\"timedScalars\":{\"" + ALPHA + "\":" + listingEntry(ALPHA, "The alpha measurement", "Group A", "RUNNING", true)
                + ",\"" + BETA + "\":" + listingEntry(BETA, null, "Group B", "RUNNING", false)
                + ",\"" + UNKNOWN + "\":" + listingEntry(UNKNOWN, null, "other", "UNKNOWN", false)
                + "}\n}\n", json());
    }

    @Test void getDataMarksEveryRequestedKeyAsSelected() throws IOException {
        pageDefinition.handleRestCall("getData", List.of(BETA, UNKNOWN), newJson());

        assertTrue(json().contains("\"" + BETA + "\":" + listingEntry(BETA, null, "Group B", "RUNNING", true)), json());
        assertTrue(json().contains("\"" + UNKNOWN + "\":" + listingEntry(UNKNOWN, null, "other", "UNKNOWN", true)), json());
        assertFalse(json().contains("\"" + ALPHA + "\":" + listingEntry(ALPHA, "The alpha measurement", "Group A", "RUNNING", true)), json());

    }

    @Test void getDataWithoutSelectionMarksNothingAsSelected() throws IOException {
        pageDefinition.handleRestCall("getData", List.of(), newJson());

        assertTrue(json().contains("\"selected\":false}"), json());
        assertFalse(json().contains("\"selected\":true}"), json());
    }

    @Test void getGraphDataWritesAllValuesOfTheSelectedScalar() throws IOException {
        pageDefinition.handleRestCall("getGraphData", List.of(ALPHA), newJson());

        assertEquals("[" + graphEntry(ALPHA, "The alpha measurement", "Group A", "RUNNING",
                valueOf(now - TEN_MINUTES_MILLIS, "42"), valueOf(now, "43.000")) + "]", json());
    }

    @Test void getGraphDataHandlesACommaSeparatedSelection() throws IOException {
        pageDefinition.handleRestCall("getGraphData", List.of(ALPHA + "," + BETA), newJson());

        assertTrue(json().contains(metaInfosOf(ALPHA, "The alpha measurement", "Group A", "RUNNING") + ",\"values\":["), json());
        assertTrue(json().contains(metaInfosOf(BETA, null, "Group B", "RUNNING") + ",\"values\":["), json());
        assertFalse(json().contains("\"key\":\"" + UNKNOWN + "\""), json());
    }

    @Test void getGraphDataMatchesKeysIgnoringCase() throws IOException {
        pageDefinition.handleRestCall("getGraphData", List.of("ALPHA"), newJson());

        assertTrue(json().contains("\"key\":\"" + ALPHA + "\""), json());
    }

    @Test void getGraphDataDecodesUrlEncodedSelection() throws Exception {
        final NSysMonConfigBuilder configBuilder = new NSysMonConfigBuilder(new ADefaultApplicationInfoProvider("dummy", "version"));
        configBuilder.addScalarTimedMeasurer(new MetadataMeasurer("alpha beta", "Group S", "Spaced name"));
        final NSysMonApi sysMon = new NSysMonImpl(configBuilder.build());
        sysMon.addTimedScalarMeasurement(new AScalarDataPoint(now, "alpha beta", 5, 0));
        final TimedScalarsPageDefinition definition = new TimedScalarsPageDefinition();
        definition.init(sysMon);

        definition.handleRestCall("getGraphData", List.of("alpha+beta"), newJson());

        assertTrue(json().contains("\"key\":\"alpha beta\""), json());
    }

    @Test void getGraphDataWritesAnEmptyArrayForUnknownKeys() throws IOException {
        pageDefinition.handleRestCall("getGraphData", List.of("no-such-scalar"), newJson());

        assertEquals("[]", json());
    }

    @Test void getGraphDataWithoutParametersWritesNothing() throws IOException {
        pageDefinition.handleRestCall("getGraphData", List.of(), newJson());
        assertEquals("", json());

        jsonBuffer.reset();
        pageDefinition.handleRestCall("getGraphData", null, newJson());
        assertEquals("", json());
    }

    @Test void getLatestGraphDataSkipsDataPointsOutsideTheRequestedWindow() throws IOException {
        pageDefinition.handleRestCall("getLatestGraphData", List.of(ALPHA, "5"), newJson());

        assertEquals("[" + graphEntry(ALPHA, "The alpha measurement", "Group A", "RUNNING", valueOf(now, "43.000")) + "]", json());
    }

    @Test void getLatestGraphDataKeepsAllDataPointsOfAWideWindow() throws IOException {
        pageDefinition.handleRestCall("getLatestGraphData", List.of(ALPHA, "60"), newJson());

        assertTrue(json().contains(valueOf(now - TEN_MINUTES_MILLIS, "42")), json());
        assertTrue(json().contains(valueOf(now, "43.000")), json());
    }

    @Test void getLatestGraphDataWithoutParametersWritesNothing() throws IOException {
        pageDefinition.handleRestCall("getLatestGraphData", List.of(), newJson());
        assertEquals("", json());

        jsonBuffer.reset();
        pageDefinition.handleRestCall("getLatestGraphData", null, newJson());
        assertEquals("", json());
    }

    @Test void restartTimedScalarRerendersTheListingOfAllTimedScalars() throws IOException {
        pageDefinition.handleRestCall("restartTimedScalar", List.of(ALPHA), newJson());

        assertTrue(json().contains("{\"timedScalars\":{"), json());
        assertTrue(json().contains("\"" + ALPHA + "\":" + listingEntry(ALPHA, "The alpha measurement", "Group A", "RUNNING", true)), json());
    }

    @Test void restartTimedScalarAcceptsKeysNobodyIsResponsibleFor() throws IOException {
        pageDefinition.handleRestCall("restartTimedScalar", List.of("no-such-scalar"), newJson());

        assertTrue(json().contains("{\"timedScalars\":{"), json());
        assertTrue(json().contains("\"selected\":false}"), json());
    }

    @Test void getDataForExportWritesEveryTimedScalarAsAGraphEntry() throws IOException {
        pageDefinition.getDataForExport(jsonBuffer);

        assertEquals("[" + graphEntry(ALPHA, "The alpha measurement", "Group A", "RUNNING",
                valueOf(now - TEN_MINUTES_MILLIS, "42"), valueOf(now, "43.000"))
                + "," + graphEntry(BETA, null, "Group B", "RUNNING", valueOf(now, "7"))
                + "," + graphEntry(UNKNOWN, null, "other", "UNKNOWN", valueOf(now, "1"))
                + "]", json());
    }

    @Test void unknownServicesAreNotHandled() throws IOException {
        assertFalse(pageDefinition.handleRestCall("noSuchService", List.of(), newJson()));
        assertEquals("", json());
    }
}
