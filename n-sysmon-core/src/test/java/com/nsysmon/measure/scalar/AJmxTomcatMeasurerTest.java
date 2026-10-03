package com.nsysmon.measure.scalar;

import com.nsysmon.NSysMonApi;
import com.nsysmon.config.ADefaultConfigFactory;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.data.AScalarDataPoint;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.management.Attribute;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author arno
 */
@ExtendWith(MockitoExtension.class)
public class AJmxTomcatMeasurerTest {

    private static final String KEY_REQUEST_COUNT = "Tomcat: Request Count";
    private static final String KEY_BYTES_RECEIVED = "Tomcat: Received MB";
    private static final String KEY_BYTES_SENT = "Tomcat: Sent MB";
    private static final String KEY_ERROR_COUNT = "Tomcat: Error Count";
    private static final String KEY_THREADS_BUSY = "Tomcat: Threads Busy";

    private static final long MEGABYTE = 1024L * 1024L;

    @Mock
    private NSysMonApi sysMon;

    private AJmxTomcatMeasurer measurer;
    private Map<String, AScalarDataPoint> data;

    @BeforeAll
    public static void registerMBeans() throws Exception {
        TomcatStubMBean.register();
    }

    @AfterAll
    public static void unregisterMBeans() throws Exception {
        TomcatStubMBean.unregister();
    }

    @BeforeEach
    public void setUp() {
        measurer = new AJmxTomcatMeasurer();
        data = new HashMap<>();
        final Map<String, String> additionalConfig = new HashMap<>();
        additionalConfig.put(ADefaultConfigFactory.KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR, TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR);
        additionalConfig.put(ADefaultConfigFactory.KEY_TOMCAT_THREAD_POOL, TomcatStubMBean.NAME_THREAD_POOL);
        when(sysMon.getConfig()).thenReturn(new NSysMonConfig(null, 0, 0, 0, 0, 0, 0, 0L, 0, 0L, 0, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                "", Collections.emptyList(), additionalConfig, Boolean.TRUE, true, null, new HashMap<>(), false));
        measurer.setNSysMon(sysMon);
    }

    @AfterEach
    public void tearDown() throws Exception {
        measurer.shutdown();
    }

    private void setAttributes(int requestCount, long bytesReceived, long bytesSent, int errorCount, int threadsBusy)
            throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        server.setAttribute(new ObjectName(TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR),
                new Attribute(TomcatStubMBean.ATTR_REQUEST_COUNT, requestCount));
        server.setAttribute(new ObjectName(TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR),
                new Attribute(TomcatStubMBean.ATTR_BYTES_RECEIVED, bytesReceived));
        server.setAttribute(new ObjectName(TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR),
                new Attribute(TomcatStubMBean.ATTR_BYTES_SENT, bytesSent));
        server.setAttribute(new ObjectName(TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR),
                new Attribute(TomcatStubMBean.ATTR_ERROR_COUNT, errorCount));
        server.setAttribute(new ObjectName(TomcatStubMBean.NAME_THREAD_POOL),
                new Attribute(TomcatStubMBean.ATTR_THREADS_BUSY, threadsBusy));
    }

    @Test
    public void allAttributesAreTakenFromTheConfiguredMBeans() throws Exception {
        setAttributes(4711, MEGABYTE, 2 * MEGABYTE, 2, 3);

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(5, data.size());
        assertEquals(4711, data.get(KEY_REQUEST_COUNT).getValueRaw());
        assertEquals(2, data.get(KEY_ERROR_COUNT).getValueRaw());
        assertEquals(3, data.get(KEY_THREADS_BUSY).getValueRaw());
    }

    @Test
    public void byteCountersAreConvertedToMegabytes() throws Exception {
        setAttributes(1, MEGABYTE, 2 * MEGABYTE, 0, 0);

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(1000, data.get(KEY_BYTES_RECEIVED).getValueRaw());
        assertEquals(2000, data.get(KEY_BYTES_SENT).getValueRaw());
    }

    @Test
    public void megabyteValuesAreReportedWithThreeFractionDigits() throws Exception {
        setAttributes(1, MEGABYTE, 0, 0, 0);

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(3, data.get(KEY_BYTES_RECEIVED).getNumFracDigits());
        assertEquals(0, data.get(KEY_REQUEST_COUNT).getNumFracDigits());
    }

    @Test
    public void countersAreReportedAsAbsoluteValues() throws Exception {
        setAttributes(100, 0, 0, 0, 0);

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());
        data.clear();
        setAttributes(150, 0, 0, 0, 0);
        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        assertEquals(150, data.get(KEY_REQUEST_COUNT).getValueRaw());
    }

    @Test
    public void timestampIsPassedThroughToEveryDataPoint() throws Exception {
        setAttributes(1, MEGABYTE, MEGABYTE, 1, 1);

        measurer.contributeMeasurements(data, 123456L, new HashMap<>());

        assertTrue(data.values().stream().allMatch(d -> d.getTimestamp() == 123456L));
    }

    @Test
    public void noAveragingTimeoutIsConfigured() throws Exception {
        final Map<String, Object> mementos = new HashMap<>();

        measurer.prepareMeasurements(mementos);
        measurer.shutdown();

        assertTrue(mementos.isEmpty());
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}
