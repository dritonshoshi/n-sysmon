package com.nsysmon.datasink.cyclicdump;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ASlf4JDebugCyclicMeasurementDumperTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;
    private boolean originalAdditive;

    @BeforeEach void attachRecorder() {
        logger = (Logger) LoggerFactory.getLogger(ASlf4JDebugCyclicMeasurementDumper.class);
        originalLevel = logger.getLevel();
        originalAdditive = logger.isAdditive();

        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
    }

    @AfterEach void detachRecorder() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
        logger.setAdditive(originalAdditive);
    }

    @Test void dumpLogsTheGivenLineAtDebugLevel() {
        final ASlf4JDebugCyclicMeasurementDumper dumper = new ASlf4JDebugCyclicMeasurementDumper(3600, 3600, 0);

        dumper.dump("Scalar Measurement: load = 0.5");

        assertEquals(1, appender.list.size());
        assertEquals(Level.DEBUG, appender.list.get(0).getLevel());
        assertEquals("Scalar Measurement: load = 0.5", appender.list.get(0).getFormattedMessage());
    }

    @Test void dumpLogsEveryLineSeparately() {
        final ASlf4JDebugCyclicMeasurementDumper dumper = new ASlf4JDebugCyclicMeasurementDumper(3600, 3600, 0);

        dumper.dump("first");
        dumper.dump("second");

        assertEquals(2, appender.list.size());
        assertEquals("first", appender.list.get(0).getFormattedMessage());
        assertEquals("second", appender.list.get(1).getFormattedMessage());
    }
}