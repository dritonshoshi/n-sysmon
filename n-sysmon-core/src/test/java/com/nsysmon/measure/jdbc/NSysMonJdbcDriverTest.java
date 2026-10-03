package com.nsysmon.measure.jdbc;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AScalarDataPoint;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NSysMonJdbcDriverTest {

    private Properties info;

    @BeforeEach void before() {
        info = new Properties();
        info.setProperty("user", "sa");
        info.setProperty("password", "");
    }

    @AfterEach void after() {
        System.clearProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED);
        JdbcTestSupport.TestSysMonSource.sysMon = null;
        reRegisterDriver();
    }

    /**
     * The driver registers itself in its static initializer and through the JDBC service loader. The test that
     * deregisters it has to leave the driver manager in its original state, so that tests running afterwards
     * (including the pre-existing JdbcTest) still find the driver.
     */
    private void reRegisterDriver() {
        try {
            DriverManager.registerDriver(NSysMonJdbcDriver.INSTANCE);
        } catch (SQLException alreadyRegistered) {
            // nothing to do
        }
    }

    @Test void acceptsURL_returnsFalseForForeignAndNullUrls() throws SQLException {
        assertFalse(NSysMonJdbcDriver.INSTANCE.acceptsURL(null));
        assertFalse(NSysMonJdbcDriver.INSTANCE.acceptsURL("jdbc:h2:mem:demo"));
        assertFalse(NSysMonJdbcDriver.INSTANCE.acceptsURL(""));
    }

    @Test void acceptsURL_returnsTrueForNsysMonUrls() throws SQLException {
        assertTrue(NSysMonJdbcDriver.INSTANCE.acceptsURL("nsysmon::jdbc:h2:mem:demo"));
        assertTrue(NSysMonJdbcDriver.INSTANCE.acceptsURL(NSysMonJdbcDriver.URL_PREFIX));
    }

    @Test void connect_returnsNullForForeignUrl() throws SQLException {
        assertNull(NSysMonJdbcDriver.INSTANCE.connect("jdbc:h2:mem:demo", info));
    }

    @Test void connect_returnsNullWhenNoInnerUrlIsPresent() throws SQLException {
        assertNull(NSysMonJdbcDriver.INSTANCE.connect("nsysmon:noinnerurl", info));
    }

    @Test void connect_wrapsTheInnerConnection() throws SQLException {
        final Connection conn = connect(":jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        assertInstanceOf(NSysMonConnection.class, conn);
        try (Statement stmt = conn.createStatement()) {
            assertTrue(stmt.execute("select 1"));
        }
    }

    @Test void connect_passesUserAndPasswordToTheInnerDriver() throws SQLException {
        final Connection conn = connect(":jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        assertTrue("sa".equalsIgnoreCase(conn.getMetaData().getUserName()));
    }

    @Test void connect_countsTheConnectionInTheQualifiedPool() throws Exception {
        final String pool = JdbcTestSupport.uniquePoolIdentifier();
        final Connection conn = connect("qualifier" + '=' + pool + ":jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        assertEquals(1, openConnections(pool));

        conn.close();

        assertEquals(0, openConnections(pool));
    }

    @Test void connect_parsesPoolIdentifierCaseInsensitivelyAndTrimsWhitespace() throws Exception {
        final String pool = JdbcTestSupport.uniquePoolIdentifier();
        final Connection conn = connect("  QUALIFIER = " + pool + " :jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        assertEquals(1, openConnections(pool));
        conn.close();
    }

    @Test void connect_returnsUnwrappedConnectionWhenNsysMonIsGloballyDisabled() throws SQLException {
        System.setProperty(NSysMonConfig.PROPNAME_GLOBALLY_DISABLED, "true");

        final Connection conn = connect(":jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        assertFalse(conn instanceof NSysMonConnection);
        assertInstanceOf(org.h2.jdbc.JdbcConnection.class, conn);
    }

    @Test void connect_returnsUnwrappedConnectionWhenTheSysMonSourceIsUnknown() throws SQLException {
        final Connection conn = connect("nsysmon-source=no.such.SourceClass:jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        assertFalse(conn instanceof NSysMonConnection);
        assertInstanceOf(org.h2.jdbc.JdbcConnection.class, conn);
    }

    @Test void connect_usesTheSysMonInstanceProvidedByTheSource() throws SQLException {
        final CollectingDataSink dataSink = new CollectingDataSink();
        JdbcTestSupport.TestSysMonSource.sysMon = JdbcTestSupport.createSysMon(dataSink);

        final Connection conn = connect("nsysmon-source" + '='
                + JdbcTestSupport.TestSysMonSource.class.getName() + ":jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName());

        // the connection only uses the sysmon instance of the source, so the measurement of the statement
        // shows up in the data sink of that instance
        final NSysMonApi sysMon = JdbcTestSupport.TestSysMonSource.sysMon;
        sysMon.measure("outer", m -> {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("select 1");
            }
        });

        assertEquals(1, dataSink.data.size());
        assertEquals("outer", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void connect_rejectsMalformedParameters() {
        final String url = "nsysmon:qualifier:jdbc:h2:mem:" + JdbcTestSupport.uniqueDbName();

        assertThrows(IllegalArgumentException.class, () -> NSysMonJdbcDriver.INSTANCE.connect(url, info));
    }

    @Test void connect_propagatesFailuresOfTheInnerDriver() {
        final String url = NSysMonJdbcDriver.URL_PREFIX + ":jdbc:unknowndb:mem:" + JdbcTestSupport.uniqueDbName();

        assertThrows(SQLException.class, () -> NSysMonJdbcDriver.INSTANCE.connect(url, info));
    }

    @Test void getPropertyInfo_returnsNoProperties() throws SQLException {
        assertEquals(0, NSysMonJdbcDriver.INSTANCE.getPropertyInfo("nsysmon::jdbc:h2:mem:demo", info).length);
    }

    @Test void getMajorVersion_isOneAndMinorVersion_isZero() {
        assertEquals(1, NSysMonJdbcDriver.INSTANCE.getMajorVersion());
        assertEquals(0, NSysMonJdbcDriver.INSTANCE.getMinorVersion());
        assertTrue(NSysMonJdbcDriver.INSTANCE.jdbcCompliant());
    }

    @Test void getParentLogger_isNotSupported() {
        assertThrows(SQLFeatureNotSupportedException.class, () -> NSysMonJdbcDriver.INSTANCE.getParentLogger());
    }

    @Test void deregister_removesTheDriverFromTheDriverManager() throws SQLException {
        NSysMonJdbcDriver.deregister();

        assertThrows(SQLException.class, () -> DriverManager.getDriver(NSysMonJdbcDriver.URL_PREFIX + ":jdbc:h2:mem:demo"));
    }

    @Test void driverIsRegisteredWithTheDriverManager() throws SQLException {
        assertSame(NSysMonJdbcDriver.INSTANCE, DriverManager.getDriver(NSysMonJdbcDriver.URL_PREFIX + ":jdbc:h2:mem:demo"));
    }

    /** @param urlAfterPrefix the part of the url following the 'nsysmon:' prefix, i.e. parameters and inner url */
    private Connection connect(String urlAfterPrefix) throws SQLException {
        return NSysMonJdbcDriver.INSTANCE.connect(NSysMonJdbcDriver.URL_PREFIX + urlAfterPrefix, info);
    }

    private long openConnections(String pool) throws Exception {
        final Map<String, AScalarDataPoint> data = new HashMap<>();
        AConnectionCounter.getInstance().contributeMeasurements(data, 0L, new HashMap<>());
        assertTrue(data.containsKey("JDBC: Open Connections (" + pool + ')'));
        return data.get("JDBC: Open Connections (" + pool + ')').getValueRaw();
    }
}
