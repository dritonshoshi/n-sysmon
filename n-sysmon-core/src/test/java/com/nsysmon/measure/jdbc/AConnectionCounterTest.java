package com.nsysmon.measure.jdbc;

import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AConnectionCounterTest {

    private AConnectionCounter counter;
    private String pool;

    @BeforeEach void before() {
        counter = AConnectionCounter.getInstance();
        pool = JdbcTestSupport.uniquePoolIdentifier();
    }

    @Test void getInstance_returnsTheSharedSingleton() {
        assertSame(AConnectionCounter.INSTANCE, counter);
        assertSame(counter, AConnectionCounter.getInstance());
    }

    @Test void openAndActivateConnection_areCountedPerPool() throws Exception {
        counter.onOpenConnection(pool);
        counter.onOpenConnection(pool);
        counter.onActivateConnection(pool);
        counter.onOpenConnection("other-" + pool);

        Map<String, AScalarDataPoint> data = contribute();

        assertEquals(2, data.get("JDBC: Open Connections (" + pool + ')').getValueRaw());
        assertEquals(1, data.get("JDBC: Active Connections (" + pool + ')').getValueRaw());
        assertEquals(1, data.get("JDBC: Open Connections (other-" + pool + ')').getValueRaw());
    }

    @Test void closeConnection_decrementsOpenConnections() throws Exception {
        counter.onOpenConnection(pool);
        counter.onOpenConnection(pool);

        counter.onCloseConnection(pool);

        assertEquals(1, contribute().get("JDBC: Open Connections (" + pool + ')').getValueRaw());
    }

    @Test void passivateConnection_decrementsActiveConnections() throws Exception {
        counter.onOpenConnection(pool);
        counter.onActivateConnection(pool);
        counter.onActivateConnection(pool);

        counter.onPassivateConnection(pool);

        assertEquals(1, contribute().get("JDBC: Active Connections (" + pool + ')').getValueRaw());
    }

    @Test void connectionsWithoutQualifier_areCountedInTheDefaultPool() throws Exception {
        // make sure both default-pool measurements exist before taking the reference values
        counter.onOpenConnection(null);
        counter.onActivateConnection(null);
        final long openBefore = value("JDBC: Open Connections");
        final long activeBefore = value("JDBC: Active Connections");

        counter.onOpenConnection(null);
        counter.onOpenConnection(null);
        counter.onActivateConnection(null);
        counter.onPassivateConnection(null);
        counter.onCloseConnection(null);

        // the default pool is shared with other tests, so only the change caused here can be asserted
        assertEquals(openBefore + 1, value("JDBC: Open Connections"));
        assertEquals(activeBefore, value("JDBC: Active Connections"));
    }

    @Test void contributeMeasurements_keepsTimestampAndNumFracDigits() throws Exception {
        counter.onOpenConnection(pool);

        Map<String, AScalarDataPoint> data = new HashMap<>();
        counter.contributeMeasurements(data, 1234L, new HashMap<>());

        AScalarDataPoint dataPoint = data.get("JDBC: Open Connections (" + pool + ')');
        assertEquals(1234L, dataPoint.getTimestamp());
        assertEquals(0, dataPoint.getNumFracDigits());
        assertEquals("JDBC: Open Connections (" + pool + ')', dataPoint.getName());
    }

    @Test void prepareMeasurementsAndShutdown_doNotContributeAnything() throws Exception {
        Map<String, Object> mementos = new HashMap<>();

        counter.prepareMeasurements(mementos);
        counter.shutdown();

        assertTrue(mementos.isEmpty());
    }

    @Test void getTimeoutInMilliSeconds_isNone() {
        assertTrue(counter.getTimeoutInMilliSeconds().isEmpty());
    }

    @Test void getGroupnameOfMeasurement_isJdbcForJdbcMeasurements() {
        assertEquals("JDBC", counter.getGroupnameOfMeasurement("JDBC: Open Connections"));
        assertEquals("JDBC", counter.getGroupnameOfMeasurement("JDBC: Open Connections (mypool)"));
        assertNull(counter.getGroupnameOfMeasurement("CPU Load"));
        assertNull(counter.getGroupnameOfMeasurement(null));
    }

    @Test void getDescriptionOfMeasurement_describesOpenAndActiveConnections() {
        assertEquals("Open database connections.", counter.getDescriptionOfMeasurement("JDBC: Open Connections"));
        assertEquals("Open database connections.", counter.getDescriptionOfMeasurement("JDBC: Open Connections (p)"));
        assertEquals("Active database connections.", counter.getDescriptionOfMeasurement("JDBC: Active Connections"));
        assertNull(counter.getDescriptionOfMeasurement("JDBC: Something Else"));
        assertNull(counter.getDescriptionOfMeasurement(null));
    }

    @Test void isResponsibleForMeasurement_isTrueOnlyForKnownMeasurements() {
        assertTrue(counter.isResponsibleForMeasurement("JDBC: Open Connections"));
        assertFalse(counter.isResponsibleForMeasurement("JDBC: Unknown"));
        assertFalse(counter.isResponsibleForMeasurement(null));
    }

    private Map<String, AScalarDataPoint> contribute() throws Exception {
        Map<String, AScalarDataPoint> data = new HashMap<>();
        counter.contributeMeasurements(data, 0L, new HashMap<>());
        return data;
    }

    private long value(String measurement) throws Exception {
        final AScalarDataPoint dataPoint = contribute().get(measurement);
        assertTrue(dataPoint != null, "no measurement '" + measurement + '\'');
        return dataPoint.getValueRaw();
    }
}
