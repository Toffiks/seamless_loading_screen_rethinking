package com.minenash.seamless_loading_screen;

/** A fade-out whose clock starts on the first frame that can actually be rendered. */
public final class FadeOutAnimation {
    private long durationNanos;
    private long firstFrameNanos;
    private boolean frameStarted;

    public void start(long durationNanos) {
        if (durationNanos <= 0L) throw new IllegalArgumentException("durationNanos must be positive");
        this.durationNanos = durationNanos;
        firstFrameNanos = 0L;
        frameStarted = false;
    }

    public float valueForRender(long nowNanos) {
        if (!frameStarted) {
            firstFrameNanos = nowNanos;
            frameStarted = true;
            return 1.0f;
        }
        return valueAt(nowNanos);
    }

    public float currentValue(long nowNanos) {
        return frameStarted ? valueAt(nowNanos) : 1.0f;
    }

    public boolean isFinished(long nowNanos) {
        return frameStarted && valueAt(nowNanos) <= 0.0f;
    }

    private float valueAt(long nowNanos) {
        long elapsedNanos = Math.max(0L, nowNanos - firstFrameNanos);
        float remaining = Math.max(0.0f, 1.0f - elapsedNanos / (float) durationNanos);
        return remaining * remaining * (3.0f - 2.0f * remaining);
    }
}
