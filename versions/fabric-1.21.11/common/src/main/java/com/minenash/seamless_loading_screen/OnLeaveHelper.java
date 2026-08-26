package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Coordinates capturing the final world frame before disconnecting.
 */
public class OnLeaveHelper {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CAPTURE_TIMEOUT_SECONDS = 5;
    private static final int SAVE_TIMEOUT_SECONDS = 30;
    private static final DateTimeFormatter ARCHIVE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS");
    private static final AtomicReference<CaptureSession> PENDING_CAPTURE = new AtomicReference<>();
    private static final Set<CompletableFuture<Void>> IN_FLIGHT_SAVES = ConcurrentHashMap.newKeySet();

    private record CaptureRequest(
            Path output,
            Path worldIcon,
            boolean archiveScreenshots
    ) {}

    /** All mutable data for one capture, kept separate from later requests. */
    private static final class CaptureSession {
        private final AtomicReference<Runnable> continuation;
        private final AtomicBoolean waitForSave;
        private final CaptureRequest request;
        private final AtomicBoolean captureStarted = new AtomicBoolean();
        private final AtomicBoolean captureResolved = new AtomicBoolean();
        private final AtomicBoolean captureComplete = new AtomicBoolean();
        private final AtomicBoolean saveComplete = new AtomicBoolean();
        private final AtomicBoolean forceContinuation = new AtomicBoolean();
        private final AtomicBoolean saveTimeoutScheduled = new AtomicBoolean();
        private final AtomicBoolean stateRestored = new AtomicBoolean();
        private final AtomicBoolean continuationScheduled = new AtomicBoolean();
        private Perspective previousPerspective;
        private int previousFramebufferWidth;
        private int previousFramebufferHeight;
        private boolean captureResolutionApplied;

        private CaptureSession(Runnable continuation, boolean waitForSave, CaptureRequest request) {
            this.continuation = new AtomicReference<>(continuation);
            this.waitForSave = new AtomicBoolean(waitForSave);
            this.request = request;
        }

        private boolean canContinue() {
            return forceContinuation.get()
                    || captureComplete.get() && (!waitForSave.get() || saveComplete.get());
        }
    }

    public static boolean shouldTakeScreenshot() {
        CaptureSession session = PENDING_CAPTURE.get();
        return session != null && !session.captureStarted.get();
    }

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
        if (ScreenshotLoader.getDisplayMode() == DisplayMode.FREEZE) {
            runnable.run();
            return;
        }

        CaptureRequest request = createCaptureRequest();
        if (request == null) {
            runnable.run();
            return;
        }

        CaptureSession session = new CaptureSession(runnable, waitForSave, request);
        while (true) {
            CaptureSession pending = PENDING_CAPTURE.get();
            if (pending != null) {
                if (waitForSave) {
                    pending.continuation.set(runnable);
                    pending.waitForSave.set(true);
                    scheduleSaveTimeout(pending);
                    releaseContinuation(pending);
                }
                LOGGER.debug("[SeamlessLoadingScreen] Reused the pending screenshot capture for a repeated exit action");
                return;
            }
            if (PENDING_CAPTURE.compareAndSet(null, session)) break;
        }

        var client = MinecraftClient.getInstance();
        session.previousPerspective = client.options.getPerspective();
        try {
            applyCaptureResolution(client, SeamlessLoadingScreenConfig.get().resolution, session);
        } catch (RuntimeException | OutOfMemoryError e) {
            LOGGER.warn("[SeamlessLoadingScreen] Unable to use the configured capture resolution; falling back to Native", e);
            restoreCaptureResolution(client, session);
        }

        try {
            client.options.setPerspective(Perspective.FIRST_PERSON);
        } catch (RuntimeException e) {
            LOGGER.warn("[SeamlessLoadingScreen] Unable to switch to first-person view for the screenshot", e);
            session.previousPerspective = null;
        }

        // A minimized window or a renderer cancelled by another mod may never
        // produce the requested frame. Never leave disconnect or shutdown stuck.
        CompletableFuture.delayedExecutor(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> {
                    if (!session.captureResolved.compareAndSet(false, true)) return;
                    session.forceContinuation.set(true);
                    releaseContinuation(session);
                });
    }

    public static boolean awaitPendingSaves(Runnable continuation) {
        CaptureSession pending = PENDING_CAPTURE.get();
        if (pending != null) {
            pending.continuation.set(continuation);
            pending.waitForSave.set(true);
            scheduleSaveTimeout(pending);
            releaseContinuation(pending);
            return true;
        }

        CompletableFuture<?>[] saves = IN_FLIGHT_SAVES.stream()
                .filter(future -> !future.isDone())
                .toArray(CompletableFuture[]::new);
        if (saves.length == 0) return false;

        AtomicBoolean resumed = new AtomicBoolean();
        Runnable resumeOnce = () -> {
            if (!resumed.compareAndSet(false, true)) return;
            CompletableFuture.runAsync(() -> MinecraftClient.getInstance().execute(continuation));
        };
        CompletableFuture.allOf(saves).whenComplete((unused, error) -> resumeOnce.run());
        CompletableFuture.delayedExecutor(SAVE_TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(resumeOnce);
        return true;
    }

    /**
     * Called after world rendering and before GUI rendering.
     */
    public static void takeScreenShot() {
        CaptureSession session = PENDING_CAPTURE.get();
        if (session == null || !session.captureStarted.compareAndSet(false, true)) return;
        var client = MinecraftClient.getInstance();

        try {
            ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), image -> {
                session.captureResolved.compareAndSet(false, true);
                restoreClientState(client, session);
                session.captureComplete.set(true);
                scheduleSaveTimeout(session);

                try {
                    CompletableFuture<Void> save = CompletableFuture.runAsync(
                            () -> saveScreenshot(image, session.request), Util.getIoWorkerExecutor());
                    IN_FLIGHT_SAVES.add(save);
                    save.whenComplete((unused, error) -> {
                        IN_FLIGHT_SAVES.remove(save);
                        session.saveComplete.set(true);
                        releaseContinuation(session);
                    });
                } catch (RuntimeException e) {
                    image.close();
                    session.saveComplete.set(true);
                    LOGGER.error("[SeamlessLoadingScreen] Unable to schedule screenshot saving", e);
                    releaseContinuation(session);
                    return;
                }

                releaseContinuation(session);
            });
        } catch (RuntimeException | OutOfMemoryError e) {
            session.captureResolved.compareAndSet(false, true);
            restoreClientState(client, session);
            session.forceContinuation.set(true);
            LOGGER.error("[SeamlessLoadingScreen] Unable to start screenshot capture", e);
            releaseContinuation(session);
        }
    }

    private static CaptureRequest createCaptureRequest() {
        String relativeFileName = ScreenshotLoader.getFileName();
        if (relativeFileName == null || relativeFileName.isBlank()) {
            LOGGER.warn("[SeamlessLoadingScreen] No screenshot target is known for the current world");
            return null;
        }

        Path gameDirectory = PlatformFunctions.getGameDir().toAbsolutePath().normalize();
        Path output = gameDirectory.resolve(relativeFileName).normalize();
        if (!output.startsWith(gameDirectory)) {
            LOGGER.error("[SeamlessLoadingScreen] Refusing to write a screenshot outside the game directory: {}", output);
            return null;
        }

        var client = MinecraftClient.getInstance();
        Path worldIcon = null;
        if (SeamlessLoadingScreenConfig.get().updateWorldIcon && client.isInSingleplayer()
                && client.getServer() != null) {
            worldIcon = client.getServer().getIconFile().orElse(null);
        }

        return new CaptureRequest(output, worldIcon,
                SeamlessLoadingScreenConfig.get().archiveScreenshots);
    }

    private static void saveScreenshot(NativeImage capturedImage, CaptureRequest request) {
        try {
            Path output = request.output();
            writeAtomically(capturedImage, output);

            if (request.archiveScreenshots()) {
                String fileName = output.getFileName().toString();
                int extension = fileName.lastIndexOf('.');
                String baseName = extension > 0 ? fileName.substring(0, extension) : fileName;
                String timestamp = ARCHIVE_TIMESTAMP.format(LocalDateTime.now());
                Path archive = output.getParent().getParent()
                        .resolve("archive")
                        .resolve(baseName + "_" + timestamp + ".png");
                copyAtomically(output, archive);
            }

            if (request.worldIcon() != null) updateIcon(request.worldIcon(), capturedImage);
        } catch (Exception | OutOfMemoryError e) {
            LOGGER.error("[SeamlessLoadingScreen] Unable to save the world screenshot: {}",
                    request.output(), e);
        } finally {
            capturedImage.close();
        }
    }

    private static void applyCaptureResolution(
            MinecraftClient client,
            SeamlessLoadingScreenConfig.ScreenshotResolution resolution,
            CaptureSession session
    ) {
        session.captureResolutionApplied = false;
        if (resolution == SeamlessLoadingScreenConfig.ScreenshotResolution.Native) return;

        var window = client.getWindow();
        session.previousFramebufferWidth = window.getFramebufferWidth();
        session.previousFramebufferHeight = window.getFramebufferHeight();
        if (session.previousFramebufferWidth == resolution.width
                && session.previousFramebufferHeight == resolution.height) return;

        session.captureResolutionApplied = true;
        window.setFramebufferWidth(resolution.width);
        window.setFramebufferHeight(resolution.height);
        client.onResolutionChanged();
    }

    private static void restoreCaptureResolution(MinecraftClient client, CaptureSession session) {
        if (session.captureResolutionApplied) {
            try {
                var window = client.getWindow();
                window.setFramebufferWidth(session.previousFramebufferWidth);
                window.setFramebufferHeight(session.previousFramebufferHeight);
                client.onResolutionChanged();
            } catch (RuntimeException | OutOfMemoryError e) {
                LOGGER.error("[SeamlessLoadingScreen] Unable to restore the framebuffer size", e);
            } finally {
                session.captureResolutionApplied = false;
            }
        }
    }

    private static void restoreClientState(MinecraftClient client, CaptureSession session) {
        if (!session.stateRestored.compareAndSet(false, true)) return;
        restoreCaptureResolution(client, session);

        if (session.previousPerspective != null) {
            try {
                client.options.setPerspective(session.previousPerspective);
            } catch (RuntimeException e) {
                LOGGER.error("[SeamlessLoadingScreen] Unable to restore the camera perspective", e);
            } finally {
                session.previousPerspective = null;
            }
        }
    }

    private static void writeAtomically(NativeImage image, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), ".sls-", ".tmp.png");

        try {
            image.writeTo(temporary);
            moveAtomically(temporary, output);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void copyAtomically(Path source, Path output) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(output.getParent(), ".sls-", ".tmp.png");

        try {
            Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
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

    private static void releaseContinuation(CaptureSession session) {
        if (!session.canContinue()) return;
        if (!session.continuationScheduled.compareAndSet(false, true)) return;

        // ScreenshotRecorder invokes its callback while RenderSystem is draining
        // the GPU-fence queue. Disconnecting synchronously from that callback can
        // re-enter the same queue and remove its current element twice.
        CompletableFuture.runAsync(() -> {
            var client = MinecraftClient.getInstance();
            client.execute(() -> {
                if (!session.canContinue()) {
                    session.continuationScheduled.set(false);
                    if (session.canContinue()) releaseContinuation(session);
                    return;
                }
                if (!PENDING_CAPTURE.compareAndSet(session, null)) return;
                restoreClientState(client, session);

                try {
                    session.continuation.get().run();
                } catch (RuntimeException e) {
                    LOGGER.error("[SeamlessLoadingScreen] Unable to continue after screenshot capture", e);
                }
            });
        });
    }

    private static void scheduleSaveTimeout(CaptureSession session) {
        if (!session.captureComplete.get() || !session.waitForSave.get()
                || !session.saveTimeoutScheduled.compareAndSet(false, true)) return;

        CompletableFuture.delayedExecutor(SAVE_TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (session.saveComplete.get()) return;
            session.forceContinuation.set(true);
            releaseContinuation(session);
        });
    }
}
