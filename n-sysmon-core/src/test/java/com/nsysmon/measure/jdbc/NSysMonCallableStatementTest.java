package com.nsysmon.measure.jdbc;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NSysMonCallableStatementTest {

    private static final String CALL = "call myabs(?)";

    @Mock private Connection wrappedConnection;
    @Mock private CallableStatement innerStatement;

    private CollectingDataSink dataSink;
    private NSysMonApi sysMon;
    private ASimpleMeasurement outer;
    private Connection rawConnection;
    private NSysMonCallableStatement mockStmt;

    @BeforeEach void before() throws SQLException {
        dataSink = new CollectingDataSink();
        sysMon = JdbcTestSupport.createSysMon(dataSink);
        // a collecting measurement can never be top-level: without a surrounding measurement, its data would be
        // discarded, so every test runs within one
        outer = sysMon.start("outer");
        mockStmt = new NSysMonCallableStatement(wrappedConnection, innerStatement, sysMon, CALL);

        rawConnection = JdbcTestSupport.createDataSource().getConnection();
        try (java.sql.Statement setup = rawConnection.createStatement()) {
            setup.execute("create alias myabs for \"java.lang.Math.abs(int)\"");
        }
    }

    @AfterEach void after() throws SQLException {
        outer.finish();
        rawConnection.close();
        NSysMon.get().getConfig().stopOverrideSqlParameters();
        NSysMon.get().getConfig().stopOverrideCollectTooltips();
    }

    @Test void aCallableStatementIsMeasuredLikeAPreparedStatement() throws SQLException {
        final NSysMonCallableStatement real = new NSysMonCallableStatement(wrappedConnection, rawConnection.prepareCall(CALL), sysMon, CALL);

        real.registerOutParameter(1, Types.INTEGER);
        real.setInt(1, -5);

        assertTrue(real.execute());
        assertEquals(5, real.getInt(1));
        assertEquals("jdbc: " + CALL, real.m.getIdentifier());
    }

    @Test void numericSettersAreDelegated() throws SQLException {
        final BigDecimal amount = new BigDecimal("12.34");

        mockStmt.setBoolean("p", true);
        mockStmt.setByte("p", (byte) 1);
        mockStmt.setShort("p", (short) 2);
        mockStmt.setInt("p", 3);
        mockStmt.setLong("p", 4L);
        mockStmt.setFloat("p", 5.5f);
        mockStmt.setDouble("p", 6.5);
        mockStmt.setBigDecimal("p", amount);

        verify(innerStatement).setBoolean("p", true);
        verify(innerStatement).setByte("p", (byte) 1);
        verify(innerStatement).setShort("p", (short) 2);
        verify(innerStatement).setInt("p", 3);
        verify(innerStatement).setLong("p", 4L);
        verify(innerStatement).setFloat("p", 5.5f);
        verify(innerStatement).setDouble("p", 6.5);
        verify(innerStatement).setBigDecimal("p", amount);
    }

    @Test void typedSettersAreDelegated() throws Exception {
        final Date date = Date.valueOf("2020-02-29");
        final Time time = Time.valueOf("10:20:30");
        final Timestamp timestamp = Timestamp.valueOf("2020-02-29 10:20:30");
        final byte[] bytes = {1, 2, 3};

        mockStmt.setString("p", "text");
        mockStmt.setBytes("p", bytes);
        mockStmt.setDate("p", date);
        mockStmt.setTime("p", time);
        mockStmt.setTimestamp("p", timestamp);
        mockStmt.setURL("p", new URL("http://localhost/"));
        mockStmt.setNString("p", "ntext");

        verify(innerStatement).setString("p", "text");
        verify(innerStatement).setBytes("p", bytes);
        verify(innerStatement).setDate("p", date);
        verify(innerStatement).setTime("p", time);
        verify(innerStatement).setTimestamp("p", timestamp);
        verify(innerStatement).setURL("p", new URL("http://localhost/"));
        verify(innerStatement).setNString("p", "ntext");
    }

    @Test void jdbcObjectSettersAreDelegated() throws Exception {
        final Blob blob = org.mockito.Mockito.mock(Blob.class);
        final Clob clob = org.mockito.Mockito.mock(Clob.class);
        final NClob nClob = org.mockito.Mockito.mock(NClob.class);
        final RowId rowId = org.mockito.Mockito.mock(RowId.class);
        final SQLXML sqlxml = org.mockito.Mockito.mock(SQLXML.class);

        mockStmt.setBlob("p", blob);
        mockStmt.setClob("p", clob);
        mockStmt.setNClob("p", nClob);
        mockStmt.setRowId("p", rowId);
        mockStmt.setSQLXML("p", sqlxml);

        verify(innerStatement).setBlob("p", blob);
        verify(innerStatement).setClob("p", clob);
        verify(innerStatement).setNClob("p", nClob);
        verify(innerStatement).setRowId("p", rowId);
        verify(innerStatement).setSQLXML("p", sqlxml);
    }

    @Test void nullSettersAreDelegated() throws SQLException {
        mockStmt.setNull("p", Types.INTEGER);
        mockStmt.setNull("p", Types.INTEGER, "INTEGER");

        verify(innerStatement).setNull("p", Types.INTEGER);
        verify(innerStatement).setNull("p", Types.INTEGER, "INTEGER");
    }

    @Test void objectSettersAreDelegated() throws SQLException {
        final Object value = new Object();

        mockStmt.setObject("p", value);
        mockStmt.setObject("p", value, Types.INTEGER);
        mockStmt.setObject("p", value, Types.INTEGER, 2);

        verify(innerStatement).setObject("p", value);
        verify(innerStatement).setObject("p", value, Types.INTEGER);
        verify(innerStatement).setObject("p", value, Types.INTEGER, 2);
    }

    @Test void calendarSettersAreDelegated() throws SQLException {
        final Calendar calendar = Calendar.getInstance();
        final Date date = Date.valueOf("2020-02-29");
        final Time time = Time.valueOf("10:20:30");
        final Timestamp timestamp = Timestamp.valueOf("2020-02-29 10:20:30");

        mockStmt.setDate("p", date, calendar);
        mockStmt.setTime("p", time, calendar);
        mockStmt.setTimestamp("p", timestamp, calendar);

        verify(innerStatement).setDate("p", date, calendar);
        verify(innerStatement).setTime("p", time, calendar);
        verify(innerStatement).setTimestamp("p", timestamp, calendar);
    }

    @Test void streamSettersWithLengthAreDelegated() throws SQLException {
        final ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[]{1});
        final Reader reader = new StringReader("text");

        mockStmt.setAsciiStream("p", bytes, 1);
        mockStmt.setBinaryStream("p", bytes, 1);
        mockStmt.setCharacterStream("p", reader, 4);
        mockStmt.setAsciiStream("p", bytes, 1L);
        mockStmt.setBinaryStream("p", bytes, 1L);
        mockStmt.setCharacterStream("p", reader, 4L);
        mockStmt.setNCharacterStream("p", reader, 4L);
        mockStmt.setClob("p", reader, 4L);
        mockStmt.setBlob("p", bytes, 1L);
        mockStmt.setNClob("p", reader, 4L);

        verify(innerStatement).setAsciiStream("p", bytes, 1);
        verify(innerStatement).setBinaryStream("p", bytes, 1);
        verify(innerStatement).setCharacterStream("p", reader, 4);
        verify(innerStatement).setAsciiStream("p", bytes, 1L);
        verify(innerStatement).setBinaryStream("p", bytes, 1L);
        verify(innerStatement).setCharacterStream("p", reader, 4L);
        verify(innerStatement).setNCharacterStream("p", reader, 4L);
        verify(innerStatement).setClob("p", reader, 4L);
        verify(innerStatement).setBlob("p", bytes, 1L);
        verify(innerStatement).setNClob("p", reader, 4L);
    }

    @Test void streamSettersWithoutLengthAreDelegated() throws SQLException {
        final ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[]{1});
        final Reader reader = new StringReader("text");

        mockStmt.setAsciiStream("p", bytes);
        mockStmt.setBinaryStream("p", bytes);
        mockStmt.setCharacterStream("p", reader);
        mockStmt.setNCharacterStream("p", reader);
        mockStmt.setBlob("p", bytes);
        mockStmt.setClob("p", reader);
        mockStmt.setNClob("p", reader);

        verify(innerStatement).setAsciiStream("p", bytes);
        verify(innerStatement).setBinaryStream("p", bytes);
        verify(innerStatement).setCharacterStream("p", reader);
        verify(innerStatement).setNCharacterStream("p", reader);
        verify(innerStatement).setBlob("p", bytes);
        verify(innerStatement).setClob("p", reader);
        verify(innerStatement).setNClob("p", reader);
    }

    @Test void registerOutParameterIsDelegatedForIndexes() throws SQLException {
        mockStmt.registerOutParameter(1, Types.INTEGER);
        mockStmt.registerOutParameter(1, Types.INTEGER, 2);
        mockStmt.registerOutParameter(1, Types.INTEGER, "INTEGER");

        verify(innerStatement).registerOutParameter(1, Types.INTEGER);
        verify(innerStatement).registerOutParameter(1, Types.INTEGER, 2);
        verify(innerStatement).registerOutParameter(1, Types.INTEGER, "INTEGER");
    }

    @Test void registerOutParameterIsDelegatedForNames() throws SQLException {
        mockStmt.registerOutParameter("p", Types.INTEGER);
        mockStmt.registerOutParameter("p", Types.INTEGER, 2);
        mockStmt.registerOutParameter("p", Types.INTEGER, "INTEGER");

        verify(innerStatement).registerOutParameter("p", Types.INTEGER);
        verify(innerStatement).registerOutParameter("p", Types.INTEGER, 2);
        verify(innerStatement).registerOutParameter("p", Types.INTEGER, "INTEGER");
    }

    @Test void primitiveGettersByIndexAreDelegated() throws SQLException {
        when(innerStatement.getBoolean(1)).thenReturn(true);
        when(innerStatement.getByte(1)).thenReturn((byte) 1);
        when(innerStatement.getShort(1)).thenReturn((short) 2);
        when(innerStatement.getInt(1)).thenReturn(3);
        when(innerStatement.getLong(1)).thenReturn(4L);
        when(innerStatement.getFloat(1)).thenReturn(5.5f);
        when(innerStatement.getDouble(1)).thenReturn(6.5);

        assertTrue(mockStmt.getBoolean(1));
        assertEquals((byte) 1, mockStmt.getByte(1));
        assertEquals((short) 2, mockStmt.getShort(1));
        assertEquals(3, mockStmt.getInt(1));
        assertEquals(4L, mockStmt.getLong(1));
        assertEquals(5.5f, mockStmt.getFloat(1));
        assertEquals(6.5, mockStmt.getDouble(1));
    }

    @Test void primitiveGettersByNameAreDelegated() throws SQLException {
        when(innerStatement.getBoolean("p")).thenReturn(true);
        when(innerStatement.getByte("p")).thenReturn((byte) 1);
        when(innerStatement.getShort("p")).thenReturn((short) 2);
        when(innerStatement.getInt("p")).thenReturn(3);
        when(innerStatement.getLong("p")).thenReturn(4L);
        when(innerStatement.getFloat("p")).thenReturn(5.5f);
        when(innerStatement.getDouble("p")).thenReturn(6.5);

        assertTrue(mockStmt.getBoolean("p"));
        assertEquals((byte) 1, mockStmt.getByte("p"));
        assertEquals((short) 2, mockStmt.getShort("p"));
        assertEquals(3, mockStmt.getInt("p"));
        assertEquals(4L, mockStmt.getLong("p"));
        assertEquals(5.5f, mockStmt.getFloat("p"));
        assertEquals(6.5, mockStmt.getDouble("p"));
    }

    @Test void stringAndByteGettersAreDelegated() throws SQLException {
        final byte[] bytes = {1, 2, 3};
        final Map<String, Class<?>> typeMap = new HashMap<>();
        when(innerStatement.getString(1)).thenReturn("byIndex");
        when(innerStatement.getString("p")).thenReturn("byName");
        when(innerStatement.getBytes(1)).thenReturn(bytes);
        when(innerStatement.getBytes("p")).thenReturn(bytes);

        assertEquals("byIndex", mockStmt.getString(1));
        assertEquals("byName", mockStmt.getString("p"));
        assertEquals(3, mockStmt.getBytes(1).length);
        assertEquals(3, mockStmt.getBytes("p").length);

        mockStmt.getObject(1, typeMap);
        mockStmt.getObject("p", typeMap);
        verify(innerStatement).getObject(1, typeMap);
        verify(innerStatement).getObject("p", typeMap);
    }

    @Test void bigDecimalGettersAreDelegated() throws SQLException {
        final BigDecimal amount = new BigDecimal("12.34");
        when(innerStatement.getBigDecimal(1)).thenReturn(amount);
        when(innerStatement.getBigDecimal("p")).thenReturn(amount);

        assertEquals(amount, mockStmt.getBigDecimal(1));
        assertEquals(amount, mockStmt.getBigDecimal("p"));
        mockStmt.getBigDecimal(1, 2);
        verify(innerStatement).getBigDecimal(1, 2);
    }

    @Test void dateAndTimeGettersAreDelegated() throws SQLException {
        final Date date = Date.valueOf("2020-02-29");
        final Time time = Time.valueOf("10:20:30");
        final Timestamp timestamp = Timestamp.valueOf("2020-02-29 10:20:30");
        when(innerStatement.getDate(1)).thenReturn(date);
        when(innerStatement.getTime(1)).thenReturn(time);
        when(innerStatement.getTimestamp(1)).thenReturn(timestamp);

        assertEquals(date, mockStmt.getDate(1));
        assertEquals(time, mockStmt.getTime(1));
        assertEquals(timestamp, mockStmt.getTimestamp(1));

        mockStmt.getDate("p");
        mockStmt.getTime("p");
        mockStmt.getTimestamp("p");
        verify(innerStatement).getDate("p");
        verify(innerStatement).getTime("p");
        verify(innerStatement).getTimestamp("p");
    }

    @Test void dateAndTimeGettersWithCalendarAreDelegated() throws SQLException {
        final Calendar calendar = Calendar.getInstance();

        mockStmt.getDate(1, calendar);
        mockStmt.getTime(1, calendar);
        mockStmt.getTimestamp(1, calendar);
        mockStmt.getDate("p", calendar);
        mockStmt.getTime("p", calendar);
        mockStmt.getTimestamp("p", calendar);

        verify(innerStatement).getDate(1, calendar);
        verify(innerStatement).getTime(1, calendar);
        verify(innerStatement).getTimestamp(1, calendar);
        verify(innerStatement).getDate("p", calendar);
        verify(innerStatement).getTime("p", calendar);
        verify(innerStatement).getTimestamp("p", calendar);
    }

    @Test void jdbcObjectGettersAreDelegated() throws SQLException {
        final Blob blob = org.mockito.Mockito.mock(Blob.class);
        final Clob clob = org.mockito.Mockito.mock(Clob.class);
        final Array array = org.mockito.Mockito.mock(Array.class);
        final Ref ref = org.mockito.Mockito.mock(Ref.class);
        final RowId rowId = org.mockito.Mockito.mock(RowId.class);
        final NClob nClob = org.mockito.Mockito.mock(NClob.class);
        final SQLXML sqlxml = org.mockito.Mockito.mock(SQLXML.class);
        when(innerStatement.getBlob(1)).thenReturn(blob);
        when(innerStatement.getClob(1)).thenReturn(clob);
        when(innerStatement.getArray(1)).thenReturn(array);
        when(innerStatement.getRef(1)).thenReturn(ref);
        when(innerStatement.getRowId(1)).thenReturn(rowId);
        when(innerStatement.getNClob(1)).thenReturn(nClob);
        when(innerStatement.getSQLXML(1)).thenReturn(sqlxml);

        assertEquals(blob, mockStmt.getBlob(1));
        assertEquals(clob, mockStmt.getClob(1));
        assertEquals(array, mockStmt.getArray(1));
        assertEquals(ref, mockStmt.getRef(1));
        assertEquals(rowId, mockStmt.getRowId(1));
        assertEquals(nClob, mockStmt.getNClob(1));
        assertEquals(sqlxml, mockStmt.getSQLXML(1));
    }

    @Test void jdbcObjectGettersByNameAreDelegated() throws SQLException {
        mockStmt.getBlob("p");
        mockStmt.getClob("p");
        mockStmt.getArray("p");
        mockStmt.getRef("p");
        mockStmt.getRowId("p");
        mockStmt.getNClob("p");
        mockStmt.getSQLXML("p");

        verify(innerStatement).getBlob("p");
        verify(innerStatement).getClob("p");
        verify(innerStatement).getArray("p");
        verify(innerStatement).getRef("p");
        verify(innerStatement).getRowId("p");
        verify(innerStatement).getNClob("p");
        verify(innerStatement).getSQLXML("p");
    }

    @Test void objectGettersAreDelegated() throws SQLException {
        final Object value = new Object();
        when(innerStatement.getObject(1)).thenReturn(value);
        when(innerStatement.getObject("p")).thenReturn(value);
        when(innerStatement.getObject(1, String.class)).thenReturn("byIndex");
        when(innerStatement.getObject("p", String.class)).thenReturn("byName");

        assertEquals(value, mockStmt.getObject(1));
        assertEquals(value, mockStmt.getObject("p"));
        assertEquals("byIndex", mockStmt.getObject(1, String.class));
        assertEquals("byName", mockStmt.getObject("p", String.class));
        final Map<String, Class<?>> typeMap = new HashMap<>();
        mockStmt.getObject(1, typeMap);
        mockStmt.getObject("p", typeMap);
        verify(innerStatement).getObject(1, typeMap);
        verify(innerStatement).getObject("p", typeMap);
    }

    @Test void nationalCharacterGettersAreDelegated() throws SQLException {
        when(innerStatement.getNString(1)).thenReturn("ntext");

        assertEquals("ntext", mockStmt.getNString(1));
        mockStmt.getNString("p");
        mockStmt.getNCharacterStream(1);
        mockStmt.getNCharacterStream("p");
        mockStmt.getCharacterStream(1);
        mockStmt.getCharacterStream("p");

        verify(innerStatement).getNCharacterStream(1);
        verify(innerStatement).getNCharacterStream("p");
        verify(innerStatement).getCharacterStream(1);
        verify(innerStatement).getCharacterStream("p");
    }

    @Test void urlAndWasNullAreDelegated() throws Exception {
        final URL url = new URL("http://localhost/");
        when(innerStatement.getURL(1)).thenReturn(url);
        when(innerStatement.wasNull()).thenReturn(true);

        assertEquals(url, mockStmt.getURL(1));
        assertTrue(mockStmt.wasNull());
        mockStmt.getURL("p");
        verify(innerStatement).getURL("p");
    }

    @Test void parametersAreCollectedWithTheirName() throws SQLException {
        NSysMon.get().getConfig().startOverrideSqlParameters();
        NSysMon.get().getConfig().startOverrideCollectTooltips();

        mockStmt.setInt("p", 42);

        assertEquals("42", mockStmt.m.getParameters().get("p"));
    }

    @Test void parametersOfLaterBatchesArePrefixedWithTheBatchNumber() throws SQLException {
        NSysMon.get().getConfig().startOverrideSqlParameters();
        NSysMon.get().getConfig().startOverrideCollectTooltips();

        mockStmt.addBatch();
        mockStmt.setInt("p", 42);

        assertEquals("42", mockStmt.m.getParameters().get("#1: p"));
    }

    @Test void parametersAreNotCollectedWhenTooltipsAreDisabled() throws SQLException {
        NSysMon.get().getConfig().startOverrideSqlParameters();

        mockStmt.setInt("p", 42);

        assertTrue(mockStmt.m.getParameters().isEmpty());
    }

    @Test void theResultSetAndIndexBasedStatementsAreStillAvailable() throws SQLException {
        final ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(innerStatement.executeQuery()).thenReturn(resultSet);

        assertInstanceOf(NSysMonResultSet.class, mockStmt.executeQuery());
        assertNull(mockStmt.getWarnings());
        assertFalse(mockStmt.isClosed());
        mockStmt.setInt(1, 3);
        mockStmt.executeUpdate();
        mockStmt.execute();
        mockStmt.addBatch();
        mockStmt.clearBatch();
        mockStmt.getMetaData();
        mockStmt.close();

        verify(innerStatement).setInt(1, 3);
        verify(innerStatement).executeUpdate();
        verify(innerStatement).execute();
        verify(innerStatement).addBatch();
        verify(innerStatement).clearBatch();
        verify(innerStatement).getMetaData();
        verify(innerStatement).close();
    }

    @Test void close_finishesTheCollectingMeasurement() throws SQLException {
        mockStmt.close();

        assertTrue(mockStmt.m.isFinished());
        verify(innerStatement).close();
    }
}
