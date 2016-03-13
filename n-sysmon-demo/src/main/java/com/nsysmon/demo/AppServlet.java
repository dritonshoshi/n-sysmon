package com.nsysmon.demo;

import com.nsysmon.NSysMon;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.measure.jdbc.NSysMonDataSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author arno
 */
public class AppServlet extends HttpServlet {
    private static final DataSource dataSource = createDataSource();

    static Connection conn;

    static {
        try {
            // store__ the connection to keep the in-memory database
            conn = getConnection();
            conn.createStatement().execute("create table A (oid number primary key)");

            conn.commit();

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final PrintWriter out = resp.getWriter();
        out.println ("<html><head><title>N-SysMon demo content</title></head><body><h1>N-SysMon demo content</h1></body></html>");

        final ASimpleMeasurement parMeasurement = NSysMon.get().start("parallel", false);
        sleep();

        NSysMon.get().measure("a", m -> {
            return sleep();
        });
        NSysMon.get().measure("b", m -> {
            doQuery(); doQuery(); doQuery(); return sleep();
        });
        NSysMon.get().measure("q", m -> {
            doQuery(); doQuery(); doQuery(); doQuery(); doQuery(); doQuery(); doQuery(); doQuery(); return sleep();
        });
        NSysMon.get().measure("b", m -> {
            return sleep();
        });
        NSysMon.get().measure("b", m -> {
            return sleep();
        });
        NSysMon.get().measure("b", m -> {
            return sleep();
        });
        NSysMon.get().measure("b", m -> {
            return sleep();
        });
        NSysMon.get().measure("a", m -> {
            return sleep();
        });
        NSysMon.get().measure("a", m -> {
            doQuery(); return sleep();
        });
        NSysMon.get().measure("b", m -> {
            NSysMon.get().measure("x", m1 -> {
                doQuery(); return sleep();
            });
            doQuery(); return sleep();
        });
        parMeasurement.finish();
        NSysMon.get().measure("c", m -> {
            return sleep();
        });
        NSysMon.get().measure("d", m -> {
            doQueryWithMultipleParameters();
            return sleep();
        });

        correlations("e");

        hugeTree(8, 6);
    }

    private void correlations(String nameOfStartingNode) {
        // mainReason -> subReason -> subReason
        final String mainReasonT1_1 = UUID.randomUUID().toString();
        final String subReasonT1_1 = UUID.randomUUID().toString();
        final String subReasonT1_1_1 = UUID.randomUUID().toString();

        NSysMon.get().measure(nameOfStartingNode + 0, m1 -> {
            NSysMon.get().startFlow(new ACorrelationId("mainReasonT1_1", mainReasonT1_1, null));
            NSysMon.get().measure(nameOfStartingNode + 1, m2 -> {
                NSysMon.get().joinFlow(new ACorrelationId("subReasonT1_1", subReasonT1_1, mainReasonT1_1));
                NSysMon.get().measure(nameOfStartingNode + 2, m3 -> {
                    NSysMon.get().joinFlow(new ACorrelationId("subReasonT1_1_1", subReasonT1_1_1, subReasonT1_1));
                });
            });
        });

        // mainReason -> subReason -> subReason1
        //                         -> subReason2
        final String mainReasonT2_1 = UUID.randomUUID().toString();
        final String subReasonT2_1 = UUID.randomUUID().toString();
        final String subReasonT2_1_1 = UUID.randomUUID().toString();
        final String subReasonT2_1_2 = UUID.randomUUID().toString();

        NSysMon.get().measure(nameOfStartingNode + 1, m1 -> {
            NSysMon.get().startFlow(new ACorrelationId("mainReasonT2_1", mainReasonT2_1, null));
            NSysMon.get().measure(nameOfStartingNode + 2, m2 -> {
                NSysMon.get().joinFlow(new ACorrelationId("subReasonT2_1", subReasonT2_1, mainReasonT2_1));
                NSysMon.get().measure(nameOfStartingNode + 3, m3 -> {
                    NSysMon.get().joinFlow(new ACorrelationId("subReasonT2_1_1", subReasonT2_1_1, subReasonT2_1));
                    NSysMon.get().joinFlow(new ACorrelationId("subReasonT2_1_2", subReasonT2_1_2, subReasonT2_1));
                });
            });
        });

        // mainReason -> subReason -> subReason1 -> subReason1_1
        //                         -> subReason2 -> subReason2_1
        //                         -> subReason2 -> subReason2_2
        final String mainReasonT3_1 = UUID.randomUUID().toString();
        final String subReasonT3_1 = UUID.randomUUID().toString();
        final String subReasonT3_2 = UUID.randomUUID().toString();
        final String subReasonT3_2_1 = UUID.randomUUID().toString();
        final String subReasonT3_1_1 = UUID.randomUUID().toString();
        final String subReasonT3_2_2 = UUID.randomUUID().toString();

        NSysMon.get().measure(nameOfStartingNode + 1, m1 -> {
            NSysMon.get().startFlow(new ACorrelationId("mainReasonT3_1", mainReasonT3_1, null));
            NSysMon.get().measure(nameOfStartingNode + 2, m2 -> {
                NSysMon.get().joinFlow(new ACorrelationId("subReasonT3_1", subReasonT3_1, mainReasonT3_1));
                NSysMon.get().joinFlow(new ACorrelationId("subReasonT3_2", subReasonT3_2, mainReasonT3_1));
                NSysMon.get().measure(nameOfStartingNode + 3, m3 -> {
                    NSysMon.get().joinFlow(new ACorrelationId("subReasonT3_1_1", subReasonT3_1_1, subReasonT3_1));
                    NSysMon.get().joinFlow(new ACorrelationId("subReasonT3_2_1", subReasonT3_2_1, subReasonT3_2));
                    NSysMon.get().joinFlow(new ACorrelationId("subReasonT3_2_2", subReasonT3_2_2, subReasonT3_2));
                });
            });
        });
    }

    private void hugeTree(final int width, final int depth) {
        for(int i=0; i<width; i++) {
            if(depth > 0) {
                NSysMon.get().measure("Q-" + depth + "-" + i, m -> {
                    hugeTree(width, depth-1);
                });
            }
        }
    }

    private Object sleep() {
        try {
            Thread.sleep(20 + new Random().nextInt(20));
        } catch (InterruptedException exc) {
            throw new RuntimeException(exc);
        }
        return null;
    }

    private void doQuery() {
        try {
            final Connection conn = getConnection();
            try {
                final PreparedStatement ps = conn.prepareStatement("select * from A where oid < ? and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1 and 1=1");
                try {
                    ps.setLong(1, 25);
                    final ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        //Just ignore the result, goal is only to generat the sql
                    }
                }
                finally {
                    ps.close();
                }
            } finally {
                conn.commit();
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void doQueryWithMultipleParameters() {
        try {
            final Connection conn = getConnection();
            try {
                final PreparedStatement ps = conn.prepareStatement("select * from A where oid < ? and oid < ? and oid < ? and 1=? and 2=? and 3=? and 4=? and 5=? and 6=?");
                try {
                    ps.setLong(1, 25);
                    ps.setLong(2, 25);
                    ps.setLong(3, 25);
                    ps.setLong(4, 1);
                    ps.setLong(5, 2);
                    ps.setLong(6, 3);
                    ps.setString(7, "str4");
                    ps.setInt(8, 5);
                    ps.setByte(9, (byte)6);
                    final ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        //Just ignore the result, goal is only to generat the sql
                    }
                }
                finally {
                    ps.close();
                }
            } finally {
                conn.commit();
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
//        final Connection result = DriverManager.getConnection("nsysmon:qualifier=123:jdbc:h2:memgc:demo", "sa", "");
//        result.setAutoCommit(false);
//        return result;
    }

    private static DataSource createDataSource() {
        final DataSource inner = new DataSource() {
            @Override public Connection getConnection() throws SQLException {
                final Connection result = DriverManager.getConnection("jdbc:h2:mem:demo", "sa", "");
                result.setAutoCommit(false);
                return result;
            }

            @Override
            public Connection getConnection(String username, String password) throws SQLException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public PrintWriter getLogWriter() throws SQLException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void setLogWriter(PrintWriter out) throws SQLException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public void setLoginTimeout(int seconds) throws SQLException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public int getLoginTimeout() throws SQLException {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Logger getParentLogger() throws SQLFeatureNotSupportedException {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> iface) throws SQLException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            @Override
            public boolean isWrapperFor(Class<?> iface) throws SQLException {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };

        return new NSysMonDataSource(inner, "234", NSysMon.get());
    }
}
