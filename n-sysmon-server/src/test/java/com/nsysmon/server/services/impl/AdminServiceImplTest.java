package com.nsysmon.server.services.impl;

import com.nsysmon.server.storage.MonitoredApplicationDao;
import com.nsysmon.server.storage.ScalarMetaData;
import com.nsysmon.server.storage.ScalarMetaDataDao;
import com.nsysmon.server.util.AOption;
import com.nsysmon.server.util.json.ListWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {
    @Mock private MonitoredApplicationDao monitoredApplicationDao;
    @Mock private ScalarMetaDataDao scalarMetaDataDao;

    private AdminServiceImpl createService() {
        return new AdminServiceImpl(monitoredApplicationDao, scalarMetaDataDao);
    }

    @Test void allAppsEndpointWrapsTheNamesFromTheDao() {
        when(monitoredApplicationDao.getMonitoredApplicationNames()).thenReturn(List.of("app-1", "app-2"));

        final ListWrapper<String> underTest = createService().getMonitoredApplicationNames();

        assertEquals(List.of("app-1", "app-2"), underTest.getData());
    }

    @Test void allAppsEndpointReturnsAnEmptyWrapperForNoApplications() {
        when(monitoredApplicationDao.getMonitoredApplicationNames()).thenReturn(List.of());

        assertTrue(createService().getMonitoredApplicationNames().getData().isEmpty());
    }

    @Test void getReturnsTheMetaDataOfAKnownScalar() {
        when(scalarMetaDataDao.getAll()).thenReturn(Map.of("cpu.load", new ScalarMetaData("cpu.load", 3)));

        final AOption<ScalarMetaData> underTest = createService().get("cpu.load");

        assertTrue(underTest.isDefined());
        assertEquals(3, underTest.get().numFracDigits);
    }

    @Test void getIsEmptyForAnUnknownScalar() {
        when(scalarMetaDataDao.getAll()).thenReturn(Map.of("cpu.load", new ScalarMetaData("cpu.load", 3)));

        final AOption<ScalarMetaData> underTest = createService().get("heap.used");

        assertTrue(underTest.isEmpty());
    }

    @Test void getLoadsTheMetaDataFromTheDaoOnFirstAccess() {
        when(scalarMetaDataDao.getAll()).thenReturn(Map.of());

        createService().get("cpu.load");

        verify(scalarMetaDataDao).getAll();
    }

    /**
     * The two lookups happen microseconds apart, so both fall inside the ten second refresh interval and the second
     * one has to be served from the cache, even though the DAO would return different meta data.
     */
    @Test void getUsesTheCachedMetaDataWithinTheRefreshInterval() {
        when(scalarMetaDataDao.getAll())
                .thenReturn(Map.of("cpu.load", new ScalarMetaData("cpu.load", 3)))
                .thenReturn(Map.of("heap.used", new ScalarMetaData("heap.used", 1)));
        final AdminServiceImpl underTest = createService();

        underTest.get("cpu.load");
        final AOption<ScalarMetaData> result = underTest.get("heap.used");

        verify(scalarMetaDataDao, times(1)).getAll();
        assertTrue(result.isEmpty(), "the meta data should have been read from the cache");
    }

    @Test void refreshIntervalIsTenSeconds() {
        assertEquals(10_000L, AdminServiceImpl.SCALAR_META_REFRESH_INTERVAL_MILLIS);
    }

    @Test void scalarInstanceAndTimeIndexNameIsExposed() {
        assertEquals("instance-and-time", AdminServiceImpl.INDEX_NAME_SCALAR_INSTANCE_AND_TIME);
    }

    @Test void initSchemaIsCurrentlyEmptyAndTouchesNoDao() {
        createService().initSchema();

        verifyNoInteractions(monitoredApplicationDao, scalarMetaDataDao);
    }
}
