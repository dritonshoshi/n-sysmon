package com.nsysmon.measure.http;

import com.ajjpj.afoundation.collection.immutable.AOption;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.measure.AMeasureCallbackVoid;
import com.nsysmon.measure.AWithParameters;
import com.nsysmon.servlet.auth.AAllowAllWebAuthorizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @author arno
 */
@ExtendWith(MockitoExtension.class)
public class AHttpRequestMeasuringFilterTest {

    public static class TestAnalyzer implements AHttpRequestAnalyzer {
        static AOption<String> identifier = AOption.none();
        static Map<String, String> parameters = Collections.emptyMap();

        @Override public AHttpRequestDetails analyze(HttpServletRequest request) {
            return new AHttpRequestDetails() {
                @Override public AOption<String> getIdentifier() {
                    return identifier;
                }
                @Override public Map<String, String> getParameters() {
                    return parameters;
                }
            };
        }
    }

    private static class TestFilter extends AHttpRequestMeasuringFilter {
        private final NSysMonApi sysMon;

        TestFilter(NSysMonApi sysMon) {
            this.sysMon = sysMon;
        }

        @Override protected NSysMonApi getSysMon() {
            return sysMon;
        }
    }

    static NSysMonConfig configWith(AHttpRequestAnalyzer analyzer) {
        return new NSysMonConfig(null, 0, 0, 0, 0, 0, 0, 0L, 0, 0L, 0, null, analyzer,
                AAllowAllWebAuthorizer.INSTANCE,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                "", Collections.emptyList(), new HashMap<>(), Boolean.TRUE, true, null, new HashMap<>(), false);
    }

    @Mock
    private NSysMonApi sysMon;
    @Mock
    private FilterConfig filterConfig;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private AWithParameters measurement;

    private AHttpRequestMeasuringFilter filter;

    @BeforeEach
    public void setUp() {
        TestAnalyzer.identifier = AOption.none();
        TestAnalyzer.parameters = Collections.emptyMap();
        filter = new TestFilter(sysMon);
    }

    private void stubMeasureToRunTheCallback() throws Exception {
        doAnswer(invocation -> {
            invocation.<AMeasureCallbackVoid<Exception>>getArgument(1).call(measurement);
            return null;
        }).when(sysMon).measure(anyString(), any(AMeasureCallbackVoid.class));
    }

    @Test
    public void analyzerIsTakenFromTheConfigIfNoClassNameIsConfigured() throws Exception {
        final ASimpleHttpRequestAnalyzer configured = new ASimpleHttpRequestAnalyzer();
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN)).thenReturn(null);
        when(sysMon.getConfig()).thenReturn(configWith(configured));

        assertSame(configured, filter.createAnalyzer(filterConfig));
    }

    @Test
    public void analyzerIsInstantiatedFromTheConfiguredClassName() throws Exception {
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN))
                .thenReturn(TestAnalyzer.class.getName());

        assertSame(TestAnalyzer.class, filter.createAnalyzer(filterConfig).getClass());
    }

    @Test
    public void initFailsWithServletExceptionIfTheAnalyzerClassIsUnknown() {
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN))
                .thenReturn("does.not.Exist");

        assertThrows(ServletException.class, () -> filter.init(filterConfig));
    }

    @Test
    public void measuredRequestPassesAllParametersToTheMeasurement() throws Exception {
        TestAnalyzer.identifier = AOption.some("my-request");
        TestAnalyzer.parameters = Map.of("k1", "v1", "k2", "v2");
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN))
                .thenReturn(TestAnalyzer.class.getName());
        stubMeasureToRunTheCallback();
        filter.init(filterConfig);

        filter.doFilter(request, response, chain);

        verify(measurement).addParameter("k1", "v1");
        verify(measurement).addParameter("k2", "v2");
        verify(chain).doFilter(request, response);
    }

    @Test
    public void identifierOfTheAnalyzerIsUsedAsMeasurementIdentifier() throws Exception {
        TestAnalyzer.identifier = AOption.some("the-identifier");
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN))
                .thenReturn(TestAnalyzer.class.getName());
        stubMeasureToRunTheCallback();
        filter.init(filterConfig);

        filter.doFilter(request, response, chain);

        verify(sysMon).measure(org.mockito.ArgumentMatchers.eq("the-identifier"), any(AMeasureCallbackVoid.class));
    }

    @Test
    public void requestsWithoutIdentifierArePassedThroughUnmeasured() throws Exception {
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN))
                .thenReturn(TestAnalyzer.class.getName());
        filter.init(filterConfig);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(measurement);
        verify(sysMon, never()).measure(anyString(), any(AMeasureCallbackVoid.class));
    }

    @Test
    public void filterKeepsWorkingAfterDestroy() throws Exception {
        TestAnalyzer.identifier = AOption.some("after-destroy");
        when(filterConfig.getInitParameter(AHttpRequestMeasuringFilter.PARAM_ANALYZER_CLASS_FQN))
                .thenReturn(TestAnalyzer.class.getName());
        stubMeasureToRunTheCallback();
        filter.init(filterConfig);

        filter.destroy();
        filter.doFilter(request, response, chain);

        verify(sysMon).measure(org.mockito.ArgumentMatchers.eq("after-destroy"), any(AMeasureCallbackVoid.class));
        verify(chain).doFilter(request, response);
    }
}
