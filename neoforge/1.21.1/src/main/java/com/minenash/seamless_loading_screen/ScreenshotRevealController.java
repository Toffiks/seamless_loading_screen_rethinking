package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig.ScreenshotRevealMode;

/** Defines how each reveal mode behaves during the loading screen. */
public final class ScreenshotRevealController {
    private final ScreenshotRevealAnimation animation;

    public ScreenshotRevealController(long fullRevealDurationNanos) {
        animation = new ScreenshotRevealAnimation(fullRevealDurationNanos);
    }

    public float duringChunkLoading(ScreenshotRevealMode mode, float progress, long nowNanos) {
        if (mode == ScreenshotRevealMode.New) return animation.update(clamp(progress), nowNanos);
        return smoothstep(animation.update(1.0f, nowNanos));
    }

    public float currentVisibility(ScreenshotRevealMode mode) {
        float value = animation.getValue();
        return mode == ScreenshotRevealMode.Classic ? smoothstep(value) : value;
    }

    public float finishLoading() {
        animation.complete();
        return 1.0f;
    }

    public void reset() {
        animation.reset();
    }

    private static float clamp(float value) {
        if (!Float.isFinite(value)) return 0.0f;
        return Math.max(0.0f, Math.min(value, 1.0f));
    }

    private static float smoothstep(float value) {
        float clamped = clamp(value);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }
}
