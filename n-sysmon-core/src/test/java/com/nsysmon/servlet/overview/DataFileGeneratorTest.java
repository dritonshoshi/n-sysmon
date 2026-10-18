package com.nsysmon.servlet.overview;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
import com.nsysmon.impl.NSysMonImpl;
import com.nsysmon.testutil.ExportingPageDefinition;
import com.nsysmon.testutil.RecordingJson;
import com.nsysmon.testutil.StubPageDefinition;
import com.nsysmon.testutil.TestSysMons;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataFileGeneratorTest {
    @TempDir Path tempDir;

    private MockedStatic<NSysMon> nsysMonSingleton;
    private RecordingJson json;

    @BeforeEach void setUp() {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.setPathDatafiles(tempDir.toString());
        configBuilder.addPresentationMenuEntry("aMenu",
                List.of(new ExportingPageDefinition("exportMe"), new StubPageDefinition("plain")));
        final NSysMonConfig config = configBuilder.build();

        final NSysMonApi sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(config);
        nsysMonSingleton = Mockito.mockStatic(NSysMon.class);
        nsysMonSingleton.when(NSysMon::get).thenReturn(sysMon);

        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        nsysMonSingleton.close();
    }

    /**
     * The generator's threads only know their page after {@link DataFileGeneratorThread#run()}, which in production
     *  happens on the first scheduled run or on shutdown of N-SysMon.
     */
    private DataFileGenerator startedGenerator(String pagesToStore) throws Exception {
        final DataFileGenerator result = new DataFileGenerator(pagesToStore);
        final NSysMonImpl sysMon = (NSysMonImpl) TestSysMons.create();
        result.init(sysMon);
        sysMon.shutdown();
        return result;
    }

    private String servedDataOf(DataFileGenerator page) throws Exception {
        assertTrue(page.handleRestCall("getData", List.of(), json.helper()));
        return json.compactJson();
    }

    private List<Path> filesInTempDir() throws IOException {
        try (var files = Files.list(tempDir)) {
            return files.toList();
        }
    }

    @Test void exposesItsLabelsAndFileNames() {
        final DataFileGenerator page = new DataFileGenerator("");

        assertEquals("dataFileGenerator", page.getId());
        assertEquals("File Generator", page.getShortLabel());
        assertEquals("Generate Data Files on Server", page.getFullLabel());
        assertEquals("datafilegenerator.html", page.getHtmlFileName());
        assertEquals("CtrlDataFileGenerator", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        final DataFileGenerator page = new DataFileGenerator("");

        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesAnEmptyListWithoutConfiguredPages() throws Exception {
        assertEquals("{\"pages\":[]}", servedDataOf(new DataFileGenerator("")));
    }

    @Test void servesTheConfiguredPages() throws Exception {
        final String result = servedDataOf(startedGenerator("exportMe:30"));

        assertTrue(result.contains("\"id\":\"exportMe\",\"fullLabel\":\"exportMe Full\",\"timeToWait\":30,"), result);
        assertTrue(result.contains("\"lastExportTimestamp\":\"2"), result);
    }

    @Test void ignoresEntriesWithoutAPageId() throws Exception {
        final String result = servedDataOf(startedGenerator(" , :30 , exportMe:15"));

        assertEquals(1, result.split("\"id\":", -1).length - 1, result);
        assertTrue(result.contains("\"timeToWait\":15"), result);
    }

    @Test void ignoresEntriesWithoutAValidInterval() throws Exception {
        assertEquals("{\"pages\":[]}", servedDataOf(new DataFileGenerator("exportMe:notANumber")));
    }

    @Test void exportsTheDataOfEveryConfiguredPageOnShutdown() throws Exception {
        startedGenerator("exportMe:30");

        final List<Path> files = filesInTempDir();

        assertEquals(1, files.size(), files.toString());
        assertTrue(files.get(0).getFileName().toString().startsWith(DataFileGeneratorSupporter.DATAFILE_PREFIX));
        assertTrue(files.get(0).getFileName().toString().contains("exportMe"));
        assertTrue(files.get(0).getFileName().toString().endsWith(".gz"));
    }

}
