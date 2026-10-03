package com.nsysmon.servlet.overview;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers how {@link DataFileTools} reacts to file names that do not follow the
 *  <code>nsysmon_installation_host_page_Ddate.Ttime.gz</code> convention.
 */
class DataFileToolsFallbacksTest {
    private static final String UNKNOWN = "<unknown>";

    private final DataFileTools tools = new DataFileTools();

    @Test void controllerIdIsUnknownWithoutAnyUnderscore() {
        assertEquals(UNKNOWN, tools.getNsysmonControllerIdFromFilename("nsysmon.gz"));
    }

    @Test void controllerIdIsUnknownWithASingleUnderscore() {
        assertEquals(UNKNOWN, tools.getNsysmonControllerIdFromFilename("nsysmon_installation"));
    }

    @Test void controllerIdIsUnknownWithTwoUnderscores() {
        assertEquals(UNKNOWN, tools.getNsysmonControllerIdFromFilename("nsysmon_installation_host"));
    }

    @Test void controllerIdIsUnknownWithThreeUnderscores() {
        assertEquals(UNKNOWN, tools.getNsysmonControllerIdFromFilename("nsysmon_installation_host_page"));
    }

    @Test void dateParsingFailsForNamesWithTooFewSegments() {
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> tools.getDateFromFilename("nsysmon_installation"));
    }

    @Test void dateParsingFailsForTooShortTimestamps() {
        assertThrows(StringIndexOutOfBoundsException.class,
                () -> tools.getDateFromFilename("nsysmon_installation_host_page_D20.gz"));
    }

    @Test void timeIsUnknownForTooShortTimestamps() {
        assertEquals(UNKNOWN, tools.getTimeFromFilename("nsysmon_installation_host_page_D20200102.T1.gz"));
    }

    @Test void installationAndHostAreTakenFromTheSecondAndThirdSegment() {
        final String fileName = "some/dir/nsysmon_installation_host_page_D20200102.T030405.gz";

        assertEquals("installation", tools.getInstallationFromFilename(fileName));
        assertEquals("host", tools.getHostFromFilename(fileName));
    }
}
