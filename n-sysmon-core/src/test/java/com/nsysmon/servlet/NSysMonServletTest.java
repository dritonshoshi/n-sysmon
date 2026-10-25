package com.nsysmon.servlet;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.testutil.RecordingServletOutputStream;
import com.nsysmon.testutil.StubPageDefinition;
import com.nsysmon.testutil.TestSysMons;
import com.nsysmon.servlet.auth.AAllowAllWebAuthorizer;
import com.nsysmon.servlet.auth.AWebAuthorizer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NSysMonServletTest {
    private RecordingPageDefinition pageDefinition;
    private RecordingServletOutputStream responseBody;
    private HttpServletResponse response;
    private NSysMonServlet servlet;

    @BeforeEach void setUp() throws IOException, ServletException {
        pageDefinition = new RecordingPageDefinition("somePage");

        responseBody = new RecordingServletOutputStream();
        response = mock(HttpServletResponse.class);
        when(response.getOutputStream()).thenReturn(responseBody);

        servlet = servletWith(AAllowAllWebAuthorizer.INSTANCE);
    }

    private NSysMonServlet servletWith(AWebAuthorizer webAuthorizer) throws ServletException {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.setWebAuthorizer(webAuthorizer);
        configBuilder.addPresentationMenuEntry("aMenu", List.of(pageDefinition, new StubPageDefinition("otherPage")));
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(configBuilder.build());

        final NSysMonServlet result = new NSysMonServlet() {
            @Override protected NSysMonApi getSysMon() {
                return sysMon;
            }
        };
        result.init();
        return result;
    }

    @Test void getSysMonDefaultsToTheSingleton() {
        final NSysMonApi mockApi = mock(NSysMonApi.class);
        try (org.mockito.MockedStatic<NSysMon> nsysMon = Mockito.mockStatic(NSysMon.class)) {
            nsysMon.when(NSysMon::get).thenReturn(mockApi);

            assertEquals(mockApi, new NSysMonServlet().getSysMon());
        }
    }

    @Test void servesNsysMonHtmlAsItsDefaultPage() {
        assertEquals("nsysmon.html", servlet.getDefaultHtmlName());
    }

    @Test void usesTheConfiguredWebAuthorizer() {
        assertSame(AAllowAllWebAuthorizer.INSTANCE, servlet.getWebAuthorizer());
    }

    @Test void rejectsRequestsThatTheConfiguredAuthorizerDenies() throws Exception {
        final AWebAuthorizer denying = mock(AWebAuthorizer.class);
        final NSysMonServlet deniedServlet = servletWith(denying);

        final HttpServletRequest deniedRequest = mock(HttpServletRequest.class);
        when(deniedRequest.getRequestURI()).thenReturn(
                "/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_SEGMENT + NSysMonServlet.CONFIG_JS);
        final HttpServletResponse deniedResponse = mock(HttpServletResponse.class);

        deniedServlet.service(deniedRequest, deniedResponse);

        verify(denying).onUnauthorized(deniedRequest, deniedResponse);
        verify(deniedResponse, never()).getOutputStream();
    }

    @Test void routesRestCallsToThePageDefinitionWithTheGivenId() throws Exception {
        assertTrue(servlet.handleRestCall(new ArrayList<>(List.of("somePage", "getData", "aParam")), response));

        assertEquals("getData", pageDefinition.lastService);
        assertEquals(List.of("aParam"), pageDefinition.lastParams);
    }

    @Test void reportsUnrestCallsThatNoPageDefinitionHandles() throws Exception {
        assertFalse(servlet.handleRestCall(new ArrayList<>(List.of("somePage", "thereIsNoSuchService")), response));

        assertFalse(pageDefinition.handled);
    }

    @Test void reportsUnrestCallsForUnknownPageIds() {
        final IllegalArgumentException exc = assertThrows(IllegalArgumentException.class,
                () -> servlet.handleRestCall(new ArrayList<>(List.of("thereIsNoSuchPage", "getData")), response));

        assertEquals("no page def with ID 'thereIsNoSuchPage'", exc.getMessage());
    }

    @Test void rejectsDuplicatePageDefinitionIds() {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.addPresentationMenuEntry("aMenu",
                List.of(new StubPageDefinition("duplicate"), new StubPageDefinition("duplicate")));
        final NSysMonConfig config = configBuilder.build();
        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(config);

        final NSysMonServlet servletWithDuplicates = new NSysMonServlet() {
            @Override protected NSysMonApi getSysMon() {
                return sysMon;
            }
        };

        final IllegalStateException exc = assertThrows(IllegalStateException.class, servletWithDuplicates::init);

        assertEquals("more than one page definitions with id 'duplicate'", exc.getMessage());
    }

    @Test void handleDynamicRejectsUnknownDynamicSegments() throws Exception {
        assertFalse(servlet.handleDynamic(List.of("somethingElse"), response));
    }

    @Test void serveConfigWritesTheAngularConfigScript() throws Exception {
        assertTrue(servlet.handleDynamic(List.of(NSysMonServlet.CONFIG_JS), response));

        final String result = responseBody.content();
        assertTrue(result.startsWith("angular.module('NSysMonApp').constant('configRaw', {"), result);
        assertTrue(result.stripTrailing().endsWith("}\n);"), result);
        assertTrue(result.contains("\"applicationId\":\"test-app\""), result);
        assertTrue(result.contains("\"applicationDeployment\":\"test-deployment\""), result);
        assertTrue(result.contains("\"applicationNode\":\"test-node\""), result);
        assertTrue(result.contains("\"applicationVersion\":\"1.0\""), result);
        assertTrue(result.contains("\"applicationInstanceHtmlColorCode\":\""), result);
    }

    @Test void serveConfigDescribesEveryRegisteredPageDefinition() throws Exception {
        servlet.handleDynamic(List.of(NSysMonServlet.CONFIG_JS), response);

        final String result = responseBody.content();
        assertTrue(result.contains("{\"label\":\"aMenu\",\"entries\":["), result);
        assertTrue(result.contains("{\"id\":\"otherPage\",\"controller\":\"CtrlotherPage\","
                + "\"htmlFileName\":\"otherPage.html\",\"shortLabel\":\"otherPage Short\","
                + "\"fullLabel\":\"otherPage Full\"}"), result);
    }

    @Test void serveConfigWritesTheDefaultPageAsNullWhenItIsNotConfigured() throws Exception {
        servlet.handleDynamic(List.of(NSysMonServlet.CONFIG_JS), response);

        assertTrue(responseBody.content().contains("\"defaultPage\":null"), responseBody.content());
    }

    /** A page definition that records how it was called. */
    private static class RecordingPageDefinition extends StubPageDefinition {
        private String lastService;
        private List<String> lastParams = List.of();
        private boolean handled;

        RecordingPageDefinition(String id) {
            super(id);
        }

        @Override public boolean handleRestCall(String service, List<String> params, AJsonSerHelperForNSysmon json) {
            lastService = service;
            lastParams = List.copyOf(params);
            handled = "getData".equals(service);
            return handled;
        }
    }
}
