package com.nsysmon.server.storage;

import com.nsysmon.server.util.AOption;

import java.util.Map;

/**
 * @author arno
 */
public interface ScalarMetaDataDao {
    Map<String, ScalarMetaData> getAll();

    AOption<ScalarMetaData> get(String name);
    void store(ScalarMetaData scalarMetaData);
}
