package com.minenash.seamless_loading_screen;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Creates and prunes screenshots owned by Seamless Loading Screen.
 *
 * <p>Managed archives have a distinct {@code sls_} prefix and a strict name
 * format. Pruning is deliberately non-recursive and ignores every other file,
 * directory and symbolic link in the archive directory.</p>
 */
public final class ScreenshotArchive {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MANAGED_PREFIX = "sls_";
    private static final int MAX_BASE_NAME_LENGTH = 160;
    private static final DateTimeFormatter ARCHIVE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");
    private static final Pattern MANAGED_FILE_PATTERN = Pattern.compile(
            "^" + MANAGED_PREFIX + ".+_(\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3})_[0-9a-f]{8}\\.png$",
            Pattern.DOTALL);

    private ScreenshotArchive() {}

    /**
     * Copies a saved world screenshot to its sibling archive directory, then
     * keeps only the newest {@code maxEntries} managed archive files.
     *
     * @return the path of the newly created archive
     */
    public static Path archiveScreenshot(Path screenshot, int maxEntries) throws IOException {
        if (screenshot == null || screenshot.getFileName() == null) {
            throw new IOException("Screenshot path has no file name");
        }

        Path categoryDirectory = screenshot.getParent();
        Path worldsDirectory = categoryDirectory == null ? null : categoryDirectory.getParent();
        if (worldsDirectory == null) {
            throw new IOException("Screenshot path has no worlds directory: " + screenshot);
        }

        Path archiveDirectory = worldsDirectory.resolve("archive");
        Files.createDirectories(archiveDirectory);

        String archiveName = createArchiveName(screenshot.getFileName().toString());
        Path archive = archiveDirectory.resolve(archiveName);
        copyAtomically(screenshot, archive);

        try {
            enforceLimit(archiveDirectory, maxEntries);
        } catch (IOException e) {
            // The archive was already written successfully. A cleanup problem
            // must not make the primary screenshot save appear to have failed.
            LOGGER.warn("[SeamlessLoadingScreen] Unable to enforce the screenshot archive limit in {}",
                    archiveDirectory, e);
        }
        return archive;
    }

    static void enforceLimit(Path archiveDirectory, int maxEntries) throws IOException {
        int safeLimit = Math.max(1, maxEntries);
        List<ArchiveEntry> entries = new ArrayList<>();

        try (Stream<Path> paths = Files.list(archiveDirectory)) {
            for (Path path : paths.toList()) {
                ArchiveEntry entry = readManagedEntry(path);
                if (entry != null) entries.add(entry);
            }
        }

        if (entries.size() <= safeLimit) return;

        entries.sort(Comparator
                .comparing(ArchiveEntry::timestamp)
                .thenComparing(ArchiveEntry::lastModified)
                .thenComparing(entry -> entry.path().getFileName().toString()));

        int remaining = entries.size();
        for (ArchiveEntry entry : entries) {
            if (remaining <= safeLimit) break;
            Path path = entry.path();
            try {
                Files.deleteIfExists(path);
                remaining--;
            } catch (IOException e) {
                LOGGER.warn("[SeamlessLoadingScreen] Unable to delete old archived screenshot: {}", path, e);
            }
        }
    }

    private static ArchiveEntry readManagedEntry(Path path) {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) return null;

        Matcher matcher = MANAGED_FILE_PATTERN.matcher(path.getFileName().toString());
        if (!matcher.matches()) return null;

        try {
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), ARCHIVE_TIMESTAMP);
            FileTime lastModified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
            return new ArchiveEntry(path, timestamp, lastModified);
        } catch (DateTimeParseException | IOException e) {
            // If ownership or age cannot be established, leave the file alone.
            LOGGER.warn("[SeamlessLoadingScreen] Ignoring an unreadable archive entry: {}", path, e);
            return null;
        }
    }

    private static String createArchiveName(String screenshotName) {
        int extension = screenshotName.lastIndexOf('.');
        String baseName = extension > 0 ? screenshotName.substring(0, extension) : screenshotName;
        if (baseName.length() > MAX_BASE_NAME_LENGTH) {
            int end = MAX_BASE_NAME_LENGTH;
            if (Character.isHighSurrogate(baseName.charAt(end - 1))) end--;
            baseName = baseName.substring(0, end);
        }
        if (baseName.isBlank()) baseName = "screenshot";

        String timestamp = ARCHIVE_TIMESTAMP.format(LocalDateTime.now());
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        return MANAGED_PREFIX + baseName + "_" + timestamp + "_" + uniqueSuffix + ".png";
    }

    private static void copyAtomically(Path source, Path output) throws IOException {
        Path temporary = Files.createTempFile(output.getParent(), ".sls-archive-", ".tmp");
        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
            moveAtomically(temporary, output);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path output) throws IOException {
        try {
            Files.move(source, output, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, output);
        }
    }

    private record ArchiveEntry(Path path, LocalDateTime timestamp, FileTime lastModified) {}
}
