package com.nsysmon.servlet.overview;

import com.nsysmon.NSysMon;
import com.nsysmon.NSysMonApi;
import com.nsysmon.config.NSysMonConfig;
import com.nsysmon.config.NSysMonConfigBuilder;
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
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoadableServerDataFilesTest {
    private static final String FILE_NAME = "nsysmon_installation_host_exportMe_D20200102.T030405.gz";

    @TempDir Path tempDir;

    private MockedStatic<NSysMon> nsysMonSingleton;
    private NSysMonApi sysMon;
    private LoadableServerDataFiles page;
    private RecordingJson json;

    @BeforeEach void setUp() {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.setPathDatafiles(tempDir.toString());
        configBuilder.addPresentationMenuEntry("aMenu",
                List.of(new ExportingPageDefinition("exportMe"), new StubPageDefinition("unknownPage")));
        final NSysMonConfig config = configBuilder.build();

        sysMon = mock(NSysMonApi.class);
        when(sysMon.getConfig()).thenReturn(config);
        nsysMonSingleton = Mockito.mockStatic(NSysMon.class);
        nsysMonSingleton.when(NSysMon::get).thenReturn(sysMon);

        page = new LoadableServerDataFiles();
        page.init(sysMon);
        json = new RecordingJson();
    }

    @AfterEach void tearDown() {
        nsysMonSingleton.close();
    }

    private static void writeGzipFile(Path dir, String fileName, String content) throws IOException {
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(dir.resolve(fileName)))) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String servedFiles() throws Exception {
        assertTrue(page.handleRestCall("getFiles", List.of(), json.helper()));
        return json.compactJson();
    }

    @Test void exposesItsLabelsAndFileNames() {
        assertEquals("loadableServerDataFiles", page.getId());
        assertEquals("Datafiles", page.getShortLabel());
        assertEquals("List Data Files on Server", page.getFullLabel());
        assertEquals("loadableserverdatafiles.html", page.getHtmlFileName());
        assertEquals("CtrlLoadableServerDataFiles", page.getControllerName());
    }

    @Test void returnsFalseForUnknownService() throws Exception {
        assertFalse(page.handleRestCall("thereIsNoSuchService", List.of(), json.helper()));
    }

    @Test void servesAnEmptyListForAnEmptyDirectory() throws Exception {
        assertEquals("{\"files\":[]}", servedFiles());
    }

    @Test void listsTheDataFilesWithTheirMetadata() throws Exception {
        writeGzipFile(tempDir, FILE_NAME, "{}");

        final String result = servedFiles();

        assertTrue(result.contains("\"name\":\"" + FILE_NAME + "\""), result);
        assertTrue(result.contains("\"page\":\"exportMe\""), result);
        assertTrue(result.contains("\"date\":\"2020-01-02\""), result);
        assertTrue(result.contains("\"time\":\"03:04:05\""), result);
        assertTrue(result.contains("\"installation\":\"installation\""), result);
        assertTrue(result.contains("\"host\":\"host\""), result);
        assertTrue(result.contains("\"controllerId\":\"exportMe\""), result);
        assertTrue(result.contains("\"controllerName\":\"exportMe Full\""), result);
    }

    @Test void reportsTheCompressedFileSize() throws Exception {
        writeGzipFile(tempDir, FILE_NAME, "{}");

        servedFiles();

        assertTrue(json.compactJson().contains("\"size\":" + Files.size(tempDir.resolve(FILE_NAME))), json.compactJson());
    }

    @Test void ignoresFilesThatAreNoGzippedDataFiles() throws Exception {
        writeGzipFile(tempDir, "somethingelse.gz", "{}");
        Files.writeString(tempDir.resolve("nsysmon_installation_host_exportMe_D20200102.T030405"), "{}");

        assertEquals("{\"files\":[]}", servedFiles());
    }

    @Test void hasNoControllerNameForUnregisteredPages() throws Exception {
        writeGzipFile(tempDir, "nsysmon_installation_host_notRegistered_D20200102.T030405.gz", "{}");

        assertTrue(servedFiles().contains("\"controllerName\":null"), json.compactJson());
    }

    @Test void reportsAnUnreadableDataFileDirectory() throws Exception {
        final NSysMonConfigBuilder configBuilder = TestSysMons.configBuilder();
        configBuilder.setPathDatafiles(tempDir.resolve("does-not-exist").toString());
        when(sysMon.getConfig()).thenReturn(configBuilder.build());

        assertEquals("{\"files\":[]}", servedFiles());
    }

    @Test void loadsTheContentsOfADataFile() throws Exception {
        writeGzipFile(tempDir, FILE_NAME, "{\"loaded\":true}");

        assertTrue(page.handleRestCall("loadFromFile", List.of(FILE_NAME), json.helper()));

        assertEquals("{\"loaded\":true}", json.json());
    }

    @Test void replacesPercentEncodedSpacesInTheFileName() throws Exception {
        writeGzipFile(tempDir, "nsysmon_installation_host_export Me_D20200102.T030405.gz", "{\"loaded\":true}");

        assertTrue(page.handleRestCall("loadFromFile",
                List.of("nsysmon_installation_host_export%20Me_D20200102.T030405.gz"), json.helper()));

        assertEquals("{\"loaded\":true}", json.json());
    }

    @Test void refusesToLoadFilesWithADirectoryInTheirName() throws Exception {
        assertTrue(page.handleRestCall("loadFromFile", List.of("../etc/passwd"), json.helper()));

        assertEquals("", json.json());
    }

    @Test void refusesToLoadFilesWithABackslashInTheirName() throws Exception {
        assertTrue(page.handleRestCall("loadFromFile", List.of("..\\passwd"), json.helper()));

        assertEquals("", json.json());
    }

    @Test void failsForAFileThatDoesNotExist() {
        assertThrows(IOException.class,
                () -> page.handleRestCall("loadFromFile", List.of("there-is-no-such-file.gz"), json.helper()));
    }

}
