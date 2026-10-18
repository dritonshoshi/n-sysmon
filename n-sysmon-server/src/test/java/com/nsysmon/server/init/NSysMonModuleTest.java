package com.nsysmon.server.init;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;
import com.mongodb.DB;
import com.nsysmon.server.processing.BufferingPersistenceProcessor;
import com.nsysmon.server.processing.EventBus;
import com.nsysmon.server.processing.impl.BufferingPersistenceProcessorImpl;
import com.nsysmon.server.processing.impl.EventBusImpl;
import com.nsysmon.server.services.AdminService;
import com.nsysmon.server.services.ConfigProvider;
import com.nsysmon.server.services.impl.AdminServiceImpl;
import com.nsysmon.server.services.impl.ConfigProviderImpl;
import com.nsysmon.server.storage.MonitoredApplicationDao;
import com.nsysmon.server.storage.ScalarDataDao;
import com.nsysmon.server.storage.ScalarMetaDataDao;
import com.nsysmon.server.storage.impl.MonitoredApplicationDaoImpl;
import com.nsysmon.server.storage.impl.ScalarDataDaoImpl;
import com.nsysmon.server.storage.impl.ScalarMetaDataDaoImpl;
import com.nsysmon.server.upload.preprocess.InputProcessor;
import com.nsysmon.server.upload.preprocess.SystemClockCorrector;
import com.nsysmon.server.upload.preprocess.impl.InputProcessorImpl;
import com.nsysmon.server.upload.preprocess.impl.SystemClockCorrectorNullImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NSysMonModuleTest {
    private static DB db;
    private static Injector injector;
    private static BufferingPersistenceProcessorImpl bufferingProcessor;

    /**
     * The DB binding is overridden because {@link com.nsysmon.server.storage.impl.MongoDbProvider} opens a real MongoDB
     * client. All other bindings are the ones configured by {@link NSysMonModule}.
     */
    @BeforeAll static void createInjector() {
        db = mock(DB.class);
        injector = Guice.createInjector(Modules.override(new NSysMonModule()).with(b -> b.bind(DB.class).toInstance(db)));
        bufferingProcessor = (BufferingPersistenceProcessorImpl) injector.getInstance(BufferingPersistenceProcessor.class);
    }

    @AfterAll static void shutDownWorkers() throws Exception {
        bufferingProcessor.shutdown();
    }

    @Test void bindsTheDaosToTheirMongoImplementations() {
        assertTrue(injector.getInstance(MonitoredApplicationDao.class) instanceof MonitoredApplicationDaoImpl);
        assertTrue(injector.getInstance(ScalarMetaDataDao.class) instanceof ScalarMetaDataDaoImpl);
        assertTrue(injector.getInstance(ScalarDataDao.class) instanceof ScalarDataDaoImpl);
    }

    @Test void bindsTheServicesToTheirImplementations() {
        assertTrue(injector.getInstance(AdminService.class) instanceof AdminServiceImpl);
        assertTrue(injector.getInstance(ConfigProvider.class) instanceof ConfigProviderImpl);
    }

    @Test void bindsTheUploadComponents() {
        assertTrue(injector.getInstance(InputProcessor.class) instanceof InputProcessorImpl);
        assertTrue(injector.getInstance(SystemClockCorrector.class) instanceof SystemClockCorrectorNullImpl);
    }

    @Test void databaseBindingIsReplacedByTheTestMock() {
        assertSame(db, injector.getInstance(DB.class));
    }

    @Test void eventBusIsBoundAsASingleton() {
        final EventBus underTest = injector.getInstance(EventBus.class);

        assertTrue(underTest instanceof EventBusImpl);
        assertSame(underTest, injector.getInstance(EventBus.class));
    }

    @Test void bufferingPersistenceProcessorIsBoundAsASingleton() {
        final BufferingPersistenceProcessor underTest = injector.getInstance(BufferingPersistenceProcessor.class);

        assertSame(bufferingProcessor, underTest);
    }

    @Test void inputProcessorIsASingleton() {
        assertSame(injector.getInstance(InputProcessor.class), injector.getInstance(InputProcessor.class));
    }
}
