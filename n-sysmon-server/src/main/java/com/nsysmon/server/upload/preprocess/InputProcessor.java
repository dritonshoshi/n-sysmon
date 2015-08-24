package com.nsysmon.server.upload.preprocess;

import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceRootNode;

/**
 * @author arno
 */
public interface InputProcessor {
    void updateSystemClockDiff(InstanceIdentifier instance, long senderTimestamp);

    void addEnvironmentEntry(InstanceIdentifier instance, EnvironmentNode environmentNode);
    void addScalarEntry     (InstanceIdentifier instance, ScalarNode scalarNode);
    void addTraceEntry      (InstanceIdentifier instance, TraceRootNode traceRootNode);
}
