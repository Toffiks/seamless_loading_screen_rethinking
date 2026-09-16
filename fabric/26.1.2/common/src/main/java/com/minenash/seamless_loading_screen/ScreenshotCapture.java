package com.minenash.seamless_loading_screen;

import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.slf4j.Logger;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/** Coordinates the last-frame capture without blocking FastQuit. */
public final class ScreenshotCapture {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CAPTURE_TIMEOUT_SECONDS = 5;
    private static final int SAVE_TIMEOUT_SECONDS = 30;

    private static final AtomicReference<CaptureJob> PENDING_CAPTURE = new AtomicReference<>();
    private static final ThreadPoolExecutor SAVE_EXECUTOR = createSaveExecutor();
    private static volatile CompletableFuture<Void> lastSave = CompletableFuture.completedFuture(null);

    private ScreenshotCapture() {}

    private static final class CaptureJob {
        private final ScreenshotWriter.Request request;
        private final CompletableFuture<Void> captured = new CompletableFuture<>();
        private final CompletableFuture<Void> saved = new CompletableFuture<>();
        private final AtomicBoolean captureStarted = new AtomicBoolean();
        private final AtomicBoolean saveTimeoutScheduled = new AtomicBoolean();

        private volatile Runnable continuation;
        private volatile boolean waitForSave;

        private CaptureJob(Runnable continuation, boolean waitForSave, ScreenshotWriter.Request request) {
            this.continuation = continuation;
            this.waitForSave = waitForSave;
            this.request = request;
        }

        private boolean canContinue() {
            return captured.isDone() && (!waitForSave || saved.isDone());
        }
    }

    public static boolean shouldCapture() {
        CaptureJob job = PENDING_CAPTURE.get();
        return job != null && !job.captureStarted.get();
    }

    /** Captures the frame, then lets a normal disconnect continue immediately. */
    public static void begin(Runnable continuation) {
        begin(continuation, false);
    }

    /**
     * Full shutdown waits for PNG encoding. A normal disconnect only waits until
     * the framebuffer has been copied, so FastQuit can save the world in parallel.
     */
    public static void begin(Runnable continuation, boolean waitForSave) {
        if (ScreenshotLoader.getDisplayMode() == DisplayMode.FREEZE) {
            continuation.run();
            return;
        }

        ScreenshotWriter.Request request = ScreenshotWriter.createRequest();
        if (request == null) {
            continuation.run();
            return;
        }

        CaptureJob job = new CaptureJob(continuation, waitForSave, request);
        job.captured.whenComplete((unused, error) -> resumeWhenReady(job));
        job.saved.whenComplete((unused, error) -> resumeWhenReady(job));

        while (!PENDING_CAPTURE.compareAndSet(null, job)) {
            CaptureJob pending = PENDING_CAPTURE.get();
            if (pending == null) continue;
            if (waitForSave) requireSavedFile(pending, continuation);
            LOGGER.debug("[SeamlessLoadingScreen] Reused the pending screenshot capture for a repeated exit action");
            return;
        }

        if (waitForSave) scheduleSaveTimeout(job);
        CompletableFuture.delayedExecutor(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS).execute(() -> {
            if (job.captured.complete(null)) job.saved.complete(null);
        });
    }

    /** Waits for a pending capture or the last queued save before shutdown. */
    public static boolean awaitPendingSave(Runnable continuation) {
        CaptureJob pending = PENDING_CAPTURE.get();
        if (pending != null) {
            requireSavedFile(pending, continuation);
            return true;
        }

        CompletableFuture<Void> save = lastSave;
        if (save.isDone()) return false;

        CompletableFuture<Void> timeout = new CompletableFuture<>();
        CompletableFuture.delayedExecutor(SAVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> timeout.complete(null));
        CompletableFuture.anyOf(save.handle((unused, error) -> null), timeout)
                .thenRun(() -> runOnClientLater(continuation));
        return true;
    }

    /** Called after world rendering and before GUI rendering. */
    public static void captureFrame() {
        CaptureJob job = PENDING_CAPTURE.get();
        if (job == null || !job.captureStarted.compareAndSet(false, true)) return;

        try {
            Minecraft client = Minecraft.getInstance();
            Screenshot.takeScreenshot(client.getMainRenderTarget(), image -> finishCapture(job, image));
        } catch (RuntimeException | OutOfMemoryError error) {
            LOGGER.error("[SeamlessLoadingScreen] Unable to start screenshot capture", error);
            job.saved.complete(null);
            job.captured.complete(null);
        }
    }

    private static void finishCapture(CaptureJob job, NativeImage image) {
        CompletableFuture<Void> save;
        try {
            save = submitSave(image, job.request);
        } catch (RejectedExecutionException rejected) {
            image.close();
            save = lastSave;
            LOGGER.warn("[SeamlessLoadingScreen] Screenshot save queue is full; dropping the newest capture");
        } catch (RuntimeException | OutOfMemoryError error) {
            image.close();
            save = lastSave;
            LOGGER.error("[SeamlessLoadingScreen] Unable to schedule screenshot saving", error);
        }

        save.whenComplete((unused, error) -> job.saved.complete(null));
        job.captured.complete(null);
    }

    private static synchronized CompletableFuture<Void> submitSave(
            NativeImage image,
            ScreenshotWriter.Request request
    ) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        SAVE_EXECUTOR.execute(() -> {
            try {
                ScreenshotWriter.save(image, request);
                completion.complete(null);
            } catch (RuntimeException | Error error) {
                completion.completeExceptionally(error);
                throw error;
            }
        });
        lastSave = completion;
        return completion;
    }

    private static void requireSavedFile(CaptureJob job, Runnable continuation) {
        job.continuation = continuation;
        job.waitForSave = true;
        scheduleSaveTimeout(job);
        resumeWhenReady(job);
    }

    private static void scheduleSaveTimeout(CaptureJob job) {
        if (!job.saveTimeoutScheduled.compareAndSet(false, true)) return;
        CompletableFuture.delayedExecutor(SAVE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .execute(() -> job.saved.complete(null));
    }

    private static void resumeWhenReady(CaptureJob job) {
        if (!job.canContinue()) return;

        // ScreenshotRecorder invokes its callback while the GPU-fence queue is
        // being drained, so disconnecting must be deferred to a later task.
        runOnClientLater(() -> {
            if (!job.canContinue() || !PENDING_CAPTURE.compareAndSet(job, null)) return;
            try {
                job.continuation.run();
            } catch (RuntimeException error) {
                LOGGER.error("[SeamlessLoadingScreen] Unable to continue after screenshot capture", error);
            }
        });
    }

    private static void runOnClientLater(Runnable action) {
        CompletableFuture.runAsync(() -> Minecraft.getInstance().execute(action));
    }

    private static ThreadPoolExecutor createSaveExecutor() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 30L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                runnable -> {
                    Thread thread = new Thread(runnable, "Seamless Loading Screen saver");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.AbortPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
