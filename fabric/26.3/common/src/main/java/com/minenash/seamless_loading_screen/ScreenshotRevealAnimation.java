package com.minenash.seamless_loading_screen;

/**
 * Smooths increases in screenshot opacity without ever exceeding the current target.
 */
public final class ScreenshotRevealAnimation {
    private final long fullRevealDurationNanos;

    private float value;
    private long lastUpdateNanos;
    private boolean started;

    public ScreenshotRevealAnimation(long fullRevealDurationNanos) {
        if (fullRevealDurationNanos <= 0L) {
            throw new IllegalArgumentException("fullRevealDurationNanos must be positive");
        }
        this.fullRevealDurationNanos = fullRevealDurationNanos;
    }

    public void reset() {
        value = 0.0f;
        lastUpdateNanos = 0L;
        started = false;
    }

    public float update(float requestedTarget, long nowNanos) {
        float safeTarget = Float.isFinite(requestedTarget)
                ? Math.max(0.0f, Math.min(requestedTarget, 1.0f))
                : value;

        if (!started) {
            lastUpdateNanos = nowNanos;
            started = true;
            return value;
        }

        long elapsedNanos = nowNanos - lastUpdateNanos;
        lastUpdateNanos = nowNanos;
        if (elapsedNanos <= 0L || value >= safeTarget) return value;

        float maximumStep = elapsedNanos / (float) fullRevealDurationNanos;
        value = Math.min(safeTarget, value + maximumStep);
        return value;
    }

    public void complete() {
        value = 1.0f;
        lastUpdateNanos = 0L;
        started = false;
    }

    public float getValue() {
        return value;
    }
}
