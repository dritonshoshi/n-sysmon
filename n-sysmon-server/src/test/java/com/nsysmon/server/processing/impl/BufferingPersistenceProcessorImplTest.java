package com.nsysmon.server.processing.impl;

import com.nsysmon.server.data.json.ScalarNode;
import com.nsysmon.server.processing.EventBus;
import com.nsysmon.server.processing.NewDataListener;
import com.nsysmon.server.services.ConfigData;
import com.nsysmon.server.services.ConfigProvider;
import com.nsysmon.server.storage.ScalarDataDao;
import com.nsysmon.testutil.Nodes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BufferingPersistenceProcessorImplTest {
    private static final long TIMEOUT_SECONDS = 10;
    private static final long NO_ACTIVITY_MILLIS = 200;

    @Mock private EventBus eventBus;
    @Mock private ScalarDataDao scalarDataDao;
    @Mock private ConfigProvider configProvider;

    private final List<BufferingPersistenceProcessorImpl> processors = new ArrayList<>();

    @AfterEach void shutDownAllProcessors() throws Exception {
        for(BufferingPersistenceProcessorImpl p: processors) {
            p.shutdown();
        }
    }

    @Test void asksTheConfigProviderForItsConfiguration() {
        createProcessor(eventBus, config());

        verify(configProvider).getConfigData();
    }

    @Test void registersAListenerOnTheEventBus() {
        createProcessor(eventBus, config());

        final ArgumentCaptor<NewDataListener> captor = ArgumentCaptor.forClass(NewDataListener.class);
        verify(eventBus).addListener(captor.capture());

        assertNotNull(captor.getValue());
    }

    @Test void shutdownRemovesItsListenerFromTheEventBus() throws Exception {
        final ArgumentCaptor<NewDataListener> captor = ArgumentCaptor.forClass(NewDataListener.class);
        final BufferingPersistenceProcessorImpl underTest = createProcessor(eventBus, config());
        verify(eventBus).addListener(captor.capture());

        underTest.shutdown();

        verify(eventBus).removeListener(captor.getValue());
    }

    @Test void storesScalarDataFiredOnTheEventBus() throws Exception {
        final CountDownLatch stored = new CountDownLatch(1);
        storeScalarDataCollecting(new CopyOnWriteArrayList<>(), stored);
        final EventBusImpl bus = new EventBusImpl();
        createProcessor(bus, config());
        final ScalarNode scalar = Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L);

        bus.fireNewScalarData(scalar);

        assertTrue(stored.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "scalar data was never stored");
        final ArgumentCaptor<ScalarNode> captor = ArgumentCaptor.forClass(ScalarNode.class);
        verify(scalarDataDao).storeScalarData(captor.capture());
        assertSame(scalar, captor.getValue());
    }

    @Test void storesScalarDataOfSeveralEventsInTheSameOrder() throws Exception {
        final CountDownLatch stored = new CountDownLatch(2);
        final List<ScalarNode> storedNodes = new CopyOnWriteArrayList<>();
        storeScalarDataCollecting(storedNodes, stored);
        final EventBusImpl bus = new EventBusImpl();
        createProcessor(bus, config());
        final ScalarNode first = Nodes.scalar("uuid-1", "cpu.load", 1.0, 10L);
        final ScalarNode second = Nodes.scalar("uuid-2", "cpu.load", 2.0, 11L);

        bus.fireNewScalarData(first);
        bus.fireNewScalarData(second);

        assertTrue(stored.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "both scalar values were not stored");
        assertEquals(List.of(first, second), storedNodes);
    }

    @Test void doesNotStoreScalarDataAfterShutdown() throws Exception {
        final CountDownLatch stored = new CountDownLatch(1);
        storeScalarDataCollecting(new CopyOnWriteArrayList<>(), stored);
        final EventBusImpl bus = new EventBusImpl();
        final BufferingPersistenceProcessorImpl underTest = createProcessor(bus, config());

        underTest.shutdown();
        bus.fireNewScalarData(Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L));

        assertFalse(stored.await(NO_ACTIVITY_MILLIS, TimeUnit.MILLISECONDS), "data was stored after shutdown");
    }

    @Test void discardsTheOldestScalarDataWhenTheQueueIsFull() throws Exception {
        final CountDownLatch firstStoreEntered = new CountDownLatch(1);
        final CountDownLatch releaseFirstStore = new CountDownLatch(1);
        final CountDownLatch lastStored = new CountDownLatch(1);
        final List<ScalarNode> storedNodes = new CopyOnWriteArrayList<>();
        blockFirstStore(storedNodes, firstStoreEntered, releaseFirstStore, lastStored);
        final EventBusImpl bus = new EventBusImpl();
        createProcessor(bus, config(1));
        final ScalarNode first = Nodes.scalar("uuid-1", "cpu.load", 1.0, 10L);
        final ScalarNode discardedByThird = Nodes.scalar("uuid-2", "cpu.load", 2.0, 11L);
        final ScalarNode discardedByFourth = Nodes.scalar("uuid-3", "cpu.load", 3.0, 12L);
        final ScalarNode last = Nodes.scalar("uuid-4", "cpu.load", 4.0, 13L);

        bus.fireNewScalarData(first);
        assertTrue(firstStoreEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "first value was never stored");
        bus.fireNewScalarData(discardedByThird);
        bus.fireNewScalarData(discardedByFourth);
        bus.fireNewScalarData(last);
        releaseFirstStore.countDown();

        assertTrue(lastStored.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "last value was never stored");
        assertEquals(List.of("uuid-1", "uuid-4"), storedNodes.stream().map(ScalarNode::getUuid).toList());
    }

    @Test void shutdownStopsTheWorkerThreads() throws Exception {
        final CountDownLatch storeEntered = new CountDownLatch(1);
        final CountDownLatch releaseStore = new CountDownLatch(1);
        final AtomicReference<Thread> workerThread = new AtomicReference<>();
        blockStoreIn(workerThread, storeEntered, releaseStore);
        final EventBusImpl bus = new EventBusImpl();
        final BufferingPersistenceProcessorImpl underTest = createProcessor(bus, config());

        bus.fireNewScalarData(Nodes.scalar("scalar-uuid", "cpu.load", 0.75, 10L));
        assertTrue(storeEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "store was never started");
        underTest.shutdown();
        releaseStore.countDown();

        workerThread.get().join(TIMEOUT_SECONDS * 1000);
        assertFalse(workerThread.get().isAlive(), "worker thread did not terminate");
    }

    @Test void keepsStoringDataAfterAStoreFails() throws Exception {
        final CountDownLatch storedAfterFailure = new CountDownLatch(1);
        failFirstStore(storedAfterFailure);
        final EventBusImpl bus = new EventBusImpl();
        createProcessor(bus, config());

        bus.fireNewScalarData(Nodes.scalar("uuid-1", "cpu.load", 1.0, 10L));
        bus.fireNewScalarData(Nodes.scalar("uuid-2", "cpu.load", 2.0, 11L));

        assertTrue(storedAfterFailure.await(TIMEOUT_SECONDS, TimeUnit.SECONDS), "worker stopped after a failed store");
    }

    @Test void doesNotPersistTraceAndEnvironmentData() throws Exception {
        final CountDownLatch stored = new CountDownLatch(1);
        storeScalarDataCollecting(new CopyOnWriteArrayList<>(), stored);
        final EventBusImpl bus = new EventBusImpl();
        createProcessor(bus, config());

        bus.fireNewTrace(Nodes.traceRoot("trace-uuid", Nodes.trace("root", 10L)));
        bus.fireNewEnvironmentData(Nodes.environment("env-uuid", "os.name", "Linux", 10L));

        assertFalse(stored.await(NO_ACTIVITY_MILLIS, TimeUnit.MILLISECONDS), "trace/environment data was persisted");
    }

    private BufferingPersistenceProcessorImpl createProcessor(EventBus bus, ConfigData config) {
        when(configProvider.getConfigData()).thenReturn(config);
        final BufferingPersistenceProcessorImpl result = new BufferingPersistenceProcessorImpl(bus, scalarDataDao, configProvider);
        processors.add(result);
        return result;
    }

    /**
     * Records every stored scalar node in {@code collected} and counts {@code stored} down once per store. The stubbing
     * is lenient because the tests that assert that nothing is stored never trigger it.
     */
    private void storeScalarDataCollecting(List<ScalarNode> collected, CountDownLatch stored) {
        lenient().doAnswer(invocation -> {
            collected.add(invocation.getArgument(0));
            stored.countDown();
            return null;
        }).when(scalarDataDao).storeScalarData(any());
    }

    /**
     * Like {@link #storeScalarDataCollecting}, but the store of the first node blocks until
     * {@code releaseFirstStore} is counted down, so that a test can fill up the queue while a store is in progress.
     * {@code lastStored} is counted down once the second node has been stored.
     */
    private void blockFirstStore(List<ScalarNode> collected, CountDownLatch firstStoreEntered,
                                 CountDownLatch releaseFirstStore, CountDownLatch lastStored) throws Exception {
        doAnswer(invocation -> {
            collected.add(invocation.getArgument(0));
            if(collected.size() == 1) {
                firstStoreEntered.countDown();
                releaseFirstStore.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            }
            if(collected.size() == 2) {
                lastStored.countDown();
            }
            return null;
        }).when(scalarDataDao).storeScalarData(any());
    }

    /**
     * Records the thread that stores the scalar data and blocks it until {@code releaseStore} is counted down.
     */
    private void blockStoreIn(AtomicReference<Thread> workerThread,
                              CountDownLatch storeEntered, CountDownLatch releaseStore) throws Exception {
        doAnswer(invocation -> {
            workerThread.set(Thread.currentThread());
            storeEntered.countDown();
            releaseStore.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return null;
        }).when(scalarDataDao).storeScalarData(any());
    }

    /**
     * Lets the first store fail and counts {@code storedAfterFailure} down when the second one succeeds.
     */
    private void failFirstStore(CountDownLatch storedAfterFailure) {
        final AtomicInteger storeCount = new AtomicInteger();
        doAnswer(invocation -> {
            if(storeCount.incrementAndGet() == 1) {
                throw new RuntimeException("simulated storage failure");
            }
            storedAfterFailure.countDown();
            return null;
        }).when(scalarDataDao).storeScalarData(any());
    }

    private static ConfigData config() {
        return config(10);
    }

    /** One worker thread per data type, the trace and environment ones being no-ops. */
    private static ConfigData config(int queueSize) {
        return new ConfigData(queueSize, queueSize, queueSize, 1, 1, 1);
    }
}
