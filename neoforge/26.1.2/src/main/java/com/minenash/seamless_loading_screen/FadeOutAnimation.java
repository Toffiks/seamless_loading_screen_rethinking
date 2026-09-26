package com.minenash.seamless_loading_screen;

/** A fade-out whose clock starts on the first frame that can actually be rendered. */
public final class FadeOutAnimation {
    private long durationNanos;
    private long firstFrameNanos;
    private float startValue;
    private boolean frameStarted;

    public void start(long durationNanos, float startValue) {
        if (durationNanos <= 0L) throw new IllegalArgumentException("durationNanos must be positive");
        this.durationNanos = durationNanos;
        this.startValue = clamp(startValue);
        firstFrameNanos = 0L;
        frameStarted = false;
    }

    public float valueForRender(long nowNanos) {
        if (!frameStarted) {
            firstFrameNanos = nowNanos;
            frameStarted = true;
            return startValue;
        }
        return valueAt(nowNanos);
    }

    public float currentValue(long nowNanos) {
        return frameStarted ? valueAt(nowNanos) : startValue;
    }

    public boolean isFinished(long nowNanos) {
        return frameStarted && valueAt(nowNanos) <= 0.0f;
    }

    private float valueAt(long nowNanos) {
        long elapsedNanos = Math.max(0L, nowNanos - firstFrameNanos);
        float remaining = Math.max(0.0f, 1.0f - elapsedNanos / (float) durationNanos);
        return startValue * remaining * remaining * (3.0f - 2.0f * remaining);
    }

    private static float clamp(float value) {
        return Float.isFinite(value) ? Math.max(0.0f, Math.min(value, 1.0f)) : 0.0f;
    }
}
