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
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NSysMonPreparedStatementTest {

    private static final String INSERT = "insert into T values (?, ?)";
    private static final String SELECT = "select id, name from T";

    @Mock private Connection wrappedConnection;
    @Mock private PreparedStatement innerStatement;

    private CollectingDataSink dataSink;
    private NSysMonApi sysMon;
    private ASimpleMeasurement outer;
    private Connection rawConnection;
    private NSysMonPreparedStatement realStmt;
    private NSysMonPreparedStatement realQueryStmt;
    private NSysMonPreparedStatement mockStmt;

    @BeforeEach void before() throws SQLException {
        dataSink = new CollectingDataSink();
        sysMon = JdbcTestSupport.createSysMon(dataSink);
        // a collecting measurement can never be top-level: without a surrounding measurement, its data would be
        // discarded, so every test runs within one
        outer = sysMon.start("outer");
        mockStmt = new NSysMonPreparedStatement(wrappedConnection, innerStatement, sysMon, INSERT);

        rawConnection = JdbcTestSupport.createDataSource().getConnection();
        try (java.sql.Statement setup = rawConnection.createStatement()) {
            setup.execute("create table T (id int, name varchar(20))");
        }
        realStmt = new NSysMonPreparedStatement(wrappedConnection, rawConnection.prepareStatement(INSERT), sysMon, INSERT);
        realQueryStmt = new NSysMonPreparedStatement(wrappedConnection, rawConnection.prepareStatement(SELECT), sysMon, SELECT);
    }

    @AfterEach void after() throws SQLException {
        outer.finish();
        rawConnection.close();
        NSysMon.get().getConfig().stopOverrideSqlParameters();
        NSysMon.get().getConfig().stopOverrideCollectTooltips();
    }

    @Test void preparingAStatementStartsACollectingMeasurementForTheSql() {
        assertEquals("jdbc: " + INSERT, realStmt.m.getIdentifier());
    }

    @Test void close_finishesTheCollectingMeasurement() throws SQLException {
        realStmt.close();

        assertTrue(realStmt.m.isFinished());
    }

    @Test void executeQuery_wrapsTheResultSetAndRecordsTheExecuteDetail() throws SQLException {
        final ResultSet rs = realQueryStmt.executeQuery();

        assertInstanceOf(NSysMonResultSet.class, rs);
        assertEquals(1, realQueryStmt.m.getDetails().get("execute").getNum());
    }

    @Test void executeUpdate_recordsTheExecuteDetail() throws SQLException {
        realStmt.setInt(1, 1);
        realStmt.setString(2, "one");

        assertEquals(1, realStmt.executeUpdate());

        assertEquals(1, realStmt.m.getDetails().get("execute").getNum());
    }

    @Test void execute_recordsTheExecuteDetail() throws SQLException {
        realStmt.setInt(1, 1);
        realStmt.setString(2, "one");

        assertFalse(realStmt.execute());

        assertEquals(1, realStmt.m.getDetails().get("execute").getNum());
    }

    @Test void executeBatch_isMeasuredAndCountsTheBatches() throws SQLException {
        realStmt.setInt(1, 1);
        realStmt.setString(2, "one");
        realStmt.addBatch();
        realStmt.setInt(1, 2);
        realStmt.setString(2, "two");
        realStmt.addBatch();

        assertEquals(2, realStmt.batchCount);
        assertEquals(2, realStmt.executeBatch().length);
        assertEquals(1, realStmt.m.getDetails().get("execute").getNum());
    }

    @Test void addBatch_countsTheBatches() throws SQLException {
        realStmt.setInt(1, 1);
        realStmt.setString(2, "one");
        realStmt.addBatch();
        realStmt.setInt(1, 2);
        realStmt.setString(2, "two");
        realStmt.addBatch();

        assertEquals(2, realStmt.batchCount);
    }

    @Test void parametersAreCollectedWhenTooltipsAndSqlParametersAreEnabled() throws SQLException {
        collectSqlParameters(true, true);

        realStmt.setInt(1, 42);
        realStmt.setString(2, "text");

        assertEquals("42", realStmt.m.getParameters().get("1"));
        assertEquals("'text'", realStmt.m.getParameters().get("2"));
    }

    @Test void nullParametersAreCollectedAsNull() throws SQLException {
        collectSqlParameters(true, true);

        mockStmt.setNull(3, Types.INTEGER);

        assertEquals("null", mockStmt.m.getParameters().get("3"));
    }

    @Test void parametersAreNotCollectedWhenTooltipsAreDisabled() throws SQLException {
        collectSqlParameters(true, false);

        realStmt.setInt(1, 42);

        assertTrue(realStmt.m.getParameters().isEmpty());
    }

    @Test void parametersAreNotCollectedWhenSqlParametersAreDisabled() throws SQLException {
        collectSqlParameters(false, true);

        realStmt.setInt(1, 42);

        assertTrue(realStmt.m.getParameters().isEmpty());
    }

    @Test void parametersOfLaterBatchesArePrefixedWithTheBatchNumber() throws SQLException {
        collectSqlParameters(true, true);

        mockStmt.addBatch();
        mockStmt.setInt(1, 42);

        assertEquals("42", mockStmt.m.getParameters().get("#1:1"));
    }

    @Test void parameterNamesBeyondTheLookupTableAreThePlainIndex() throws SQLException {
        collectSqlParameters(true, true);

        mockStmt.setInt(21, 42);

        assertEquals("42", mockStmt.m.getParameters().get("21"));
    }

    @Test void parameterNamesOfBatchesBeyondTheLookupTableArePrefixed() throws SQLException {
        collectSqlParameters(true, true);

        for (int i = 0; i <= 50; i++) {
            mockStmt.addBatch();
        }
        mockStmt.setInt(1, 42);

        assertEquals("42", mockStmt.m.getParameters().get("#51:1"));
    }

    @Test void numericSettersAreDelegated() throws SQLException {
        mockStmt.setNull(1, Types.INTEGER);
        mockStmt.setBoolean(1, true);
        mockStmt.setByte(1, (byte) 1);
        mockStmt.setShort(1, (short) 2);
        mockStmt.setInt(1, 3);
        mockStmt.setLong(1, 4L);
        mockStmt.setFloat(1, 5.5f);
        mockStmt.setDouble(1, 6.5);

        verify(innerStatement).setNull(1, Types.INTEGER);
        verify(innerStatement).setBoolean(1, true);
        verify(innerStatement).setByte(1, (byte) 1);
        verify(innerStatement).setShort(1, (short) 2);
        verify(innerStatement).setInt(1, 3);
        verify(innerStatement).setLong(1, 4L);
        verify(innerStatement).setFloat(1, 5.5f);
        verify(innerStatement).setDouble(1, 6.5);
    }

    @Test void dateAndTextSettersAreDelegated() throws SQLException {
        final Date date = Date.valueOf("2020-02-29");
        final Time time = Time.valueOf("10:20:30");
        final Timestamp timestamp = Timestamp.valueOf("2020-02-29 10:20:30");
        final BigDecimal amount = new BigDecimal("12.34");
        final byte[] bytes = {1, 2, 3};

        mockStmt.setBigDecimal(1, amount);
        mockStmt.setString(1, "seven");
        mockStmt.setBytes(1, bytes);
        mockStmt.setDate(1, date);
        mockStmt.setTime(1, time);
        mockStmt.setTimestamp(1, timestamp);

        verify(innerStatement).setBigDecimal(1, amount);
        verify(innerStatement).setString(1, "seven");
        verify(innerStatement).setBytes(1, bytes);
        verify(innerStatement).setDate(1, date);
        verify(innerStatement).setTime(1, time);
        verify(innerStatement).setTimestamp(1, timestamp);
    }

    @Test void objectSettersAreDelegated() throws Exception {
        final Blob blob = org.mockito.Mockito.mock(Blob.class);
        final Clob clob = org.mockito.Mockito.mock(Clob.class);
        final Array array = org.mockito.Mockito.mock(Array.class);
        final Ref ref = org.mockito.Mockito.mock(Ref.class);
        final RowId rowId = org.mockito.Mockito.mock(RowId.class);
        final SQLXML sqlxml = org.mockito.Mockito.mock(SQLXML.class);
        final NClob nClob = org.mockito.Mockito.mock(NClob.class);
        final URL url = new URL("http://localhost/");

        mockStmt.setBlob(1, blob);
        mockStmt.setClob(1, clob);
        mockStmt.setArray(1, array);
        mockStmt.setRef(1, ref);
        mockStmt.setRowId(1, rowId);
        mockStmt.setSQLXML(1, sqlxml);
        mockStmt.setNClob(1, nClob);
        mockStmt.setNString(1, "text");
        mockStmt.setURL(1, url);

        verify(innerStatement).setBlob(1, blob);
        verify(innerStatement).setClob(1, clob);
        verify(innerStatement).setArray(1, array);
        verify(innerStatement).setRef(1, ref);
        verify(innerStatement).setRowId(1, rowId);
        verify(innerStatement).setSQLXML(1, sqlxml);
        verify(innerStatement).setNClob(1, nClob);
        verify(innerStatement).setNString(1, "text");
        verify(innerStatement).setURL(1, url);
    }

    @Test void streamSettersWithLengthAreDelegated() throws SQLException {
        final ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[]{1});
        final Reader reader = new StringReader("text");

        mockStmt.setAsciiStream(1, bytes, 1);
        mockStmt.setBinaryStream(1, bytes, 1);
        mockStmt.setCharacterStream(1, reader, 4);
        mockStmt.setUnicodeStream(1, bytes, 1);
        mockStmt.setAsciiStream(1, bytes, 1L);
        mockStmt.setBinaryStream(1, bytes, 1L);
        mockStmt.setCharacterStream(1, reader, 4L);
        mockStmt.setNCharacterStream(1, reader, 4L);
        mockStmt.setClob(1, reader, 4L);
        mockStmt.setBlob(1, bytes, 1L);
        mockStmt.setNClob(1, reader, 4L);

        verify(innerStatement).setAsciiStream(1, bytes, 1);
        verify(innerStatement).setBinaryStream(1, bytes, 1);
        verify(innerStatement).setCharacterStream(1, reader, 4);
        verify(innerStatement).setUnicodeStream(1, bytes, 1);
        verify(innerStatement).setAsciiStream(1, bytes, 1L);
        verify(innerStatement).setBinaryStream(1, bytes, 1L);
        verify(innerStatement).setCharacterStream(1, reader, 4L);
        verify(innerStatement).setNCharacterStream(1, reader, 4L);
        verify(innerStatement).setClob(1, reader, 4L);
        verify(innerStatement).setBlob(1, bytes, 1L);
        verify(innerStatement).setNClob(1, reader, 4L);
    }

    @Test void streamSettersWithoutLengthAreDelegated() throws SQLException {
        final ByteArrayInputStream bytes = new ByteArrayInputStream(new byte[]{1});
        final Reader reader = new StringReader("text");

        mockStmt.setAsciiStream(1, bytes);
        mockStmt.setBinaryStream(1, bytes);
        mockStmt.setCharacterStream(1, reader);
        mockStmt.setNCharacterStream(1, reader);
        mockStmt.setBlob(1, bytes);
        mockStmt.setClob(1, reader);
        mockStmt.setNClob(1, reader);

        verify(innerStatement).setAsciiStream(1, bytes);
        verify(innerStatement).setBinaryStream(1, bytes);
        verify(innerStatement).setCharacterStream(1, reader);
        verify(innerStatement).setNCharacterStream(1, reader);
        verify(innerStatement).setBlob(1, bytes);
        verify(innerStatement).setClob(1, reader);
        verify(innerStatement).setNClob(1, reader);
    }

    @Test void calendarAndObjectSettersAreDelegated() throws SQLException {
        final Calendar calendar = Calendar.getInstance();
        final Date date = Date.valueOf("2020-02-29");
        final Time time = Time.valueOf("10:20:30");
        final Timestamp timestamp = Timestamp.valueOf("2020-02-29 10:20:30");
        final Object value = new Object();

        mockStmt.setDate(1, date, calendar);
        mockStmt.setTime(1, time, calendar);
        mockStmt.setTimestamp(1, timestamp, calendar);
        mockStmt.setNull(1, Types.INTEGER, "INTEGER");
        mockStmt.setObject(1, value);
        mockStmt.setObject(1, value, Types.INTEGER);
        mockStmt.setObject(1, value, Types.INTEGER, 2);

        verify(innerStatement).setDate(1, date, calendar);
        verify(innerStatement).setTime(1, time, calendar);
        verify(innerStatement).setTimestamp(1, timestamp, calendar);
        verify(innerStatement).setNull(1, Types.INTEGER, "INTEGER");
        verify(innerStatement).setObject(1, value);
        verify(innerStatement).setObject(1, value, Types.INTEGER);
        verify(innerStatement).setObject(1, value, Types.INTEGER, 2);
    }

    @Test void metaDataAndWrapperMethodsAreDelegated() throws SQLException {
        final ResultSetMetaData resultSetMetaData = org.mockito.Mockito.mock(ResultSetMetaData.class);
        final java.sql.ParameterMetaData parameterMetaData = org.mockito.Mockito.mock(java.sql.ParameterMetaData.class);
        when(innerStatement.getMetaData()).thenReturn(resultSetMetaData);
        when(innerStatement.getParameterMetaData()).thenReturn(parameterMetaData);
        when(innerStatement.unwrap(PreparedStatement.class)).thenReturn(innerStatement);
        when(innerStatement.isWrapperFor(PreparedStatement.class)).thenReturn(true);

        assertSame(resultSetMetaData, mockStmt.getMetaData());
        assertSame(parameterMetaData, mockStmt.getParameterMetaData());
        assertSame(innerStatement, mockStmt.unwrap(PreparedStatement.class));
        assertTrue(mockStmt.isWrapperFor(PreparedStatement.class));
    }

    @Test void clearParametersAndCloseAreDelegated() throws SQLException {
        mockStmt.clearParameters();
        mockStmt.close();

        verify(innerStatement).clearParameters();
        verify(innerStatement).close();
        assertTrue(mockStmt.m.isFinished());
    }

    @Test void executeQuery_wrapsTheResultSetOfTheInnerStatement() throws SQLException {
        final ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
        when(innerStatement.executeQuery()).thenReturn(resultSet);

        final ResultSet wrapped = mockStmt.executeQuery();

        assertInstanceOf(NSysMonResultSet.class, wrapped);
        assertSame(mockStmt, wrapped.getStatement());
    }

    @Test void executeUpdateExecuteAndExecuteBatchAreDelegatedToTheInnerStatement() throws SQLException {
        when(innerStatement.executeUpdate()).thenReturn(3);
        when(innerStatement.execute()).thenReturn(true);
        when(innerStatement.executeBatch()).thenReturn(new int[]{1});

        assertEquals(3, mockStmt.executeUpdate());
        assertTrue(mockStmt.execute());
        assertEquals(1, mockStmt.executeBatch().length);
    }

    @Test void aFailedStatementExecutionIsPropagatedAndStillRecordsTheDetail() throws SQLException {
        when(innerStatement.executeUpdate()).thenThrow(new SQLException("constraint violated"));

        assertThrows(SQLException.class, () -> mockStmt.executeUpdate());

        assertEquals(1, mockStmt.m.getDetails().get("execute").getNum());
    }

    @Test void inheritedStatementMethodsAreStillAvailable() throws SQLException {
        final SQLWarning warning = new SQLWarning("careful");
        when(innerStatement.getWarnings()).thenReturn(warning);
        when(innerStatement.isClosed()).thenReturn(true);

        assertSame(warning, mockStmt.getWarnings());
        assertTrue(mockStmt.isClosed());
        assertSame(wrappedConnection, mockStmt.getConnection());
    }

    private void collectSqlParameters(boolean sqlParameters, boolean tooltips) {
        final NSysMonApi singleton = NSysMon.get();
        if (sqlParameters) {
            singleton.getConfig().startOverrideSqlParameters();
        }
        else {
            singleton.getConfig().stopOverrideSqlParameters();
        }
        if (tooltips) {
            singleton.getConfig().startOverrideCollectTooltips();
        }
        else {
            singleton.getConfig().stopOverrideCollectTooltips();
        }
    }

}
