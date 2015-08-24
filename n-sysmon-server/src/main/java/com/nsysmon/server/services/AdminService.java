package com.nsysmon.server.services;


import com.nsysmon.server.storage.ScalarMetaData;
import com.nsysmon.server.util.AOption;
import com.nsysmon.server.util.json.ListWrapper;

/**
 * @author arno
 */
public interface AdminService {
    ListWrapper<String> getMonitoredApplicationNames();

    public AOption<ScalarMetaData> get(String name);
}
