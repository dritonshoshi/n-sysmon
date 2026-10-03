package com.nsysmon.servlet.correlationflow;

import com.ajjpj.afoundation.io.AJsonSerHelperForNSysmon;
import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.data.ACorrelationId;
import com.nsysmon.measure.ASimpleMeasurement;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class CorrelationFlowPageIoErrorTest {
    @Mock private NSysMonApi sysMonApi;

    private MockedStatic<NSysMon> correlationFlowEnabled;
    private NSysMonApi sysMon;
    private CorrelationFlowPage page;

    @BeforeEach void setUp() {
        lenient().when(sysMonApi.getConfig()).thenReturn(TestSysMons.config(false));
        correlationFlowEnabled = Mockito.mockStatic(NSysMon.class);
        correlationFlowEnabled.when(NSysMon::get).thenReturn(sysMonApi);

        sysMon = TestSysMons.create();
        page = new CorrelationFlowPage(10);
        page.init(sysMon);
    }

    @AfterEach void tearDown() {
        correlationFlowEnabled.close();
    }

    private void givenOneFlow() {
        final ASimpleMeasurement measurement = sysMon.start("m");
        sysMon.startFlow(new ACorrelationId("parent", "1", null));
        sysMon.joinFlow(new ACorrelationId("child", "2", "1"));
        measurement.finish();
    }

    @Test void survivesAnIoErrorWhileWritingARootNode() throws Exception {
        givenOneFlow();
        final AJsonSerHelperForNSysmon failingJson = mock(AJsonSerHelperForNSysmon.class, withSettings().lenient());
        doThrow(new IOException("connection closed")).when(failingJson).writeStringLiteral(anyString());

        assertTrue(page.handleRestCall("getData", List.of(), failingJson));
    }

    @Test void survivesAnIoErrorWhileWritingAChildNode() throws Exception {
        givenOneFlow();
        final AJsonSerHelperForNSysmon failingJson = mock(AJsonSerHelperForNSysmon.class, withSettings().lenient());
        doThrow(new IOException("connection closed")).when(failingJson).writeStringLiteral("child");

        assertTrue(page.handleRestCall("getData", List.of(), failingJson));

        Mockito.verify(failingJson).writeStringLiteral("child");
    }
}
