package com.nsysmon.measure.scalar;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

/**
 * A minimal MBean that just hands out pre-set attribute values, so the Tomcat measurers can be driven with
 * deterministic numbers without a servlet container.
 */
class TomcatStubMBean implements DynamicMBean {

    static final String NAME_GLOBAL_REQUEST_PROCESSOR = "com.nsysmon.test:type=GlobalRequestProcessor";
    static final String NAME_THREAD_POOL = "com.nsysmon.test:type=ThreadPool";

    static final String ATTR_REQUEST_COUNT = "requestCount";
    static final String ATTR_BYTES_RECEIVED = "bytesReceived";
    static final String ATTR_BYTES_SENT = "bytesSent";
    static final String ATTR_ERROR_COUNT = "errorCount";
    static final String ATTR_THREADS_BUSY = "currentThreadsBusy";

    private final Map<String, Object> attributes = new HashMap<>();

    TomcatStubMBean put(String name, Object value) {
        attributes.put(name, value);
        return this;
    }

    static void register() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        if (!server.isRegistered(new ObjectName(NAME_GLOBAL_REQUEST_PROCESSOR))) {
            server.registerMBean(new TomcatStubMBean().put(ATTR_REQUEST_COUNT, 0).put(ATTR_ERROR_COUNT, 0)
                    .put(ATTR_BYTES_RECEIVED, 0L).put(ATTR_BYTES_SENT, 0L), new ObjectName(NAME_GLOBAL_REQUEST_PROCESSOR));
        }
        if (!server.isRegistered(new ObjectName(NAME_THREAD_POOL))) {
            server.registerMBean(new TomcatStubMBean().put(ATTR_THREADS_BUSY, 0), new ObjectName(NAME_THREAD_POOL));
        }
    }

    static void unregister() throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        if (server.isRegistered(new ObjectName(NAME_GLOBAL_REQUEST_PROCESSOR))) {
            server.unregisterMBean(new ObjectName(NAME_GLOBAL_REQUEST_PROCESSOR));
        }
        if (server.isRegistered(new ObjectName(NAME_THREAD_POOL))) {
            server.unregisterMBean(new ObjectName(NAME_THREAD_POOL));
        }
    }

    @Override public Object getAttribute(String attribute) throws AttributeNotFoundException {
        if (!attributes.containsKey(attribute)) {
            throw new AttributeNotFoundException(attribute);
        }
        return attributes.get(attribute);
    }

    @Override public void setAttribute(Attribute attribute) {
        attributes.put(attribute.getName(), attribute.getValue());
    }

    @Override public AttributeList getAttributes(String[] names) {
        final AttributeList result = new AttributeList();
        for (String name : names) {
            if (attributes.containsKey(name)) {
                result.add(new Attribute(name, attributes.get(name)));
            }
        }
        return result;
    }

    @Override public AttributeList setAttributes(AttributeList list) {
        for (Object o : list) {
            final Attribute attribute = (Attribute) o;
            attributes.put(attribute.getName(), attribute.getValue());
        }
        return list;
    }

    @Override public Object invoke(String actionName, Object[] params, String[] signature) {
        throw new UnsupportedOperationException(actionName);
    }

    @Override public MBeanInfo getMBeanInfo() {
        return new MBeanInfo(getClass().getName(), "stub", new MBeanAttributeInfo[0], null, null, null);
    }
}
