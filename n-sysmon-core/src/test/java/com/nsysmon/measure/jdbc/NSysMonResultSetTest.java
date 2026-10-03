package com.nsysmon.measure.jdbc;

import com.nsysmon.NSysMonApi;
import com.nsysmon.measure.ACollectingMeasurement;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NSysMonResultSetTest {

    private static final String SELECT = "select id, name, amount, created, ts, tm, data from T";

    @Mock private Statement wrappedStatement;
    @Mock private ResultSet innerResultSet;

    private CollectingDataSink dataSink;
    private NSysMonApi sysMon;
    private ASimpleMeasurement outer;
    private ACollectingMeasurement m;
    private Connection rawConnection;
    private Statement innerStatement;
    private NSysMonResultSet rs;
    private NSysMonResultSet mockRs;

    @BeforeEach void before() throws SQLException {
        dataSink = new CollectingDataSink();
        sysMon = JdbcTestSupport.createSysMon(dataSink);
        // a collecting measurement can never be top-level: without a surrounding measurement, its data would be
        // discarded, so every test runs within one
        outer = sysMon.start("outer");
        m = sysMon.startCollectingMeasurement("jdbc: " + SELECT);
        mockRs = new NSysMonResultSet(innerResultSet, wrappedStatement, m);

        rawConnection = JdbcTestSupport.createDataSource().getConnection();
        try (Statement setup = rawConnection.createStatement()) {
            setup.execute("create table T (id int primary key, name varchar(20), amount decimal(10,2), created date,"
                    + " ts timestamp, tm time, data varbinary(20))");
            setup.execute("insert into T values (1, 'one', 12.34, DATE '2020-02-29',"
                    + " TIMESTAMP '2020-02-29 10:20:30', TIME '10:20:30', X'0102')");
            setup.execute("insert into T values (2, 'two', 56.78, DATE '2021-03-01',"
                    + " TIMESTAMP '2021-03-01 11:21:31', TIME '11:21:31', X'0304')");
        }
        innerStatement = rawConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        rs = new NSysMonResultSet(innerStatement.executeQuery(SELECT), wrappedStatement, m);
    }

    @AfterEach void after() throws SQLException {
        outer.finish();
        rawConnection.close();
    }

    @Test void getStatement_returnsTheStatementTheResultSetBelongsTo() throws SQLException {
        assertSame(wrappedStatement, rs.getStatement());
    }

    @Test void next_isRecordedAsResultSetAccess() throws SQLException {
        assertTrue(rs.next());
        assertTrue(rs.next());
        assertFalse(rs.next());

        assertEquals(3, m.getDetails().get("result set access").getNum());
    }

    @Test void beforeFirstAndAfterLastAreRecordedAsResultSetAccess() throws SQLException {
        rs.beforeFirst();
        assertTrue(rs.isBeforeFirst());

        rs.afterLast();
        assertTrue(rs.isAfterLast());
        assertEquals(2, m.getDetails().get("result set access").getNum());
    }

    @Test void scrollingIsRecordedAsResultSetAccess() throws SQLException {
        assertTrue(rs.first());
        assertTrue(rs.isFirst());
        assertTrue(rs.last());
        assertTrue(rs.isLast());
        assertTrue(rs.previous());
        assertTrue(rs.absolute(2));
        assertTrue(rs.relative(-1));

        assertEquals(1, rs.getRow());
        assertEquals(1, rs.getInt("id"));
        assertTrue(rs.isFirst());
        assertEquals(5, m.getDetails().get("result set access").getNum());
    }

    @Test void aFailedNavigationIsPropagatedButStillRecordsTheDetail() throws SQLException {
        rs.close();

        assertThrows(SQLException.class, rs::next);

        assertEquals(1, m.getDetails().get("result set access").getNum());
    }

    @Test void close_finishesTheCollectingMeasurement() throws SQLException {
        assertFalse(rs.isClosed());

        rs.close();

        assertTrue(m.isFinished());
        assertTrue(rs.isClosed());
    }

    @Test void typedGettersByIndexReturnTheValuesOfTheRealResultSet() throws SQLException {
        assertTrue(rs.next());

        assertEquals("one", rs.getString(2));
        assertEquals(new BigDecimal("12.34"), rs.getBigDecimal(3));
        assertEquals(Date.valueOf("2020-02-29"), rs.getDate(4));
        assertEquals(Time.valueOf("10:20:30"), rs.getTime(6));
        assertEquals(Timestamp.valueOf("2020-02-29 10:20:30"), rs.getTimestamp(5));
        assertFalse(rs.wasNull());
    }

    @Test void typedGettersByLabelReturnTheValuesOfTheRealResultSet() throws SQLException {
        assertTrue(rs.next());

        assertEquals("one", rs.getString("name"));
        assertEquals(new BigDecimal("12.34"), rs.getBigDecimal("amount"));
        assertEquals(Date.valueOf("2020-02-29"), rs.getDate("created"));
        assertEquals(Time.valueOf("10:20:30"), rs.getTime("tm"));
        assertEquals(Timestamp.valueOf("2020-02-29 10:20:30"), rs.getTimestamp("ts"));
    }

    @Test void numericGettersConvertTheValuesOfTheRealResultSet() throws SQLException {
        assertTrue(rs.next());

        assertEquals(1, rs.getInt(1));
        assertEquals(1L, rs.getLong(1));
        assertEquals((short) 1, rs.getShort(1));
        assertEquals((byte) 1, rs.getByte(1));
        assertTrue(rs.getBoolean(1));
        assertEquals(1.0f, rs.getFloat(1));
        assertEquals(1.0, rs.getDouble(1));
    }

    @Test void numericGettersByLabelConvertTheValuesOfTheRealResultSet() throws SQLException {
        assertTrue(rs.next());

        assertEquals(1, rs.getInt("id"));
        assertEquals(1L, rs.getLong("id"));
        assertEquals((short) 1, rs.getShort("id"));
        assertEquals((byte) 1, rs.getByte("id"));
        assertTrue(rs.getBoolean("id"));
        assertEquals(1.0f, rs.getFloat("id"));
        assertEquals(1.0, rs.getDouble("id"));
    }

    @Test void binaryAndCharacterStreamsAreReadFromTheRealResultSet() throws Exception {
        assertTrue(rs.next());

        assertArrayEquals(new byte[]{1, 2}, rs.getBytes("data"));
        assertArrayEquals(new byte[]{1, 2}, rs.getBytes(7));
        assertEquals('o', readChar(rs.getCharacterStream(2)));
        assertEquals('o', readChar(rs.getCharacterStream("name")));
        assertEquals((int) 'o', readByte(rs.getAsciiStream(2)));
        assertEquals((int) 'o', readByte(rs.getAsciiStream("name")));
        assertEquals(1, readByte(rs.getBinaryStream(7)));
        assertEquals(1, readByte(rs.getBinaryStream("data")));
        assertEquals('o', readChar(rs.getNCharacterStream(2)));
        assertEquals('o', readChar(rs.getNCharacterStream("name")));
        assertEquals("one", rs.getNString(2));
        assertEquals("one", rs.getNString("name"));
    }

    @Test void largeObjectsAreReadFromTheRealResultSet() throws Exception {
        assertTrue(rs.next());

        final Clob clob = rs.getClob(2);
        assertEquals(3, clob.length());
        assertEquals(3, rs.getClob("name").length());
        assertEquals(3, rs.getNClob(2).length());
        assertEquals(3, rs.getNClob("name").length());
        assertNotNull(rs.getBlob(7));
        assertNotNull(rs.getBlob("data"));
        assertNotNull(rs.getSQLXML(2));
        assertNotNull(rs.getSQLXML("name"));
    }

    @Test void objectsAreReadFromTheRealResultSet() throws SQLException {
        assertTrue(rs.next());

        assertEquals(1, rs.getObject(1));
        assertEquals(1, rs.getObject("id"));
        assertEquals("one", rs.getObject("name"));
        assertEquals("one", rs.getObject(2, String.class));
        assertEquals(new BigDecimal("12.34"), rs.getObject("amount", BigDecimal.class));
        assertEquals(2, rs.findColumn("name"));
    }

    @Test void dateAndTimeGettersWithCalendarAreDelegated() throws SQLException {
        assertTrue(rs.next());
        final Calendar calendar = Calendar.getInstance();

        assertEquals(Date.valueOf("2020-02-29"), rs.getDate(4, calendar));
        assertEquals(Time.valueOf("10:20:30"), rs.getTime(6, calendar));
        assertEquals(Timestamp.valueOf("2020-02-29 10:20:30"), rs.getTimestamp(5, calendar));
        assertEquals(Date.valueOf("2020-02-29"), rs.getDate("created", calendar));
        assertEquals(Time.valueOf("10:20:30"), rs.getTime("tm", calendar));
        assertEquals(Timestamp.valueOf("2020-02-29 10:20:30"), rs.getTimestamp("ts", calendar));
    }

    @Test void bigDecimalGettersWithScaleAreDelegated() throws SQLException {
        assertTrue(rs.next());

        assertEquals(new BigDecimal("12.34"), rs.getBigDecimal(3, 2));
        assertEquals(new BigDecimal("12.34"), rs.getBigDecimal("amount", 2));
    }

    @Test void metaDataIsDelegated() throws SQLException {
        final ResultSetMetaData metaData = org.mockito.Mockito.mock(ResultSetMetaData.class);
        when(innerResultSet.getMetaData()).thenReturn(metaData);
        when(innerResultSet.getType()).thenReturn(ResultSet.TYPE_SCROLL_INSENSITIVE);
        when(innerResultSet.getConcurrency()).thenReturn(ResultSet.CONCUR_UPDATABLE);
        when(innerResultSet.getHoldability()).thenReturn(ResultSet.HOLD_CURSORS_OVER_COMMIT);

        assertSame(metaData, mockRs.getMetaData());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, mockRs.getType());
        assertEquals(ResultSet.CONCUR_UPDATABLE, mockRs.getConcurrency());
        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, mockRs.getHoldability());
    }

    @Test void warningsAreDelegated() throws SQLException {
        final SQLWarning warning = new SQLWarning("careful");
        when(innerResultSet.getWarnings()).thenReturn(warning);

        assertSame(warning, mockRs.getWarnings());
        mockRs.clearWarnings();
        verify(innerResultSet).clearWarnings();
    }

    @Test void fetchSizeAndDirectionAreDelegated() throws SQLException {
        when(innerResultSet.getFetchSize()).thenReturn(42);
        when(innerResultSet.getFetchDirection()).thenReturn(ResultSet.FETCH_FORWARD);

        mockRs.setFetchSize(42);
        mockRs.setFetchDirection(ResultSet.FETCH_FORWARD);

        assertEquals(42, mockRs.getFetchSize());
        assertEquals(ResultSet.FETCH_FORWARD, mockRs.getFetchDirection());
    }

    @Test void rowChangeFlagsAreDelegated() throws SQLException {
        when(innerResultSet.rowUpdated()).thenReturn(true);
        when(innerResultSet.rowInserted()).thenReturn(true);
        when(innerResultSet.rowDeleted()).thenReturn(true);

        assertTrue(mockRs.rowUpdated());
        assertTrue(mockRs.rowInserted());
        assertTrue(mockRs.rowDeleted());
    }

    @Test void unwrapAndIsWrapperForAreDelegated() throws SQLException {
        when(innerResultSet.unwrap(ResultSet.class)).thenReturn(innerResultSet);
        when(innerResultSet.isWrapperFor(ResultSet.class)).thenReturn(true);

        assertSame(innerResultSet, mockRs.unwrap(ResultSet.class));
        assertTrue(mockRs.isWrapperFor(ResultSet.class));
    }

    @Test void featuresTheDriverDoesNotSupportAreDelegatedAsWell() throws Exception {
        final RowId rowId = org.mockito.Mockito.mock(RowId.class);
        final Ref ref = org.mockito.Mockito.mock(Ref.class);
        final URL url = new URL("http://localhost/");
        final Array array = org.mockito.Mockito.mock(Array.class);
        final Map<String, Class<?>> typeMap = new HashMap<>();
        when(innerResultSet.getRowId(1)).thenReturn(rowId);
        when(innerResultSet.getRef(2)).thenReturn(ref);
        when(innerResultSet.getURL(3)).thenReturn(url);
        when(innerResultSet.getArray(4)).thenReturn(array);
        when(innerResultSet.getCursorName()).thenReturn("cursor");
        when(innerResultSet.getObject(1, typeMap)).thenReturn("mapped");

        assertSame(rowId, mockRs.getRowId(1));
        assertSame(ref, mockRs.getRef(2));
        assertSame(url, mockRs.getURL(3));
        assertSame(array, mockRs.getArray(4));
        assertEquals("cursor", mockRs.getCursorName());
        assertEquals("mapped", mockRs.getObject(1, typeMap));
        mockRs.getRowId("id");
        mockRs.getRef("name");
        mockRs.getURL("amount");
        mockRs.getArray("created");
        mockRs.getObject("id", typeMap);
        mockRs.getUnicodeStream(2);
        mockRs.getUnicodeStream("name");
    }

    @Test void unsupportedFeaturesArePropagatedFromTheDriver() {
        assertThrows(SQLException.class, () -> rs.getRowId(1));
        assertThrows(SQLException.class, () -> rs.getURL(3));
        assertThrows(SQLException.class, () -> rs.getRef(1));
        assertThrows(SQLException.class, () -> rs.getObject(1, new HashMap<>()));
        assertThrows(SQLException.class, () -> rs.getCursorName());
    }

    @Test void updatesByIndexAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        final Date date = Date.valueOf("2020-02-29");
        final Time time = Time.valueOf("10:20:30");
        final Timestamp timestamp = Timestamp.valueOf("2020-02-29 10:20:30");
        updatable.moveToInsertRow();

        updatable.updateNull(4);
        updatable.updateBoolean(1, true);
        updatable.updateByte(1, (byte) 1);
        updatable.updateShort(1, (short) 1);
        updatable.updateInt(1, 3);
        updatable.updateLong(1, 3L);
        updatable.updateFloat(1, 3f);
        updatable.updateDouble(1, 3d);
        updatable.updateBigDecimal(3, new BigDecimal("3.30"));
        updatable.updateString(2, "three");
        updatable.updateBytes(7, new byte[]{9});
        updatable.updateDate(4, date);
        updatable.updateTime(6, time);
        updatable.updateTimestamp(5, timestamp);
        updatable.updateObject(1, 3);
        updatable.updateObject(3, new BigDecimal("3.30"), 2);
        updatable.insertRow();

        assertEquals("three", selectName(3));
    }

    @Test void updatesByLabelAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        updatable.moveToInsertRow();

        updatable.updateNull("created");
        updatable.updateBoolean("id", true);
        updatable.updateByte("id", (byte) 2);
        updatable.updateShort("id", (short) 2);
        updatable.updateInt("id", 4);
        updatable.updateLong("id", 4L);
        updatable.updateFloat("id", 4f);
        updatable.updateDouble("id", 4d);
        updatable.updateBigDecimal("amount", new BigDecimal("4.40"));
        updatable.updateString("name", "four");
        updatable.updateBytes("data", new byte[]{9});
        updatable.updateDate("created", Date.valueOf("2020-02-29"));
        updatable.updateTime("tm", Time.valueOf("10:20:30"));
        updatable.updateTimestamp("ts", Timestamp.valueOf("2020-02-29 10:20:30"));
        updatable.updateObject("id", 4);
        updatable.updateObject("amount", new BigDecimal("4.40"), 2);
        updatable.insertRow();

        assertEquals("four", selectName(4));
    }

    @Test void nationalCharacterUpdatesAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        updatable.moveToInsertRow();

        updatable.updateNClob(2, (NClob) null);
        updatable.updateSQLXML(2, null);
        updatable.updateBlob(7, (Blob) null);
        updatable.updateClob(2, (Clob) null);
        updatable.updateArray(3, null);
        updatable.updateNClob("name", (NClob) null);
        updatable.updateSQLXML("name", null);
        updatable.updateBlob("data", (Blob) null);
        updatable.updateClob("name", (Clob) null);
        updatable.updateArray("amount", null);
        updatable.updateInt(1, 5);
        updatable.updateNString(2, "five");
        updatable.updateNString("name", "five");
        updatable.insertRow();

        assertEquals("five", selectName(5));
    }

    @Test void streamUpdatesWithLengthAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        updatable.moveToInsertRow();
        updatable.updateInt(1, 6);

        updatable.updateAsciiStream(2, asciiStream("six"), 3);
        updatable.updateBinaryStream(7, bytesStream(), 1);
        updatable.updateBinaryStream(7, bytesStream(), 1L);
        updatable.updateCharacterStream(2, new StringReader("six"), 3);
        updatable.updateNCharacterStream(2, new StringReader("six"), 3L);
        updatable.updateAsciiStream(2, asciiStream("six"), 3L);
        updatable.updateCharacterStream(2, new StringReader("six"), 3L);
        updatable.updateBlob(7, bytesStream(), 1L);
        updatable.updateClob(2, new StringReader("six"), 3L);
        updatable.updateNClob(2, new StringReader("six"), 3L);
        updatable.insertRow();

        assertEquals("six", selectName(6));
    }

    @Test void streamUpdatesWithoutLengthAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        updatable.moveToInsertRow();
        updatable.updateInt(1, 7);

        updatable.updateAsciiStream(2, asciiStream("seven"));
        updatable.updateBinaryStream(7, bytesStream());
        updatable.updateCharacterStream(2, new StringReader("seven"));
        updatable.updateNCharacterStream(2, new StringReader("seven"));
        updatable.updateBlob(7, bytesStream());
        updatable.updateClob(2, new StringReader("seven"));
        updatable.updateNClob(2, new StringReader("seven"));
        updatable.insertRow();

        assertEquals("seven", selectName(7));
    }

    @Test void streamUpdatesByLabelAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        updatable.moveToInsertRow();
        updatable.updateInt("id", 8);

        updatable.updateAsciiStream("name", asciiStream("eight"), 4);
        updatable.updateBinaryStream("data", bytesStream(), 1);
        updatable.updateCharacterStream("name", new StringReader("eight"), 4);
        updatable.updateAsciiStream("name", asciiStream("eight"), 4L);
        updatable.updateBinaryStream("data", bytesStream(), 1L);
        updatable.updateCharacterStream("name", new StringReader("eight"), 4L);
        updatable.updateAsciiStream("name", asciiStream("eight"));
        updatable.updateBinaryStream("data", bytesStream());
        updatable.updateCharacterStream("name", new StringReader("eight"));
        updatable.updateNCharacterStream("name", new StringReader("eight"), 4L);
        updatable.updateNCharacterStream("name", new StringReader("eight"));
        updatable.updateBlob("data", bytesStream(), 1L);
        updatable.updateClob("name", new StringReader("eight"), 4L);
        updatable.updateNClob("name", new StringReader("eight"), 4L);
        updatable.updateBlob("data", bytesStream());
        updatable.updateClob("name", new StringReader("eight"));
        updatable.updateNClob("name", new StringReader("eight"));
        updatable.insertRow();

        assertEquals("eight", selectName(8));
    }

    @Test void rowUpdatesAreAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        assertTrue(updatable.next());

        updatable.updateString("name", "updated");
        updatable.updateRow();
        updatable.cancelRowUpdates();
        updatable.refreshRow();

        assertEquals("updated", updatable.getString("name"));
    }

    @Test void deleteRowIsAppliedToTheRealResultSet() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        assertTrue(updatable.next());
        assertTrue(updatable.last());

        updatable.deleteRow();

        assertFalse(updatable.next());
        assertEquals("one", selectName(1));
    }

    @Test void moveToInsertRowAndMoveToCurrentRowAreDelegated() throws SQLException {
        final NSysMonResultSet updatable = updatableResultSet();
        assertTrue(updatable.next());

        updatable.moveToInsertRow();
        updatable.updateString("name", "temporary");
        updatable.moveToCurrentRow();

        assertEquals("one", updatable.getString("name"));
    }

    @Test void refAndRowIdUpdatesAreDelegated() throws SQLException {
        final Ref ref = org.mockito.Mockito.mock(Ref.class);
        final RowId rowId = org.mockito.Mockito.mock(RowId.class);

        mockRs.updateRef(1, ref);
        mockRs.updateRef("id", ref);
        mockRs.updateRowId(1, rowId);
        mockRs.updateRowId("id", rowId);

        verify(innerResultSet).updateRef(1, ref);
        verify(innerResultSet).updateRef("id", ref);
        verify(innerResultSet).updateRowId(1, rowId);
        verify(innerResultSet).updateRowId("id", rowId);
    }

    /** reads a single value from the table with an independent statement, to check the effect of an update */
    private String selectName(int id) throws SQLException {
        try (Statement check = rawConnection.createStatement();
             ResultSet result = check.executeQuery("select name from T where id = " + id)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private NSysMonResultSet updatableResultSet() throws SQLException {
        final Statement statement = rawConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        return new NSysMonResultSet(statement.executeQuery(SELECT), wrappedStatement, sysMon.startCollectingMeasurement("update"));
    }

    private static InputStream asciiStream(String text) {
        return new ByteArrayInputStream(text.getBytes());
    }

    private static InputStream bytesStream() {
        return new ByteArrayInputStream(new byte[]{1});
    }

    private static char readChar(Reader reader) throws Exception {
        return (char) reader.read();
    }

    private static int readByte(InputStream stream) throws Exception {
        return stream.read();
    }
}
