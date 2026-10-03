package com.nsysmon.measure.http;

import com.ajjpj.afoundation.collection.immutable.AOption;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author arno
 */
@ExtendWith(MockitoExtension.class)
public class ASimpleHttpRequestAnalyzerTest {

    @Mock
    private HttpServletRequest request;

    private ASimpleHttpRequestAnalyzer analyzer;

    @BeforeEach
    public void setUp() {
        analyzer = new ASimpleHttpRequestAnalyzer();
    }

    @Test
    public void identifierIsAlwaysTheSamePlaceholder() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/app?order=1"));

        final AOption<String> identifier = analyzer.analyze(request).getIdentifier();

        assertTrue(identifier.isDefined());
        assertEquals("<http request>", identifier.get());
    }

    @Test
    public void parametersContainFullUrlAndRemoteAddress() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host:8080/app/servlet"));
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        final Map<String, String> parameters = analyzer.analyze(request).getParameters();

        assertEquals(2, parameters.size());
        assertEquals("http://host:8080/app/servlet", parameters.get(ASimpleHttpRequestAnalyzer.PARAM_FULL_URL));
        assertEquals("10.0.0.1", parameters.get(ASimpleHttpRequestAnalyzer.PARAM_REMOTE_ADDR));
    }

    @Test
    public void nullCookiesAreIgnored() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/app"));
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getCookies()).thenReturn(null);

        final Map<String, String> parameters = analyzer.analyze(request).getParameters();

        assertFalse(parameters.containsKey(ASimpleHttpRequestAnalyzer.PARAM_PREFIX_COOKIE + "session"));
        assertEquals(2, parameters.size());
    }

    @Test
    public void cookiesAreAddedWithPrefixedNames() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/app"));
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("session", "abc"), new Cookie("lang", "de")});

        final Map<String, String> parameters = analyzer.analyze(request).getParameters();

        assertEquals("abc", parameters.get("http-cookie-session"));
        assertEquals("de", parameters.get("http-cookie-lang"));
        assertEquals(4, parameters.size());
    }

    @Test
    public void extractParametersIsUsableWithoutFullAnalysis() {
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://host/app"));
        when(request.getRemoteAddr()).thenReturn("::1");
        when(request.getCookies()).thenReturn(new Cookie[0]);

        final Map<String, String> parameters = analyzer.extractParameters(request);

        assertEquals("::1", parameters.get(ASimpleHttpRequestAnalyzer.PARAM_REMOTE_ADDR));
    }

    @Test
    public void extractIdentifierIgnoresTheUrl() {
        assertEquals("<http request>", analyzer.extractIdentifier("http://host/whatever").get());
        assertEquals("<http request>", analyzer.extractIdentifier("").get());
    }

    @Test
    public void subclassesCanNarrowTheIdentifier() {
        final ASimpleHttpRequestAnalyzer strippingQuery = new ASimpleHttpRequestAnalyzer() {
            @Override protected AOption<String> extractIdentifier(String url) {
                return AOption.some(url.split("\\?")[0]);
            }
        };

        assertEquals("http://host/app", strippingQuery.extractIdentifier("http://host/app?x=1").get());
    }
}