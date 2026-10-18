package com.nsysmon.server.upload.httpjson;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.nsysmon.server.data.InstanceIdentifier;
import com.nsysmon.server.data.json.EnvironmentNode;
import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.data.json.TraceRootNode;
import com.nsysmon.server.init.InitServletContextListener;
import com.nsysmon.server.upload.preprocess.InputProcessor;
import com.nsysmon.testutil.Nodes;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JsonConnectorServletTest {
    private static final InstanceIdentifier SENDER = new InstanceIdentifier("app-1", "instance-1");

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private InputProcessor processor;

    @AfterEach void resetInjector() {
        new InitServletContextListener().contextDestroyed(null);
    }

    @Test void forwardsTheSenderTimestampToTheClockCorrector() throws Exception {
        givenBody(Nodes.ROOT_NODE_JSON);

        service();

        verify(processor).updateSystemClockDiff(SENDER, 1700000000000L);
    }

    @Test void usesSenderAndSenderInstanceAsInstanceIdentifier() throws Exception {
        givenBody(Nodes.ROOT_NODE_JSON);

        service();

        final ArgumentCaptor<InstanceIdentifier> captor = ArgumentCaptor.forClass(InstanceIdentifier.class);
        verify(processor).addScalarEntry(captor.capture(), any());
        assertEquals(SENDER, captor.getValue());
    }

    @Test void forwardsAllEnvironmentEntries() throws Exception {
        givenBody(Nodes.ROOT_NODE_JSON);

        service();

        final ArgumentCaptor<EnvironmentNode> captor = ArgumentCaptor.forClass(EnvironmentNode.class);
        verify(processor).addEnvironmentEntry(any(), captor.capture());
        assertEquals("os.name", captor.getValue().getKey());
    }

    @Test void forwardsAllScalars() throws Exception {
        givenBody(Nodes.ROOT_NODE_JSON);

        service();

        final ArgumentCaptor<ScalarNode> captor = ArgumentCaptor.forClass(ScalarNode.class);
        verify(processor).addScalarEntry(any(), captor.capture());
        assertEquals("cpu.load", captor.getValue().getName());
    }

    @Test void forwardsAllTraces() throws Exception {
        givenBody(Nodes.ROOT_NODE_JSON);

        service();

        final ArgumentCaptor<TraceRootNode> captor = ArgumentCaptor.forClass(TraceRootNode.class);
        verify(processor).addTraceEntry(any(), captor.capture());
        assertEquals("trace-uuid", captor.getValue().getUuid());
    }

    @Test void acceptsAPayloadWithoutAnyData() throws Exception {
        givenBody("{\"sender\":\"app-1\",\"senderInstance\":\"instance-1\",\"senderTimestamp\":1}");

        service();

        verify(processor).updateSystemClockDiff(SENDER, 1L);
        verifyNoMoreInteractions(processor);
    }

    @Test void failsOnAMalformedPayload() throws Exception {
        givenBody("{not json");

        assertThrows(IOException.class, () -> service());

        verifyNoInteractions(processor);
    }

    @Test void getProcessorLooksUpTheInputProcessorInTheInjector() {
        final Injector injector = mock(Injector.class);
        when(injector.getInstance(InputProcessor.class)).thenReturn(processor);
        try(MockedStatic<Guice> guice = mockStatic(Guice.class)) {
            guice.when(() -> Guice.createInjector(any(Module[].class))).thenReturn(injector);
            new InitServletContextListener().contextInitialized(null);
        }

        assertEquals(processor, new JsonConnectorServlet().getProcessor());
    }

    private void service() throws Exception {
        new JsonConnectorServlet() {
            @Override protected InputProcessor getProcessor() {
                return processor;
            }
        }.service(request, response);
    }

    private void givenBody(String json) throws IOException {
        when(request.getInputStream()).thenReturn(new ByteArrayServletInputStream(json));
    }

    private static final class ByteArrayServletInputStream extends ServletInputStream {
        private final InputStream delegate;

        private ByteArrayServletInputStream(String content) {
            this.delegate = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        }

        @Override public int read() throws IOException {
            return delegate.read();
        }

        @Override public boolean isFinished() {
            return false;
        }

        @Override public boolean isReady() {
            return true;
        }

        @Override public void setReadListener(ReadListener readListener) {
        }
    }
}
