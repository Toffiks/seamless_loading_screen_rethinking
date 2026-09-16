package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Resolves screenshot paths and owns all image-file writing. */
final class ScreenshotWriter {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ScreenshotWriter() {}

    record Request(
            Path output,
            Path worldIcon,
            boolean archiveScreenshots,
            int archiveLimit,
            SeamlessLoadingScreenConfig.ScreenshotResolution resolution
    ) {}

    static Request createRequest() {
        String relativeFileName = ScreenshotLoader.getFileName();
        if (relativeFileName == null || relativeFileName.isBlank()) {
            LOGGER.warn("[SeamlessLoadingScreen] No screenshot target is known for the current world");
            return null;
        }

        Path gameDirectory = FMLPaths.GAMEDIR.get().toAbsolutePath().normalize();
        Path output = gameDirectory.resolve(relativeFileName).normalize();
        if (!output.startsWith(gameDirectory)) {
            LOGGER.error("[SeamlessLoadingScreen] Refusing to write a screenshot outside the game directory: {}", output);
            return null;
        }

        Minecraft client = Minecraft.getInstance();
        SeamlessLoadingScreenConfig config = SeamlessLoadingScreenConfig.get();
        Path worldIcon = null;
        if (config.updateWorldIcon && client.isLocalServer() && client.getSingleplayerServer() != null) {
            worldIcon = client.getSingleplayerServer().getWorldScreenshotFile().orElse(null);
        }

        return new Request(output, worldIcon,
                config.archiveScreenshots, config.archiveLimit, config.resolution);
    }

    static void save(NativeImage capturedImage, Request request) {
        NativeImage outputImage = capturedImage;
        try {
            outputImage = resize(capturedImage, request.resolution());
            writeAtomically(outputImage, request.output());

            if (request.archiveScreenshots()) {
                try {
                    ScreenshotArchive.archiveScreenshot(request.output(), request.archiveLimit());
                } catch (Exception archiveError) {
                    LOGGER.warn("[SeamlessLoadingScreen] Screenshot was saved, but archiving failed: {}",
                            request.output(), archiveError);
                }
            }
            if (request.worldIcon() != null) updateIcon(request.worldIcon(), outputImage);
        } catch (Exception | OutOfMemoryError error) {
            LOGGER.error("[SeamlessLoadingScreen] Unable to save the world screenshot: {}",
                    request.output(), error);
        } finally {
            if (outputImage != capturedImage) outputImage.close();
            capturedImage.close();
        }
    }

    private static NativeImage resize(
            NativeImage source,
            SeamlessLoadingScreenConfig.ScreenshotResolution resolution
    ) {
        var size = resolution.resolve(source.getWidth(), source.getHeight());
        if (size.width() == source.getWidth() && size.height() == source.getHeight()) return source;

        NativeImage resized = new NativeImage(size.width(), size.height(), false);
        try {
            source.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), resized);
            return resized;
        } catch (RuntimeException | OutOfMemoryError error) {
            resized.close();
            throw error;
        }
    }

    private static void writeAtomically(NativeImage image, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), ".sls-", ".tmp.png");
        try {
            image.writeToFile(temporary);
            moveAtomically(temporary, output);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void moveAtomically(Path source, Path output) throws IOException {
        try {
            Files.move(source, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, output, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void updateIcon(Path iconFile, NativeImage image) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        int x = 0;
        int y = 0;

        if (width > height) {
            x = (width - height) / 2;
            width = height;
        } else {
            y = (height - width) / 2;
            height = width;
        }

        try (NativeImage icon = new NativeImage(64, 64, false)) {
            image.resizeSubRectTo(x, y, width, height, icon);
            writeAtomically(icon, iconFile);
        }
    }
}
