package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordinates capturing the final world frame before disconnecting.
 */
public class OnLeaveHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Runnable NO_OP = () -> {};

    public static volatile boolean attemptScreenShot = false;

    private static final AtomicBoolean capturePending = new AtomicBoolean(false);
    private static Runnable onceFinished = NO_OP;
    private static boolean waitForSaveBeforeContinuation = false;
    private static Path pendingWorldIcon;

    private record CaptureRequest(
            Path output,
            Path worldIcon,
            SeamlessLoadingScreenConfig.ScreenshotResolution resolution,
            boolean archiveScreenshots
    ) {}

    /**
     * Starts a capture and delays the supplied action until the image has been
     * handed off to the I/O worker. The continuation is always run on the
     * Minecraft client thread.
     */
    public static void beginScreenshotTask(Runnable runnable) {
        beginScreenshotTask(runnable, false);
    }

    /**
     * Starts a capture. World disconnects continue as soon as the framebuffer
     * has been copied, allowing PNG encoding to run alongside FastQuit's world
     * save. Full game shutdowns can request that the file is written first so
     * the JVM cannot terminate while the screenshot is still being encoded.
     */
    public static void beginScreenshotTask(Runnable runnable, boolean waitForSave) {
        if (ScreenshotLoader.displayMode == DisplayMode.FREEZE) {
            runnable.run();
            return;
        }

        if (!capturePending.compareAndSet(false, true)) {
            runnable.run();
            return;
        }

        onceFinished = runnable;
        waitForSaveBeforeContinuation = waitForSave;
        attemptScreenShot = true;

        var client = Minecraft.getInstance();
        client.options.setCameraType(CameraType.FIRST_PERSON);

        pendingWorldIcon = null;
        if (SeamlessLoadingScreenConfig.get().updateWorldIcon && client.isLocalServer()) {
            var server = client.getSingleplayerServer();
            if (server != null) pendingWorldIcon = server.getWorldScreenshotFile().orElse(null);
        }
    }

    /**
     * Called after world rendering and before GUI rendering.
     */
    public static void takeScreenShot() {
        if (!attemptScreenShot) return;

        attemptScreenShot = false;
        var client = Minecraft.getInstance();

        try {
            Screenshot.takeScreenshot(client.gameRenderer.mainRenderTarget(), image -> {
                CaptureRequest request = createCaptureRequest();
                boolean waitForSave = waitForSaveBeforeContinuation;

                Util.ioPool().execute(() -> {
                    saveScreenshot(image, request);
                    if (waitForSave) releaseContinuation();
                });

                if (!waitForSave) releaseContinuation();
            });
        } catch (RuntimeException e) {
            LOGGER.error("[SeamlessLoadingScreen] Unable to start screenshot capture", e);
            releaseContinuation();
        }
    }

    private static CaptureRequest createCaptureRequest() {
        Path output = PlatformFunctions.getGameDir().resolve(ScreenshotLoader.getFileName()).normalize();
        CaptureRequest request = new CaptureRequest(
                output,
                pendingWorldIcon,
                SeamlessLoadingScreenConfig.get().resolution,
                SeamlessLoadingScreenConfig.get().archiveScreenshots
        );
        pendingWorldIcon = null;
        return request;
    }

    private static void saveScreenshot(NativeImage capturedImage, CaptureRequest request) {
        NativeImage image = capturedImage;

        try {
            image = resizeForConfiguredResolution(capturedImage, request.resolution());

            Path output = request.output();
            writeAtomically(image, output);

            if (request.archiveScreenshots()) {
                String fileName = output.getFileName().toString();
                int extension = fileName.lastIndexOf('.');
                String baseName = extension > 0 ? fileName.substring(0, extension) : fileName;
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
                Path archive = PlatformFunctions.getGameDir()
                        .resolve("screenshots/worlds/archive")
                        .resolve(baseName + "_" + timestamp + ".png");
                writeAtomically(image, archive);
            }

            if (request.worldIcon() != null) updateIcon(request.worldIcon(), image);
        } catch (Exception e) {
            LOGGER.error("[SeamlessLoadingScreen] Unable to save the world screenshot: {}",
                    request.output(), e);
        } finally {
            image.close();
        }
    }

    private static NativeImage resizeForConfiguredResolution(
            NativeImage source,
            SeamlessLoadingScreenConfig.ScreenshotResolution resolution
    ) {
        if (resolution == SeamlessLoadingScreenConfig.ScreenshotResolution.Native) return source;

        double scale = Math.min(
                resolution.width / (double) source.getWidth(),
                resolution.height / (double) source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));

        if (width == source.getWidth() && height == source.getHeight()) return source;

        NativeImage resized = new NativeImage(width, height, false);
        source.resizeSubRectTo(0, 0, source.getWidth(), source.getHeight(), resized);
        source.close();
        return resized;
    }

    private static void writeAtomically(NativeImage image, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = output.resolveSibling(output.getFileName() + ".tmp.png");

        image.writeToFile(temporary);
        try {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
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
            icon.writeToFile(iconFile);
        }
    }

    private static void releaseContinuation() {
        Minecraft.getInstance().execute(() -> {
            if (!capturePending.get()) return;

            pendingWorldIcon = null;
            Runnable continuation = onceFinished;
            onceFinished = NO_OP;
            waitForSaveBeforeContinuation = false;

            try {
                continuation.run();
            } catch (RuntimeException e) {
                LOGGER.error("[SeamlessLoadingScreen] Unable to continue after screenshot capture", e);
            } finally {
                capturePending.set(false);
            }
        });
    }
}
