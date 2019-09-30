package com.nsysmon.measure.jdbc;


import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.log.NSysMonLogger;
import com.nsysmon.measure.NSysMonSource;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * @author arno
 */
public class NSysMonJdbcDriver implements Driver {
    public static final String URL_PREFIX = "nsysmon:";

    public static final String PARAM_CONNECTIONPOOL_IDENTIFIER = "qualifier";
    public static final String PARAM_NSYSMON_SOURCE = "nsysmon-source";

    public static final NSysMonJdbcDriver INSTANCE = new NSysMonJdbcDriver();

    private static final NSysMonLogger log = NSysMonLogger.get(NSysMonJdbcDriver.class);

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deregister() throws SQLException {
        DriverManager.deregisterDriver(INSTANCE);
    }

    @Override public Connection connect(String url, Properties info) throws SQLException {
        if(! acceptsURL(url)) {
            return null;
        }

        final String withoutPrefix = url.substring (URL_PREFIX.length ());
        final int idxColon = withoutPrefix.indexOf(':');
        if(idxColon == -1) {
            return null;
        }

        final String innerUrl = withoutPrefix.substring(idxColon+1);
        final Connection inner = DriverManager.getConnection(innerUrl, info);

        try {
            final String paramString = withoutPrefix.substring(0, idxColon);
            final Map<String, String> params = parseParams(paramString);
            final NSysMonApi sysMon = getSysMon(params);

            if(sysMon.getConfig().isGloballyDisabled()) {
                return inner;
            }

            return new NSysMonConnection(inner, sysMon, getPoolIdentifier(params), AConnectionCounter.INSTANCE); //TODO make instance management configurable
        }
        catch (SQLException e) {
            log.error (e);
            return inner;
        }
    }

    private String getPoolIdentifier(Map<String, String> params) {
        return params.get(PARAM_CONNECTIONPOOL_IDENTIFIER);
    }

    private NSysMonApi getSysMon(Map<String, String> params) throws SQLException {
        final String sysmonSourceName = params.get(PARAM_NSYSMON_SOURCE);
        if(sysmonSourceName == null) {
            return NSysMon.get();
        }

        try {
            final NSysMonSource sysMonSource = (NSysMonSource) Class.forName(sysmonSourceName).getDeclaredConstructor().newInstance();
            return sysMonSource.getSysMon();
        } catch (Exception exc) {
            throw new SQLException("error retrieving NSysMon instance", exc);
        }
    }

    private Map<String, String> parseParams(String paramString) {
        final Map<String, String> result = new HashMap<>();

        if(paramString.trim().isEmpty()) {
            return result;
        }

        for(String part: paramString.split(";")) {
            final String[] keyValue = part.split("=");
            if(keyValue.length != 2) {
                throw new IllegalArgumentException("key/value pairs with '=', ';' between pairs");
            }
            result.put(keyValue[0].toLowerCase().trim(), keyValue[1].trim());
        }
        return result;
    }

    @Override public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return new DriverPropertyInfo[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override public int getMajorVersion() {
        return 1;
    }

    @Override public int getMinorVersion() {
        return 0;
    }

    @Override public boolean jdbcCompliant() {
        return true; //TODO what to return here?!
    }

    // introduced with JDK 1.7
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }
}
