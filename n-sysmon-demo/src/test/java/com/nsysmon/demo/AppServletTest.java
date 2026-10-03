package com.nsysmon.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppServletTest {

    private static final String SMALL_URL = "http://localhost:8181/content/small/page.html";
    private static final String BIG_URL = "http://localhost:8181/content/big/page.html";
    private static final String CREATE_DEMO_TABLE = "create table A (oid number primary key)";

    private AppServlet servlet;
    private HttpServletResponse response;
    private StringWriter responseText;

    @BeforeEach void createServlet() throws Exception {
        servlet = new AppServlet();
        response = mock(HttpServletResponse.class);
        responseText = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseText));
    }

    private static HttpServletRequest requestFor(String url) {
        final HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(url));
        return request;
    }

    /** The generated HTML, split into lines, independent of the platform's line separator. */
    private List<String> responseLines() {
        return Arrays.asList(responseText.toString().replace("\r\n", "\n").split("\n", -1));
    }

    private static DataSource dataSourceOfTheServlet() throws Exception {
        final Field field = AppServlet.class.getDeclaredField("DATA_SOURCE");
        field.setAccessible(true);
        return (DataSource) field.get(null);
    }

    private static void executeOnTheDemoTable(String sql) throws Exception {
        try (Statement statement = AppServlet.CONN.createStatement()) {
            statement.execute(sql);
        }
        AppServlet.CONN.commit();
    }

    @Test void serviceWritesTheSmallContentPageForUrlsContainingSmall() throws Exception {
        servlet.service(requestFor(SMALL_URL), response);

        assertEquals(List.of(
                "<html><head><title>N-SysMon demo content</title></head><body>",
                "<h1>N-SysMon demo content</h1>",
                "generating small content",
                "</body></html>",
                ""), responseLines());
    }

    @Test void serviceWritesTheHugeContentPageForOtherUrls() throws Exception {
        servlet.service(requestFor(BIG_URL), response);

        assertEquals(List.of(
                "<html><head><title>N-SysMon demo content</title></head><body>",
                "<h1>N-SysMon demo content</h1>",
                "generating huge content",
                "</body></html>",
                ""), responseLines());
    }

    @Test void serviceDecidesOnTheContentSizeByTheRequestUrl() throws Exception {
        servlet.service(requestFor("http://localhost:8181/content/small"), response);
        assertTrue(responseText.toString().contains("generating small content"));

        responseText.getBuffer().setLength(0);
        servlet.service(requestFor("http://localhost:8181/content/other"), response);
        assertTrue(responseText.toString().contains("generating huge content"));
    }

    @Test void serviceFailsWhenTheRequestThreadIsInterrupted() {
        Thread.currentThread().interrupt();

        assertThrows(RuntimeException.class, () -> servlet.service(requestFor(SMALL_URL), response));
    }

    @Test void serviceFailsWhenTheDemoTableIsMissing() throws Exception {
        executeOnTheDemoTable("drop table A");
        try {
            assertThrows(RuntimeException.class, () -> servlet.service(requestFor(SMALL_URL), response));
        } finally {
            executeOnTheDemoTable(CREATE_DEMO_TABLE);
        }
    }

    @Test void staticInitializerKeepsTheDemoTableAvailable() throws Exception {
        final Connection conn = AppServlet.CONN;

        assertNotNull(conn);
        assertFalse(conn.isClosed());
        try (Statement statement = conn.createStatement(); ResultSet rs = statement.executeQuery("select count(*) from A")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test void staticInitializerCommitsTheCreatedTable() throws Exception {
        try (Statement statement = AppServlet.CONN.createStatement(); ResultSet rs = statement.executeQuery("select * from A where oid = 4711")) {
            assertFalse(rs.next());
        }
    }

    @Test void theDataSourceHandsOutMeasuringConnections() throws Exception {
        final Connection conn = dataSourceOfTheServlet().getConnection();

        assertNotNull(conn);
        assertEquals("com.nsysmon.measure.jdbc.NSysMonConnection", conn.getClass().getName());
        conn.close();
    }

    @Test void credentialBasedConnectionsYieldAWrapperAroundNothing() throws Exception {
        // The demo's inner DataSource does not implement credential based access and returns null, but
        // NSysMonDataSource wraps that null unconditionally - the caller gets a wrapper around nothing.
        final Connection conn = dataSourceOfTheServlet().getConnection("sa", "");

        assertNotNull(conn);
        assertThrows(RuntimeException.class, conn::createStatement);
    }

    @Test void theDataSourceDelegatesThePlainJdbcLogWriterHandling() throws Exception {
        final DataSource dataSource = dataSourceOfTheServlet();

        assertNull(dataSource.getLogWriter());
        dataSource.setLogWriter(null);
        dataSource.setLoginTimeout(7);
        assertEquals(0, dataSource.getLoginTimeout());
    }

    @Test void theDataSourceDelegatesTheParentLoggerLookup() throws Exception {
        assertNull(dataSourceOfTheServlet().getParentLogger());
    }

    @Test void theDataSourceDelegatesTheUnwrapContract() throws Exception {
        final DataSource dataSource = dataSourceOfTheServlet();

        assertNull(dataSource.unwrap(String.class));
        assertFalse(dataSource.isWrapperFor(String.class));
    }
}
