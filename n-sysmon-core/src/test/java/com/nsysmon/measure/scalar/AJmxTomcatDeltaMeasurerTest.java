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
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author arno
 */
@ExtendWith(MockitoExtension.class)
public class AJmxTomcatDeltaMeasurerTest {

    private static final String KEY_REQUEST_COUNT = "Request Count";
    private static final String KEY_BYTES_RECEIVED = "Received MB";
    private static final String KEY_BYTES_SENT = "Sent MB";
    private static final String KEY_THREADS_BUSY = "Threads Busy";

    private static final long MEGABYTE = 1024L * 1024L;

    @Mock
    private NSysMonApi sysMon;

    private AJmxTomcatDeltaMeasurer measurer;
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
    public void setUp() throws Exception {
        measurer = new AJmxTomcatDeltaMeasurer();
        data = new HashMap<>();
        final Map<String, String> additionalConfig = new HashMap<>();
        additionalConfig.put(ADefaultConfigFactory.KEY_TOMCAT_GLOBAL_REQUEST_PROCESSOR, TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR);
        additionalConfig.put(ADefaultConfigFactory.KEY_TOMCAT_THREAD_POOL, TomcatStubMBean.NAME_THREAD_POOL);
        when(sysMon.getConfig()).thenReturn(new NSysMonConfig(null, 0, 0, 0, 0, 0, 0, 0L, 0, 0L, 0, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                "", Collections.emptyList(), additionalConfig, Boolean.TRUE, true, null, new HashMap<>(), false));
        measurer.setNSysMon(sysMon);
        resetFormerCounters();
    }

    @AfterEach
    public void tearDown() throws Exception {
        measurer.shutdown();
    }

    private static void resetFormerCounters() throws Exception {
        final Field requestCount = AJmxTomcatDeltaMeasurer.class.getDeclaredField("formerRequestCount");
        requestCount.setAccessible(true);
        requestCount.setInt(null, 0);
        for (String name : List.of("formerBytesReceived", "formerBytesSent")) {
            final Field field = AJmxTomcatDeltaMeasurer.class.getDeclaredField(name);
            field.setAccessible(true);
            field.setLong(null, 0L);
        }
    }

    private void setAttributes(int requestCount, long bytesReceived, long bytesSent, int threadsBusy) throws Exception {
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName processor = new ObjectName(TomcatStubMBean.NAME_GLOBAL_REQUEST_PROCESSOR);
        server.setAttribute(processor, new Attribute(TomcatStubMBean.ATTR_REQUEST_COUNT, requestCount));
        server.setAttribute(processor, new Attribute(TomcatStubMBean.ATTR_BYTES_RECEIVED, bytesReceived));
        server.setAttribute(processor, new Attribute(TomcatStubMBean.ATTR_BYTES_SENT, bytesSent));
        server.setAttribute(new ObjectName(TomcatStubMBean.NAME_THREAD_POOL),
                new Attribute(TomcatStubMBean.ATTR_THREADS_BUSY, threadsBusy));
    }

    @Test
    public void firstContributionIsADeltaToZero() throws Exception {
        setAttributes(42, MEGABYTE, 3 * MEGABYTE, 5);

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(42, data.get(KEY_REQUEST_COUNT).getValueRaw());
        assertEquals(1000, data.get(KEY_BYTES_RECEIVED).getValueRaw());
        assertEquals(3000, data.get(KEY_BYTES_SENT).getValueRaw());
        assertEquals(5, data.get(KEY_THREADS_BUSY).getValueRaw());
    }

    @Test
    public void requestCountIsReportedAsDeltaToTheFormerMeasurement() throws Exception {
        setAttributes(100, 0, 0, 0);
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());
        data.clear();
        setAttributes(175, 0, 0, 0);

        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        assertEquals(75, data.get(KEY_REQUEST_COUNT).getValueRaw());
    }

    @Test
    public void byteCountersAreReportedAsDeltaInMegabytes() throws Exception {
        setAttributes(0, 4 * MEGABYTE, 0, 0);
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());
        data.clear();
        setAttributes(0, 7 * MEGABYTE, 0, 0);

        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        assertEquals(3000, data.get(KEY_BYTES_RECEIVED).getValueRaw());
    }

    @Test
    public void busyThreadsAreReportedAsAbsoluteValue() throws Exception {
        setAttributes(0, 0, 0, 7);
        measurer.contributeMeasurements(data, 1000L, new HashMap<>());
        data.clear();
        setAttributes(0, 0, 0, 9);

        measurer.contributeMeasurements(data, 2000L, new HashMap<>());

        assertEquals(9, data.get(KEY_THREADS_BUSY).getValueRaw());
    }

    @Test
    public void megabyteValuesAreReportedWithThreeFractionDigits() throws Exception {
        setAttributes(1, MEGABYTE, 0, 0);

        measurer.contributeMeasurements(data, 1000L, new HashMap<>());

        assertEquals(3, data.get(KEY_BYTES_RECEIVED).getNumFracDigits());
        assertEquals(0, data.get(KEY_REQUEST_COUNT).getNumFracDigits());
    }

    @Test
    public void allMeasurementsBelongToTheTomcatGroup() {
        assertEquals("Tomcat", measurer.getGroupnameOfMeasurement(KEY_REQUEST_COUNT));
        assertEquals("Tomcat", measurer.getGroupnameOfMeasurement(KEY_BYTES_RECEIVED));
        assertEquals("Tomcat", measurer.getGroupnameOfMeasurement(KEY_BYTES_SENT));
        assertEquals("Tomcat", measurer.getGroupnameOfMeasurement(KEY_THREADS_BUSY));
        assertNull(measurer.getGroupnameOfMeasurement("something else"));
    }

    @Test
    public void allMeasurementsAreDescribed() {
        assertEquals("Number of requests to tomcat.", measurer.getDescriptionOfMeasurement(KEY_REQUEST_COUNT));
        assertEquals("Bytes received by tomcat.", measurer.getDescriptionOfMeasurement(KEY_BYTES_RECEIVED));
        assertEquals("Bytes sent by tomcat.", measurer.getDescriptionOfMeasurement(KEY_BYTES_SENT));
        assertEquals("Tomcat busy threads.", measurer.getDescriptionOfMeasurement(KEY_THREADS_BUSY));
        assertNull(measurer.getDescriptionOfMeasurement("something else"));
    }

    @Test
    public void measurerIsResponsibleForAllItsMeasurements() {
        assertTrue(measurer.isResponsibleForMeasurement(KEY_REQUEST_COUNT));
        assertTrue(measurer.isResponsibleForMeasurement(KEY_THREADS_BUSY));
    }

    @Test
    public void noAveragingTimeoutIsConfigured() {
        assertTrue(measurer.getTimeoutInMilliSeconds().isEmpty());
    }
}
