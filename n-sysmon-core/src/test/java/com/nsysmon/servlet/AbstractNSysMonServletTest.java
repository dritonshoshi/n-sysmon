package com.nsysmon.servlet;

import com.nsysmon.testutil.RecordingServletOutputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractNSysMonServletTest {
    private static final String REST = "/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_REST;

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private RecordingServletOutputStream responseBody;
    private TestServlet servlet;

    @BeforeEach void setUp() throws IOException {
        responseBody = new RecordingServletOutputStream();
        lenient().when(response.getOutputStream()).thenReturn(responseBody);
        servlet = new TestServlet();
    }

    private void givenUri(String uri) {
        when(request.getRequestURI()).thenReturn(uri);
    }

    @Test void recognisesTheContentTypesOfItsOwnStaticResources() {
        assertEquals("text/html", servlet.getContentType("partials/main.html"));
        assertEquals("text/css", servlet.getContentType("css/main.css"));
        assertEquals("text/javascript", servlet.getContentType("js/main.js"));
        assertNull(servlet.getContentType("fonts/icon.woff"));
    }

    @Test void rejectsStaticResourceNamesWithParentDirectorySegments() {
        assertThrows(IllegalArgumentException.class, () -> {
            givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_STATIC + "../secrets.txt");
            servlet.service(request, response);
        });
    }

    @Test void rejectsStaticResourceNamesThatAreAbsolutePaths() {
        assertThrows(IllegalArgumentException.class, () -> {
            givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_STATIC + "/etc/passwd");
            servlet.service(request, response);
        });
    }

    @Test void rejectsStaticResourceNamesWithEmptyPathSegments() {
        assertThrows(IllegalArgumentException.class, () -> {
            givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_STATIC + "css//main.css");
            servlet.service(request, response);
        });
    }

    @Test void servesTheStaticResourcesOfNsysMon() throws Exception {
        givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_STATIC + "nsysmon.html");

        servlet.service(request, response);

        verify(response).setContentType("text/html");
        verify(response).addHeader("Cache-Control", "no-cache");
        assertTrue(responseBody.content().contains("<html"), responseBody.content());
    }

    @Test void cachesStaticResourcesOtherThanHtml() throws Exception {
        givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_STATIC + "js/nsysmon-app.js");

        servlet.service(request, response);

        verify(response).setContentType("text/javascript");
        verify(response).addHeader("Cache-Control", "max-age=36000");
    }

    @Test void respondsWith404ForUnknownStaticResources() throws Exception {
        givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_STATIC + "there-is-no-such-file.html");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertEquals("", responseBody.content());
    }

    @Test void servesTheDefaultHtmlNameForTheContextRoot() throws Exception {
        givenUri("/nsysmon/");

        servlet.service(request, response);

        verify(response).setContentType("text/html");
        assertTrue(responseBody.content().contains("<html"), responseBody.content());
    }

    @Test void redirectsToTheContextRootWithATrailingSlash() throws Exception {
        givenUri("/nsysmon");

        servlet.service(request, response);

        verify(response).sendRedirect("/nsysmon/");
    }

    @Test void dispatchesRestCallsWithAllPathSegments() throws Exception {
        givenUri(REST + "somePage/getData/param1/param2");

        servlet.service(request, response);

        verify(response).setContentType("application/json; charset=UTF-8");
        assertEquals(List.of("somePage", "getData", "param1", "param2"), servlet.lastRestSegments);
    }

    @Test void respondsWith404ForDynamicSegmentsThatCannotBeHandled() throws Exception {
        givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_SEGMENT + "somethingElse");

        servlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        assertEquals(List.of("somethingElse"), servlet.lastDynamicSegments);
    }

    @Test void doesNotSetTheStatusForHandledDynamicSegments() throws Exception {
        givenUri("/nsysmon" + AbstractNSysMonServlet.NSYSMON_MARKER_SEGMENT + "somethingElse");
        servlet.dynamicResult = true;

        servlet.service(request, response);

        verify(response, never()).setStatus(anyInt());
    }

    @Test void writesAnErrorDocumentWhenARestCallIsNotSupported() throws Exception {
        givenUri(REST + "somePage/thereIsNoSuchService");
        servlet.restResult = false;

        servlet.service(request, response);

        verify(response).setStatus(599);
        assertTrue(responseBody.content().contains("\"msg\":\"unsupported REST call:"), responseBody.content());
        assertTrue(responseBody.content().contains("\"details\":["), responseBody.content());
    }

    @Test void reportsTheMessageOfAFailingRestCallAsJson() throws Exception {
        givenUri(REST + "somePage/thereIsNoSuchService");
        servlet.restException = new IllegalStateException("something went wrong");

        servlet.service(request, response);

        assertTrue(responseBody.content().contains("\"msg\":\"something went wrong\""), responseBody.content());
    }

    @Test void omitsTheMessageForExceptionsWithoutOne() throws Exception {
        givenUri(REST + "somePage/thereIsNoSuchService");
        servlet.restException = new IllegalStateException();

        servlet.service(request, response);

        assertFalse(responseBody.content().contains("\"msg\""), responseBody.content());
    }

    @Test void reportsAFailureToWriteTheErrorDocumentAsServletException() throws IOException {
        givenUri(REST + "somePage/thereIsNoSuchService");
        servlet.restResult = false;
        when(response.getOutputStream()).thenThrow(new IllegalStateException("connection closed"));

        final ServletException exc = assertThrows(ServletException.class, () -> servlet.service(request, response));

        assertTrue(exc.getCause() instanceof IllegalArgumentException, String.valueOf(exc.getCause()));
    }

    @Test void handleDynamicReturnsFalseByDefault() throws Exception {
        assertFalse(new MinimalServlet().handleDynamic(List.of("whatever"), response));
    }

    /** A servlet that relies on the defaults of the abstract base class. */
    private static final class MinimalServlet extends AbstractNSysMonServlet {
        @Override protected String getDefaultHtmlName() {
            return "nsysmon.html";
        }

        @Override protected boolean handleRestCall(List<String> pathSegments, HttpServletResponse resp) {
            return true;
        }
    }

    /** Captures the segments the abstract class passes on and yields canned results. */
    private static final class TestServlet extends AbstractNSysMonServlet {
        private List<String> lastRestSegments = List.of();
        private List<String> lastDynamicSegments = List.of();
        private boolean restResult = true;
        private boolean dynamicResult = false;
        private Exception restException;

        @Override protected String getDefaultHtmlName() {
            return "nsysmon.html";
        }

        @Override protected boolean handleRestCall(List<String> pathSegments, HttpServletResponse resp) throws Exception {
            lastRestSegments = List.copyOf(pathSegments);
            if (restException != null) {
                throw restException;
            }
            return restResult;
        }

        @Override protected boolean handleDynamic(List<String> pathSegments, HttpServletResponse resp) {
            lastDynamicSegments = List.copyOf(pathSegments);
            return dynamicResult;
        }

        @Override protected String getContentType(String resName) {
            return super.getContentType(resName);
        }
    }
}
