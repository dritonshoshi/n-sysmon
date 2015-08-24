package com.nsysmon.server.storage;

import com.nsysmon.server.data.json.ScalarNode;

/**
 * @author arno
 */
public interface ScalarDataDao {
    void storeScalarData(ScalarNode scalarNode);
}
