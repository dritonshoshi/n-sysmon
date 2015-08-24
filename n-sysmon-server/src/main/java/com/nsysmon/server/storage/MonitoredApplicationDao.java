package com.nsysmon.server.storage;

import com.mongodb.DBObject;

import java.util.List;

/**
 * @author arno
 */
public interface MonitoredApplicationDao {
    List<String> getMonitoredApplicationNames();
    List<DBObject> getMonitoredApplicationsRaw();
}
