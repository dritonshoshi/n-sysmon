package com.nsysmon.measure.jdbc;

import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.testutil.CollectingDataSink;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NSysMonConnectionTest {

    private static final String POOL = "the-pool";

    @Mock private Connection inner;
    @Mock private AIConnectionCounter counter;

    private CollectingDataSink dataSink;
    private NSysMonConnection conn;

    @BeforeEach void before() {
        dataSink = new CollectingDataSink();
        conn = new NSysMonConnection(inner, JdbcTestSupport.createSysMon(dataSink), POOL, counter);
    }

    @Test void creatingTheWrapper_countsTheConnectionAsOpen() {
        verify(counter).onOpenConnection(POOL);
    }

    @Test void createStatement_activatesTheConnection() throws SQLException {
        final Statement stmt = conn.createStatement();

        assertInstanceOf(NSysMonStatement.class, stmt);
        verify(counter).onActivateConnection(POOL);
    }

    @Test void createStatement_activatesTheConnectionOnlyOnce() throws SQLException {
        conn.createStatement();
        conn.createStatement();

        verify(counter, times(1)).onActivateConnection(POOL);
    }

    @Test void createStatement_doesNotActivateTheConnectionWhenAutoCommitIsOn() throws SQLException {
        when(inner.getAutoCommit()).thenReturn(true);

        conn.createStatement();

        verify(counter, never()).onActivateConnection(any());
    }

    @Test void prepareStatement_returnsAWrapperAndActivatesTheConnection() throws SQLException {
        when(inner.prepareStatement("select 1")).thenReturn(org.mockito.Mockito.mock(PreparedStatement.class));

        final PreparedStatement stmt = conn.prepareStatement("select 1");

        assertInstanceOf(NSysMonPreparedStatement.class, stmt);
        verify(counter).onActivateConnection(POOL);
    }

    @Test void prepareCall_returnsAWrapperAndActivatesTheConnection() throws SQLException {
        when(inner.prepareCall("{call a}")).thenReturn(org.mockito.Mockito.mock(CallableStatement.class));

        final CallableStatement stmt = conn.prepareCall("{call a}");

        assertInstanceOf(NSysMonCallableStatement.class, stmt);
        verify(counter).onActivateConnection(POOL);
    }

    @Test void createStatementWithResultSetProperties_isDelegated() throws SQLException {
        final Statement stmt = org.mockito.Mockito.mock(Statement.class);
        when(inner.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(stmt);

        assertSame(stmt.unwrap(Statement.class), conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY).unwrap(Statement.class));
    }

    @Test void createStatementWithHoldability_isDelegated() throws SQLException {
        final Statement stmt = org.mockito.Mockito.mock(Statement.class);
        when(inner.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT)).thenReturn(stmt);

        final Statement result = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);

        assertInstanceOf(NSysMonStatement.class, result);
    }

    @Test void prepareStatementWithResultSetProperties_isDelegated() throws SQLException {
        final PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
        when(inner.prepareStatement("select 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(stmt);

        final PreparedStatement result = conn.prepareStatement("select 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        assertInstanceOf(NSysMonPreparedStatement.class, result);
    }

    @Test void prepareStatementWithHoldability_isDelegated() throws SQLException {
        final PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
        when(inner.prepareStatement("select 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT)).thenReturn(stmt);

        final PreparedStatement result = conn.prepareStatement("select 1", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT);

        assertInstanceOf(NSysMonPreparedStatement.class, result);
    }

    @Test void prepareStatementWithAutoGeneratedKeys_isDelegated() throws SQLException {
        final PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
        when(inner.prepareStatement("insert into a values (1)", Statement.RETURN_GENERATED_KEYS)).thenReturn(stmt);

        assertInstanceOf(NSysMonPreparedStatement.class, conn.prepareStatement("insert into a values (1)", Statement.RETURN_GENERATED_KEYS));
    }

    @Test void prepareStatementWithColumnIndexes_isDelegated() throws SQLException {
        final PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
        when(inner.prepareStatement("insert into a values (1)", new int[]{1})).thenReturn(stmt);

        assertInstanceOf(NSysMonPreparedStatement.class, conn.prepareStatement("insert into a values (1)", new int[]{1}));
    }

    @Test void prepareStatementWithColumnNames_isDelegated() throws SQLException {
        final PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
        when(inner.prepareStatement("insert into a values (1)", new String[]{"id"})).thenReturn(stmt);

        assertInstanceOf(NSysMonPreparedStatement.class, conn.prepareStatement("insert into a values (1)", new String[]{"id"}));
    }

    @Test void prepareCallWithResultSetProperties_isDelegated() throws SQLException {
        final CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
        when(inner.prepareCall("{call a}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(stmt);

        assertInstanceOf(NSysMonCallableStatement.class, conn.prepareCall("{call a}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
    }

    @Test void prepareCallWithHoldability_isDelegated() throws SQLException {
        final CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
        when(inner.prepareCall("{call a}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT)).thenReturn(stmt);

        assertInstanceOf(NSysMonCallableStatement.class, conn.prepareCall("{call a}", ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.CLOSE_CURSORS_AT_COMMIT));
    }

    @Test void commit_isMeasuredAndPassivatesTheConnection() throws SQLException {
        conn.createStatement();

        conn.commit();

        verify(counter).onPassivateConnection(POOL);
        verify(inner).commit();
        assertEquals("jdbc:commit", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void commit_isMeasuredEvenIfTheInnerCommitFails() throws SQLException {
        doThrow(new SQLException("commit failed")).when(inner).commit();

        assertThrows(SQLException.class, () -> conn.commit());

        assertEquals("jdbc:commit", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void rollback_isMeasuredAndPassivatesTheConnection() throws SQLException {
        conn.createStatement();

        conn.rollback();

        verify(counter).onPassivateConnection(POOL);
        verify(inner).rollback();
        assertEquals("jdbc:rollback", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void rollback_isMeasuredEvenIfTheInnerRollbackFails() throws SQLException {
        doThrow(new SQLException("rollback failed")).when(inner).rollback();

        assertThrows(SQLException.class, () -> conn.rollback());

        assertEquals("jdbc:rollback", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void setSavepoint_isMeasured() throws SQLException {
        final Savepoint savepoint = org.mockito.Mockito.mock(Savepoint.class);
        when(inner.setSavepoint()).thenReturn(savepoint);

        assertSame(savepoint, conn.setSavepoint());

        assertEquals("jdbc:setSavepoint", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void namedSetSavepoint_isMeasured() throws SQLException {
        final Savepoint savepoint = org.mockito.Mockito.mock(Savepoint.class);
        when(inner.setSavepoint("sp")).thenReturn(savepoint);

        assertSame(savepoint, conn.setSavepoint("sp"));

        assertEquals("jdbc:setSavepoint", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void namedSetSavepoint_isMeasuredEvenIfItFails() throws SQLException {
        when(inner.setSavepoint("sp")).thenThrow(new SQLException("no savepoints supported"));

        assertThrows(SQLException.class, () -> conn.setSavepoint("sp"));

        assertEquals("jdbc:setSavepoint", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void rollbackToSavepoint_isMeasured() throws SQLException {
        final Savepoint savepoint = org.mockito.Mockito.mock(Savepoint.class);

        conn.rollback(savepoint);

        verify(inner).rollback(savepoint);
        assertEquals("jdbc:rollback", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void rollbackToSavepoint_isMeasuredEvenIfItFails() throws SQLException {
        final Savepoint savepoint = org.mockito.Mockito.mock(Savepoint.class);
        doThrow(new SQLException("invalid savepoint")).when(inner).rollback(savepoint);

        assertThrows(SQLException.class, () -> conn.rollback(savepoint));

        assertEquals("jdbc:rollback", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void releaseSavepoint_isMeasured() throws SQLException {
        final Savepoint savepoint = org.mockito.Mockito.mock(Savepoint.class);

        conn.releaseSavepoint(savepoint);

        verify(inner).releaseSavepoint(savepoint);
        assertEquals("jdbc:releaseSavepoint", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void releaseSavepoint_isMeasuredEvenIfItFails() throws SQLException {
        final Savepoint savepoint = org.mockito.Mockito.mock(Savepoint.class);
        doThrow(new SQLException("invalid savepoint")).when(inner).releaseSavepoint(savepoint);

        assertThrows(SQLException.class, () -> conn.releaseSavepoint(savepoint));

        assertEquals("jdbc:releaseSavepoint", dataSink.data.get(0).getRootNode().getIdentifier());
    }

    @Test void close_closesTheInnerConnectionAndCountsTheConnectionAsClosed() throws SQLException {
        conn.createStatement();

        conn.close();

        verify(counter).onPassivateConnection(POOL);
        verify(counter).onCloseConnection(POOL);
        verify(inner).close();
    }

    @Test void close_doesNotCountAnAlreadyClosedConnection() throws SQLException {
        when(inner.isClosed()).thenReturn(true);

        conn.close();

        verify(counter, never()).onCloseConnection(any());
        verify(inner).close();
    }

    @Test void getters_areDelegatedToTheInnerConnection() throws SQLException {
        when(inner.getAutoCommit()).thenReturn(true);
        when(inner.isReadOnly()).thenReturn(true);
        when(inner.getCatalog()).thenReturn("CAT");
        when(inner.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_SERIALIZABLE);
        when(inner.getSchema()).thenReturn("PUBLIC");
        when(inner.getNetworkTimeout()).thenReturn(1000);

        assertTrue(conn.getAutoCommit());
        assertTrue(conn.isReadOnly());
        assertEquals("CAT", conn.getCatalog());
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, conn.getTransactionIsolation());
        assertEquals("PUBLIC", conn.getSchema());
        assertEquals(1000, conn.getNetworkTimeout());
        assertFalse(conn.isClosed());
    }

    @Test void nativeSQL_isDelegatedToTheInnerConnection() throws SQLException {
        when(inner.nativeSQL("{fn now()}")).thenReturn("now()");

        assertEquals("now()", conn.nativeSQL("{fn now()}"));
        verify(counter).onActivateConnection(POOL);
    }

    @Test void metaDataAndWarnings_areDelegatedToTheInnerConnection() throws SQLException {
        final java.sql.DatabaseMetaData metaData = org.mockito.Mockito.mock(java.sql.DatabaseMetaData.class);
        final java.sql.SQLWarning warning = new java.sql.SQLWarning("careful");
        when(inner.getMetaData()).thenReturn(metaData);
        when(inner.getWarnings()).thenReturn(warning);

        assertSame(metaData, conn.getMetaData());
        assertSame(warning, conn.getWarnings());

        conn.clearWarnings();
        verify(inner).clearWarnings();
    }

    @Test void typeMapIsDelegatedToTheInnerConnection() throws SQLException {
        final Map<String, Class<?>> typeMap = Map.of("A", String.class);
        when(inner.getTypeMap()).thenReturn(typeMap);

        assertSame(typeMap, conn.getTypeMap());
        conn.setTypeMap(typeMap);
        verify(inner).setTypeMap(typeMap);
    }

    @Test void holdabilityAndValidityAreDelegatedToTheInnerConnection() throws SQLException {
        when(inner.getHoldability()).thenReturn(ResultSet.HOLD_CURSORS_OVER_COMMIT);
        when(inner.isValid(5)).thenReturn(true);

        assertEquals(ResultSet.HOLD_CURSORS_OVER_COMMIT, conn.getHoldability());
        assertTrue(conn.isValid(5));

        conn.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        verify(inner).setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Test void clientInfoIsDelegatedToTheInnerConnection() throws SQLException {
        final Properties clientInfo = new Properties();
        when(inner.getClientInfo()).thenReturn(clientInfo);
        when(inner.getClientInfo("name")).thenReturn("value");

        assertSame(clientInfo, conn.getClientInfo());
        assertEquals("value", conn.getClientInfo("name"));

        conn.setClientInfo("name", "value");
        conn.setClientInfo(clientInfo);
        verify(inner).setClientInfo("name", "value");
        verify(inner).setClientInfo(clientInfo);
    }

    @Test void settersAreDelegatedToTheInnerConnection() throws SQLException {
        conn.setAutoCommit(false);
        conn.setReadOnly(true);
        conn.setCatalog("CAT");
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        conn.setSchema("PUBLIC");

        verify(inner).setAutoCommit(false);
        verify(inner).setReadOnly(true);
        verify(inner).setCatalog("CAT");
        verify(inner).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        verify(inner).setSchema("PUBLIC");
    }

    @Test void jdbcObjectsAreCreatedByTheInnerConnection() throws Exception {
        final java.sql.Clob clob = org.mockito.Mockito.mock(java.sql.Clob.class);
        final java.sql.Blob blob = org.mockito.Mockito.mock(java.sql.Blob.class);
        final java.sql.NClob nClob = org.mockito.Mockito.mock(java.sql.NClob.class);
        final java.sql.SQLXML sqlxml = org.mockito.Mockito.mock(java.sql.SQLXML.class);
        final java.sql.Array array = org.mockito.Mockito.mock(java.sql.Array.class);
        final java.sql.Struct struct = org.mockito.Mockito.mock(java.sql.Struct.class);
        when(inner.createClob()).thenReturn(clob);
        when(inner.createBlob()).thenReturn(blob);
        when(inner.createNClob()).thenReturn(nClob);
        when(inner.createSQLXML()).thenReturn(sqlxml);
        when(inner.createArrayOf("INT", new Object[]{1})).thenReturn(array);
        when(inner.createStruct("T", new Object[]{1})).thenReturn(struct);

        assertSame(clob, conn.createClob());
        assertSame(blob, conn.createBlob());
        assertSame(nClob, conn.createNClob());
        assertSame(sqlxml, conn.createSQLXML());
        assertSame(array, conn.createArrayOf("INT", new Object[]{1}));
        assertSame(struct, conn.createStruct("T", new Object[]{1}));
    }

    @Test void abortAndNetworkTimeoutAreDelegatedToTheInnerConnection() throws SQLException {
        final java.util.concurrent.Executor executor = Runnable::run;

        conn.abort(executor);
        conn.setNetworkTimeout(executor, 4711);

        verify(inner).abort(executor);
        verify(inner).setNetworkTimeout(executor, 4711);
    }

    @Test void unwrapAndIsWrapperForAreDelegatedToTheInnerConnection() throws SQLException {
        when(inner.unwrap(Connection.class)).thenReturn(inner);
        when(inner.isWrapperFor(Connection.class)).thenReturn(true);

        assertSame(inner, conn.unwrap(Connection.class));
        assertTrue(conn.isWrapperFor(Connection.class));

        // 'unwrap' activates the connection as well
        verify(counter).onActivateConnection(POOL);
    }

    @Test void statementsExecutedOnARealConnectionAreMeasuredWithinTheConnectionMeasurement() throws SQLException {
        final DataSource dataSource = new NSysMonDataSource(JdbcTestSupport.createDataSource(), POOL, JdbcTestSupport.createSysMon(dataSink));

        try (java.sql.Connection real = dataSource.getConnection();
             Statement stmt = real.createStatement()) {
            stmt.execute("create table A (id int)");
            stmt.executeUpdate("insert into A values (1)");
        }

        final AHierarchicalData root = dataSink.data.get(0).getRootNode();
        assertEquals("jdbc:connection from pool", root.getIdentifier());
        assertEquals(2, root.getChildren().size());
        assertEquals("jdbc: create table A (id int)", root.getChildren().get(0).getIdentifier());
    }
}
