package com.nsysmon.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebappResourceBaseTest {

    @TempDir Path tempDir;

    @Test void resolvesTheWebappDirectoryBelowTheGivenWorkingDirectory() throws IOException {
        final File expected = tempDir.resolve(WebappResourceBase.WEBAPP_RESOURCE_PATH).toFile();
        assertTrue(expected.mkdirs());

        assertEquals(expected.getAbsolutePath(), WebappResourceBase.resolve(tempDir.toFile()).getAbsolutePath());
    }

    @Test void failsWithAHintWhenTheWebappModuleIsMissing() {
        final IllegalStateException exc = assertThrows(IllegalStateException.class,
                () -> WebappResourceBase.resolve(tempDir.toFile()));

        assertTrue(exc.getMessage().contains(WebappResourceBase.WEBAPP_RESOURCE_PATH), exc.getMessage());
        assertTrue(exc.getMessage().contains("repository root"), exc.getMessage());
    }

    @Test void rejectsAFileWhereTheDirectoryShouldBe() throws IOException {
        final Path path = tempDir.resolve(WebappResourceBase.WEBAPP_RESOURCE_PATH);
        Files.createDirectories(path.getParent());
        Files.createFile(path);

        assertThrows(IllegalStateException.class, () -> WebappResourceBase.resolve(tempDir.toFile()));
    }
}
