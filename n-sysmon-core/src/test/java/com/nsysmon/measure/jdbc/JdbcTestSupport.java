package com.nsysmon.measure.jdbc;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.config.appinfo.ADefaultApplicationInfoProvider;
import com.nsysmon.impl.NSysMonConfigurer;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.measure.NSysMonSource;
import com.nsysmon.testutil.CollectingDataSink;
import org.h2.jdbcx.JdbcDataSource;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixtures shared by the tests of this package: each test gets a private N-SysMon instance with its own data sink,
 * plus a private in-memory H2 database, so tests can run in any order without interfering with each other.
 */
final class JdbcTestSupport {

    private static final AtomicInteger DB_COUNTER = new AtomicInteger();

    private JdbcTestSupport() {
    }

    /**
     * The application info provider is created with all four arguments on purpose: the two-argument constructor
     * resolves the local host name, which must not happen in a hermetic test.
     */
    static NSysMonApi createSysMon(CollectingDataSink dataSink) {
        final NSysMonConfigBuilder configBuilder = new NSysMonConfigBuilder(
                new ADefaultApplicationInfoProvider("jdbc-test", "test", "test-deployment", "test-node"));

        final NSysMonApi result = new NSysMonImpl(configBuilder.build());
        NSysMonConfigurer.addDataSink(result, dataSink);
        return result;
    }

    static JdbcDataSource createDataSource() {
        final JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + uniqueDbName() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    static String uniqueDbName() {
        return "nsysmon_jdbc_" + DB_COUNTER.incrementAndGet() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    static String uniquePoolIdentifier() {
        return "pool_" + uniqueDbName();
    }

    /**
     * NSysMonJdbcDriver instantiates this class by name, so it needs a public no-arg constructor.
     */
    public static class TestSysMonSource implements NSysMonSource {

        public static NSysMonApi sysMon;

        TestSysMonSource() {
        }

        @Override public NSysMonApi getSysMon() {
            return sysMon;
        }
    }
}
