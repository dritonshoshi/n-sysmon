package com.nsysmon.testutil;

import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.data.json.CorrelationId;
import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.RootNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceNode;
import com.nsysmon.server.data.json.TraceRootNode;

import java.util.Arrays;
import java.util.List;

/**
 * Factories for the data nodes that agents upload to the server. Kept in one place so that the individual tests stay
 * focused on the behaviour they verify instead of on fixture construction.
 */
public final class Nodes {
    /**
     * A complete upload payload, as an agent sends it: one environment entry, one scalar and one trace with a child
     * node. It contains no instance identifiers because those are added by the server while preprocessing the data.
     */
    public static final String ROOT_NODE_JSON = """
            {
              "sender": "app-1",
              "senderInstance": "instance-1",
              "senderTimestamp": 1700000000000,
              "environment": [
                {"uuid": "env-uuid", "key": "os.name", "value": "Linux", "senderTimestamp": 1700000000001}
              ],
              "scalars": [
                {"uuid": "scalar-uuid", "name": "cpu.load", "value": 0.75, "senderTimestamp": 1700000000002}
              ],
              "traces": [
                {
                  "uuid": "trace-uuid",
                  "trace": {
                    "isSerial": true,
                    "senderStartTimeMillis": 1700000000003,
                    "durationNanos": 5000,
                    "identifier": "root-trace",
                    "parameters": {"sql": "select 1"},
                    "children": [
                      {
                        "isSerial": false,
                        "senderStartTimeMillis": 1700000000004,
                        "durationNanos": 2500,
                        "identifier": "child-trace"
                      }
                    ]
                  },
                  "startedFlows": [{"kind": "jms", "ident": "flow-1"}],
                  "joinedFlows": [{"kind": "jms", "ident": "flow-0"}]
                }
              ]
            }""";

    private Nodes() {
    }

    public static InstanceIdentifier instance(String applicationId, String instanceId) {
        return new InstanceIdentifier(applicationId, instanceId);
    }

    public static ScalarNode scalar(String uuid, String name, double value, long senderTimestamp) {
        final ScalarNode result = new ScalarNode();
        result.setUuid(uuid);
        result.setName(name);
        result.setValue(value);
        result.setSenderTimestamp(senderTimestamp);
        return result;
    }

    public static EnvironmentNode environment(String uuid, String key, String value, long senderTimestamp) {
        final EnvironmentNode result = new EnvironmentNode();
        result.setUuid(uuid);
        result.setKey(key);
        result.setValue(value);
        result.setSenderTimestamp(senderTimestamp);
        return result;
    }

    public static TraceNode trace(String identifier, long senderStartTimeMillis, TraceNode... children) {
        final TraceNode result = new TraceNode();
        result.setIdentifier(identifier);
        result.setSenderStartTimeMillis(senderStartTimeMillis);
        result.setChildren(Arrays.asList(children));
        return result;
    }

    public static TraceRootNode traceRoot(String uuid, TraceNode trace) {
        final TraceRootNode result = new TraceRootNode();
        result.setUuid(uuid);
        result.setTrace(trace);
        return result;
    }

    public static List<CorrelationId> correlations(String... kinds) {
        final CorrelationId[] result = new CorrelationId[kinds.length];
        for(int i = 0; i < kinds.length; i++) {
            result[i] = correlationId(kinds[i], "flow-" + i);
        }
        return Arrays.asList(result);
    }

    public static CorrelationId correlationId(String kind, String ident) {
        final CorrelationId result = new CorrelationId();
        result.setKind(kind);
        result.setIdent(ident);
        return result;
    }

    public static RootNode root(String sender, String senderInstance, long senderTimestamp) {
        final RootNode result = new RootNode();
        result.setSender(sender);
        result.setSenderInstance(senderInstance);
        result.setSenderTimestamp(senderTimestamp);
        return result;
    }
}