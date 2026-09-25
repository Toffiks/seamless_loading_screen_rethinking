package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotArchiveTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsOnlyTheConfiguredNumberOfManagedArchives() throws Exception {
        Path screenshot = temporaryDirectory.resolve("screenshots/worlds/servers/example.png");
        Files.createDirectories(screenshot.getParent());
        Files.writeString(screenshot, "image");

        Path archiveDirectory = temporaryDirectory.resolve("screenshots/worlds/archive");
        Files.createDirectories(archiveDirectory);
        Path foreignPng = archiveDirectory.resolve("keep-me.png");
        Files.writeString(foreignPng, "foreign");

        ScreenshotArchive.archiveScreenshot(screenshot, 2);
        ScreenshotArchive.archiveScreenshot(screenshot, 2);
        ScreenshotArchive.archiveScreenshot(screenshot, 2);

        try (var files = Files.list(archiveDirectory)) {
            assertEquals(2, files.filter(path -> path.getFileName().toString().startsWith("sls_")).count());
        }
        assertTrue(Files.exists(foreignPng));
    }
}
