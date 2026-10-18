package com.nsysmon.servlet.overview;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.testutil.ExportingPageDefinition;
import com.nsysmon.testutil.StubPageDefinition;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFileGeneratorThreadTest {
    @TempDir Path tempDir;

    private MockedStatic<NSysMon> nsysMonSingleton;
    private NSysMonApi sysMon;

    @BeforeEach void setUp() {
        ExportingPageDefinition.resetNumExports();
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.setPathDatafiles(tempDir.toString());
        configBuilder.addPresentationMenuEntry("aMenu",
                List.of(new ExportingPageDefinition("exportMe"), new StubPageDefinition("plain")));
        final NSysMonConfig config = configBuilder.build();

        sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(config);
        nsysMonSingleton = Mockito.mockStatic(NSysMon.class);
        nsysMonSingleton.when(NSysMon::get).thenReturn(sysMon);
    }

    @AfterEach void tearDown() {
        nsysMonSingleton.close();
    }

    @Test void exposesWhatItWasConfiguredWith() {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("somePage", 42);

        assertEquals("somePage", thread.getPageId());
        assertEquals(42, thread.getTimeToWait());
    }

    @Test void hasNoPageAndNoTimestampBeforeItsFirstRun() {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("somePage", 42);

        assertNull(thread.getPage());
        assertThrows(NullPointerException.class, thread::getLastExportTimestamp);
    }

    @Test void toStringMentionsThePageTheIntervalAndTheLastExport() {
        final String result = new DataFileGeneratorThread("somePage", 42).toString();

        assertTrue(result.startsWith("DataFileGeneratorThread["), result);
        assertTrue(result.contains("page=somePage"), result);
        assertTrue(result.contains("timeToWait=42"), result);
        assertTrue(result.contains("lastExport=null"), result);
    }

    @Test void runResolvesThePageDefinitionByItsId() {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("exportMe", 30);

        thread.run();

        assertNotNull(thread.getPage());
        assertEquals("exportMe", thread.getPage().getId());
    }

    @Test void runMatchesThePageIdCaseInsensitively() {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("EXPORTME", 30);

        thread.run();

        assertEquals("exportMe", thread.getPage().getId());
    }

    @Test void runRecordsAParsableIsoLocalTimestamp() {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("exportMe", 30);

        thread.run();

        assertDoesNotThrow(() -> LocalDateTime.parse(thread.getLastExportTimestamp(),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Test void runExportsTheDataOfAnExportablePage() throws IOException {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("exportMe", 30);

        thread.run();

        try (var files = Files.list(tempDir)) {
            final Path exported = files.findFirst().orElseThrow();
            assertEquals(ExportingPageDefinition.PAYLOAD, gunzip(exported));
        }
    }

    @Test void runDoesNotExportPagesThatCannotDoSo() throws IOException {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("plain", 30);

        thread.run();

        assertEquals("plain", thread.getPage().getId());
        try (var files = Files.list(tempDir)) {
            assertTrue(files.findAny().isEmpty());
        }
    }

    @Test void runReportsAnUnwritableTargetDirectory() {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.setPathDatafiles(tempDir.resolve("does-not-exist").toString());
        configBuilder.addPresentationMenuEntry("aMenu", List.of(new ExportingPageDefinition("exportMe")));
        when(sysMon.getConfig()).thenReturn(configBuilder.build());
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("exportMe", 30);

        assertDoesNotThrow(thread::run);

        assertEquals("exportMe", thread.getPage().getId());
        assertEquals(0, ExportingPageDefinition.numExports());
    }

    @Test void runFailsForAConfiguredPageIdThatDoesNotExist() {
        final DataFileGeneratorThread thread = new DataFileGeneratorThread("thereIsNoSuchPage", 30);

        assertThrows(NullPointerException.class, thread::run);
    }

    private static String gunzip(Path path) throws IOException {
        try (GZIPInputStream in = new GZIPInputStream(Files.newInputStream(path))) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
