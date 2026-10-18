package com.nsysmon.demo;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.measure.http.ASimpleHttpRequestAnalyzer;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NullHttpRequestAnalyzerTest {

    private static final String URL = "http://localhost:8181/content/small/page.html";
    private static final String NSYSMON_URL = "http://localhost:8181/nsysmon/index.html";

    private final NullHttpRequestAnalyzer analyzer = new NullHttpRequestAnalyzer();

    private HttpServletRequest request;

    @BeforeEach void createRequest() {
        request = mock(HttpServletRequest.class);
        when(request.getRequestURL()).thenReturn(new StringBuffer(URL));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test void extractIdentifierReturnsTheUrlForRegularContent() {
        final AOption<String> identifier = analyzer.extractIdentifier(URL);

        assertTrue(identifier.isDefined());
        assertEquals(URL, identifier.get());
    }

    @Test void extractIdentifierDropsEveryNsysmonUrl() {
        assertTrue(analyzer.extractIdentifier(NSYSMON_URL).isEmpty());
        assertTrue(analyzer.extractIdentifier("http://localhost:8181/nsysmon").isEmpty());
        assertTrue(analyzer.extractIdentifier("http://localhost:8181/content/nsysmon-report.html").isEmpty());
    }

    @Test void analyzeKeepsTheFullUrlAsIdentifierForRegularContent() {
        assertEquals(URL, analyzer.analyze(request).getIdentifier().get());
    }

    @Test void analyzeDropsTheIdentifierForNsysmonUrls() {
        when(request.getRequestURL()).thenReturn(new StringBuffer(NSYSMON_URL));

        assertTrue(analyzer.analyze(request).getIdentifier().isEmpty());
    }

    @Test void analyzeCollectsUrlAndRemoteAddressAsParameters() {
        final Map<String, String> parameters = analyzer.analyze(request).getParameters();

        assertEquals(URL, parameters.get(ASimpleHttpRequestAnalyzer.PARAM_FULL_URL));
        assertEquals("127.0.0.1", parameters.get(ASimpleHttpRequestAnalyzer.PARAM_REMOTE_ADDR));
    }

    @Test void analyzeCollectsCookieValuesAsParameters() {
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("session", "abc"), new Cookie("theme", "dark") });

        final Map<String, String> parameters = analyzer.analyze(request).getParameters();

        assertEquals("abc", parameters.get(ASimpleHttpRequestAnalyzer.PARAM_PREFIX_COOKIE + "session"));
        assertEquals("dark", parameters.get(ASimpleHttpRequestAnalyzer.PARAM_PREFIX_COOKIE + "theme"));
    }

    @Test void analyzeWorksForRequestsWithoutCookies() {
        when(request.getCookies()).thenReturn(null);

        final Map<String, String> parameters = analyzer.analyze(request).getParameters();

        assertFalse(parameters.containsKey(ASimpleHttpRequestAnalyzer.PARAM_PREFIX_COOKIE));
        assertEquals(2, parameters.size());
    }
}
