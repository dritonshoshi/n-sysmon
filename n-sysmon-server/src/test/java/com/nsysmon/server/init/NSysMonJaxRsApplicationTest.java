package com.nsysmon.server.init;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.nsysmon.server.services.AdminService;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class NSysMonJaxRsApplicationTest {
    private final InitServletContextListener listener = new InitServletContextListener();

    private AdminService adminService;
    private NSysMonJaxRsApplication underTest;

    @BeforeEach void createApplication() {
        adminService = mock(AdminService.class);
        final Injector injector = mock(Injector.class);
        when(injector.getInstance(AdminService.class)).thenReturn(adminService);
        try(MockedStatic<Guice> guice = mockStatic(Guice.class)) {
            guice.when(() -> Guice.createInjector(any(Module[].class))).thenReturn(injector);
            listener.contextInitialized(null);
        }
        underTest = new NSysMonJaxRsApplication();
    }

    @AfterEach void clearInjector() {
        listener.contextDestroyed(null);
    }

    @Test void registersTheAdminServiceAsSingletonResource() {
        assertTrue(underTest.getInstances().contains(adminService));
    }

    @Test void registersJacksonForJsonSerialization() {
        assertTrue(underTest.isRegistered(JacksonFeature.class));
    }

    @Test void doesNotRegisterTheAdminServiceClass() {
        assertEquals(1, underTest.getInstances().size());
    }
}