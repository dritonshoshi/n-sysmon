package com.nsysmon.measure.jdbc;

import com.nsysmon.NSysMon;
import com.nsysmon.data.AHierarchicalData;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.testutil.CollectingDataSink;
import org.h2.jdbcx.JdbcConnectionPool;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * @author arno
 */
public class JdbcTest {
    //TODO refactor this test to use a sys mon holder to avoid hidden side effects through the default sysmon singleton

    @Test
    public void testTopLevelJdbcDriverManager() throws Exception {
        final CollectingDataSink dataSink = new CollectingDataSink();
        NSysMonConfigurer.addDataSink(NSysMon.get(), dataSink);

        final Connection conn = DriverManager.getConnection("nsysmon::jdbc:h2:mem:demo", "sa", "");
        final Statement stmt = conn.createStatement();
        stmt.execute("create table A (oid number primary key)");
        stmt.execute("insert into A (oid) values (1)");
        final ResultSet rs = conn.createStatement().executeQuery("select * from A");
        while(rs.next());
        rs.close();
        stmt.close();
        conn.close();

        // meaurement of 'select' statement was discarded --> top-level collecting measurement
        assertEquals(2, dataSink.data.size());
        assertEquals("jdbc: create table A (oid number primary key)", dataSink.data.get(0).getRootNode().getIdentifier());
        assertEquals("jdbc: insert into A (oid) values (1)", dataSink.data.get(1).getRootNode().getIdentifier());
    }

    @Test
    public void testTopLevelJdbcDataSource() throws Exception {
        final CollectingDataSink dataSink = new CollectingDataSink();
        NSysMonConfigurer.addDataSink(NSysMon.get(), dataSink);

        final DataSource dataSource = new NSysMonDataSource(JdbcConnectionPool.create ("jdbc:h2:mem:demo", "sa", ""), null, NSysMon.get());

        try (Connection conn = dataSource.getConnection ()) {
            final Statement stmt = conn.createStatement();
            stmt.execute("create table A (oid number primary key)");
            stmt.execute("insert into A (oid) values (1)");
            final ResultSet rs = conn.createStatement().executeQuery("select * from A");
            while(rs.next());
        }

        assertEquals(1, dataSink.data.size());
        final AHierarchicalData root = dataSink.data.get (0).getRootNode ();
        assertEquals("jdbc:connection from pool", root.getIdentifier ());

        assertEquals (3, root.getChildren ().size ());

        assertEquals ("jdbc: create table A (oid number primary key)", root.getChildren ().get (0).getIdentifier ());
        assertEquals ("jdbc: insert into A (oid) values (1)",          root.getChildren ().get (1).getIdentifier ());

        assertEquals ("jdbc: select * from A",                         root.getChildren ().get (2).getIdentifier ());
    }

    //TODO implicit close of rs and stmt --> how do connection pools deal with this? They do not call Connection.close()...
    //TODO test detail measurements of SELECT
}
