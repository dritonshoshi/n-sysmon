package com.nsysmon.measure.jdbc;

import com.nsysmon.NSysMonApi;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NSysMonDataSourceTest {

    @Mock private DataSource innerDataSource;
    @Mock private Connection innerConnection;

    private CollectingDataSink dataSink;
    private NSysMonApi sysMon;

    @BeforeEach void before() {
        dataSink = new CollectingDataSink();
        sysMon = JdbcTestSupport.createSysMon(dataSink);
    }

    @Test void getConnection_measuresTheTimeTheConnectionIsUsedFromThePool() throws SQLException {
        final String pool = JdbcTestSupport.uniquePoolIdentifier();
        final DataSource dataSource = new NSysMonDataSource(JdbcTestSupport.createDataSource(), pool, sysMon);

        final Connection conn = dataSource.getConnection();

        assertInstanceOf(NSysMonConnection.class, conn);
        assertEquals(1, dataSink.numStarted);
        assertTrue(dataSink.data.isEmpty());

        conn.close();

        assertEquals(1, dataSink.data.size());
        final AHierarchicalData root = dataSink.data.get(0).getRootNode();
        assertEquals("jdbc:connection from pool", root.getIdentifier());
        assertEquals(pool, root.getParameters().get("pool"));
    }

    @Test void statementsExecutedOnTheConnectionAreChildrenOfThePoolMeasurement() throws SQLException {
        final DataSource dataSource = new NSysMonDataSource(JdbcTestSupport.createDataSource(), "pool", sysMon);

        try (Connection conn = dataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("create table A (id int)");
                stmt.executeUpdate("insert into A values (1)");
            }
        }

        final AHierarchicalData root = dataSink.data.get(0).getRootNode();
        assertEquals(2, root.getChildren().size());
        assertEquals("jdbc: create table A (id int)", root.getChildren().get(0).getIdentifier());
        assertEquals("jdbc: insert into A values (1)", root.getChildren().get(1).getIdentifier());
    }

    @Test void aConnectionWithAutoCommitOnIsNeverCountedAsActive() throws Exception {
        final String pool = JdbcTestSupport.uniquePoolIdentifier();
        final DataSource dataSource = new NSysMonDataSource(JdbcTestSupport.createDataSource(), pool, sysMon);
        final Connection conn = dataSource.getConnection();

        conn.createStatement();
        conn.commit();

        // with auto commit on, the 'active / passive' heuristics do not apply, so nothing is ever activated
        assertFalse(hasActiveConnections(pool));
        conn.close();
    }

    @Test void aConnectionIsCountedAsActiveWhileItIsUsedWithoutAutoCommit() throws Exception {
        final String pool = JdbcTestSupport.uniquePoolIdentifier();
        final DataSource dataSource = new NSysMonDataSource(JdbcTestSupport.createDataSource(), pool, sysMon);
        final Connection conn = dataSource.getConnection();

        conn.setAutoCommit(false);
        conn.createStatement();
        assertEquals(1, activeConnections(pool));

        // committing passivates the connection
        conn.commit();
        assertEquals(0, activeConnections(pool));

        conn.close();
    }

    @Test void closingAnActiveConnectionPassivatesIt() throws Exception {
        final String pool = JdbcTestSupport.uniquePoolIdentifier();
        final DataSource dataSource = new NSysMonDataSource(JdbcTestSupport.createDataSource(), pool, sysMon);
        final Connection conn = dataSource.getConnection();

        conn.setAutoCommit(false);
        conn.createStatement();
        assertEquals(1, activeConnections(pool));

        conn.close();

        assertEquals(0, activeConnections(pool));
        assertEquals(0, openConnections(pool));
    }

    @Test void getConnectionWithCredentials_passesThemToTheInnerDataSource() throws SQLException {
        when(innerDataSource.getConnection("user", "password")).thenReturn(innerConnection);
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", sysMon);

        final Connection conn = dataSource.getConnection("user", "password");

        assertInstanceOf(NSysMonConnection.class, conn);
        verify(innerDataSource).getConnection("user", "password");
    }

    @Test void getConnection_propagatesFailuresOfTheInnerDataSource() throws SQLException {
        when(innerDataSource.getConnection()).thenThrow(new SQLException("no connection available"));
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", sysMon);

        assertThrows(SQLException.class, dataSource::getConnection);
        assertTrue(dataSink.data.isEmpty());
    }

    @Test void close_closesTheInnerConnectionEvenIfTheMeasurementCannotBeFinished() throws SQLException {
        final ASimpleMeasurement measurement = org.mockito.Mockito.mock(ASimpleMeasurement.class);
        final NSysMonApi failingSysMon = org.mockito.Mockito.mock(NSysMonApi.class);
        when(failingSysMon.start(anyString())).thenReturn(measurement);
        doThrow(new IllegalStateException("no measurement hierarchy")).when(measurement).finish();
        when(innerDataSource.getConnection()).thenReturn(innerConnection);
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", failingSysMon);

        dataSource.getConnection().close();

        verify(innerConnection).close();
        verify(measurement).addParameter("pool", "pool");
    }

    @Test void logWriterIsDelegatedToTheInnerDataSource() throws SQLException {
        final PrintWriter logWriter = new PrintWriter(System.out);
        when(innerDataSource.getLogWriter()).thenReturn(logWriter);
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", sysMon);

        dataSource.setLogWriter(logWriter);
        assertSame(logWriter, dataSource.getLogWriter());

        verify(innerDataSource).setLogWriter(logWriter);
        verify(innerDataSource).getLogWriter();
    }

    @Test void loginTimeoutIsDelegatedToTheInnerDataSource() throws SQLException {
        when(innerDataSource.getLoginTimeout()).thenReturn(42);
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", sysMon);

        dataSource.setLoginTimeout(42);

        assertEquals(42, dataSource.getLoginTimeout());
        verify(innerDataSource).setLoginTimeout(42);
        verify(innerDataSource).getLoginTimeout();
    }

    @Test void parentLoggerIsDelegatedToTheInnerDataSource() throws SQLException {
        final Logger logger = Logger.getLogger("test");
        when(innerDataSource.getParentLogger()).thenReturn(logger);
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", sysMon);

        assertSame(logger, dataSource.getParentLogger());
    }

    @Test void unwrapAndIsWrapperForAreDelegatedToTheInnerDataSource() throws SQLException {
        when(innerDataSource.unwrap(DataSource.class)).thenReturn(innerDataSource);
        when(innerDataSource.isWrapperFor(DataSource.class)).thenReturn(true);
        final DataSource dataSource = new NSysMonDataSource(innerDataSource, "pool", sysMon);

        assertSame(innerDataSource, dataSource.unwrap(DataSource.class));
        assertTrue(dataSource.isWrapperFor(DataSource.class));
    }

    private long openConnections(String pool) throws Exception {
        return value(pool, "Open Connections");
    }

    private long activeConnections(String pool) throws Exception {
        return value(pool, "Active Connections");
    }

    private boolean hasActiveConnections(String pool) throws Exception {
        Map<String, AScalarDataPoint> data = new HashMap<>();
        AConnectionCounter.getInstance().contributeMeasurements(data, 0L, new HashMap<>());
        return data.containsKey("JDBC: Active Connections (" + pool + ')');
    }

    /** each test uses its own pool identifier, so the counters of the shared instance are unambiguous here */
    private long value(String pool, String kind) throws Exception {
        Map<String, AScalarDataPoint> data = new HashMap<>();
        AConnectionCounter.getInstance().contributeMeasurements(data, 0L, new HashMap<>());
        final String measurement = "JDBC: " + kind + " (" + pool + ')';
        assertTrue(data.containsKey(measurement), "no measurement '" + measurement + '\'');
        return data.get(measurement).getValueRaw();
    }
}
