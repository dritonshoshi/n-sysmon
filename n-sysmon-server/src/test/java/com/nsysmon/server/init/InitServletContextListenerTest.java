package com.nsysmon.server.init;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import jakarta.servlet.ServletContextEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class InitServletContextListenerTest {
    private final InitServletContextListener underTest = new InitServletContextListener();

    @AfterEach void clearInjector() {
        underTest.contextDestroyed(null);
    }

    @Test void injectorIsNullBeforeTheContextIsInitialized() {
        underTest.contextDestroyed(null);

        assertNull(InitServletContextListener.getInjector());
    }

    @Test void contextInitializedCreatesTheInjector() {
        final Injector injector = mock(Injector.class);

        try(MockedStatic<Guice> guice = mockStatic(Guice.class)) {
            guice.when(() -> Guice.createInjector(any(Module[].class))).thenReturn(injector);

            underTest.contextInitialized(mock(ServletContextEvent.class));

            assertSame(injector, InitServletContextListener.getInjector());
        }
    }

    @Test void contextInitializedInstallsTheNsSysMonModule() {
        try(MockedStatic<Guice> guice = mockStatic(Guice.class)) {
            guice.when(() -> Guice.createInjector(any(Module[].class))).thenReturn(mock(Injector.class));

            underTest.contextInitialized(mock(ServletContextEvent.class));

            final ArgumentCaptor<Module[]> captor = ArgumentCaptor.forClass(Module[].class);
            guice.verify(() -> Guice.createInjector(captor.capture()));
            assertSame(NSysMonModule.class, captor.getValue()[0].getClass());
        }
    }

    @Test void contextDestroyedClearsTheInjector() {
        final Injector injector = mock(Injector.class);
        try(MockedStatic<Guice> guice = mockStatic(Guice.class)) {
            guice.when(() -> Guice.createInjector(any(Module[].class))).thenReturn(injector);
            underTest.contextInitialized(mock(ServletContextEvent.class));

            underTest.contextDestroyed(mock(ServletContextEvent.class));

            assertNull(InitServletContextListener.getInjector());
        }
    }
}