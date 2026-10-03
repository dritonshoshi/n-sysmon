package com.nsysmon.datasink.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nsysmon.data.AHierarchicalDataRoot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ASlf4JDataSinkTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;
    private boolean originalAdditive;

    @BeforeEach void attachRecorder() {
        logger = (Logger) LoggerFactory.getLogger(ASlf4JDataSink.class);
        originalLevel = logger.getLevel();
        originalAdditive = logger.isAdditive();

        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
    }

    @AfterEach void detachRecorder() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
        logger.setAdditive(originalAdditive);
    }

    private List<String> messages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).collect(Collectors.toList());
    }

    private List<Level> levels() {
        return appender.list.stream().map(ILoggingEvent::getLevel).collect(Collectors.toList());
    }

    @Test void logsMeasurementHierarchyAtDebugLevel() {
        logger.setLevel(Level.DEBUG);
        final AHierarchicalDataRoot data = ALoggingDataSinkTest.traceOf(ALoggingDataSinkTest.node(true, 1000, 500, "root", ALoggingDataSinkTest.node(true, 1100, 200, "child")));

        new ASlf4JDataSink().onFinishedHierarchicalMeasurement(data);

        assertEquals(List.of("+root@1000: 500 {}", "  +child@1100: 200 {}"), messages());
        assertEquals(List.of(Level.DEBUG, Level.DEBUG), levels());
    }

    @Test void logsNothingWhenDebugIsDisabled() {
        logger.setLevel(Level.INFO);

        new ASlf4JDataSink().onFinishedHierarchicalMeasurement(ALoggingDataSinkTest.traceOf(ALoggingDataSinkTest.node(true, 1000, 500, "root")));

        assertTrue(appender.list.isEmpty());
    }

    @Test void logsNothingForStartedMeasurements() {
        logger.setLevel(Level.DEBUG);

        new ASlf4JDataSink().onStartedHierarchicalMeasurement("some-identifier");

        assertTrue(appender.list.isEmpty());
    }
}
