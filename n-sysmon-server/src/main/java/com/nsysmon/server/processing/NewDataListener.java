package com.nsysmon.server.processing;

import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceRootNode;


/**
 * @author arno
 */
public interface NewDataListener {
    void onNewScalarData(ScalarNode scalarData);
    void onNewTrace(TraceRootNode traceData);
    void onNewEnvironmentData(EnvironmentNode environmentData);
}
