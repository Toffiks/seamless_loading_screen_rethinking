package com.minenash.seamless_loading_screen;

import net.minecraft.client.gui.DrawContext;
import java.util.ArrayList;
import java.util.List;

/** Keeps the last vanilla chunk-map frame for a short fade after its screen closes. */
public final class ChunkMapFade {
    private static final long FADE_NANOS = 250_000_000L;
    private static final int MAX_RECTS = 4096;
    private static final List<Rect> RECTS = new ArrayList<>();
    private static int sourceWidth;
    private static int sourceHeight;
    private static boolean recording;
    private static boolean fading;
    private static long firstFrameNanos = -1L;

    private record Rect(int x1, int y1, int x2, int y2, int color) {}

    private ChunkMapFade() {}

    public static void beginFrame(int width, int height, boolean enabled) {
        if (fading) return;
        RECTS.clear();
        sourceWidth = width;
        sourceHeight = height;
        recording = enabled;
    }

    public static void recordRect(int x1, int y1, int x2, int y2, int color) {
        if (!recording || RECTS.size() >= MAX_RECTS) return;
        // Match the loading screen's translucent ungenerated cells.
        if (color == 0xFF000000) color = 0xAA000000;
        RECTS.add(new Rect(x1, y1, x2, y2, color));
    }

    public static void start() {
        if (fading || RECTS.isEmpty()) return;
        recording = false;
        fading = true;
        firstFrameNanos = -1L;
    }

    public static float alphaForFrame(long nowNanos) {
        if (!fading) return 0.0f;
        if (firstFrameNanos < 0L) {
            firstFrameNanos = nowNanos;
            return 1.0f;
        }
        long fadeElapsedNanos = Math.max(0L, nowNanos - firstFrameNanos);
        float progress = Math.max(0.0f, Math.min(1.0f,
                fadeElapsedNanos / (float) FADE_NANOS));
        return 1.0f - progress * progress * (3.0f - 2.0f * progress);
    }

    public static void render(DrawContext context) {
        if (!fading) return;
        float fade = alphaForFrame(System.nanoTime());
        if (fade <= 0.0f) {
            clear();
            return;
        }
        int shiftX = (context.getScaledWindowWidth() - sourceWidth) / 2;
        int shiftY = (context.getScaledWindowHeight() - sourceHeight) / 2;
        for (Rect rect : RECTS) {
            int oldAlpha = rect.color >>> 24;
            int alpha = Math.round(oldAlpha * fade);
            int color = (alpha << 24) | (rect.color & 0x00FFFFFF);
            context.fill(rect.x1 + shiftX, rect.y1 + shiftY,
                    rect.x2 + shiftX, rect.y2 + shiftY, color);
        }
    }

    public static void clear() {
        RECTS.clear();
        recording = false;
        fading = false;
        firstFrameNanos = -1L;
    }
}
