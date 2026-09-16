package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FadeOutAnimationTest {
    private static final long DURATION = 1_000_000_000L;

    @Test
    void waitingForTheFirstWorldFrameDoesNotConsumeTheFade() {
        var fade = new FadeOutAnimation();
        fade.start(DURATION);

        assertEquals(1.0f, fade.currentValue(10_000_000_000L));
        assertFalse(fade.isFinished(10_000_000_000L));
        assertEquals(1.0f, fade.valueForRender(10_000_000_000L));
    }

    @Test
    void fadeRunsFromTheFirstRenderedWorldFrame() {
        var fade = new FadeOutAnimation();
        fade.start(DURATION);
        fade.valueForRender(10_000_000_000L);

        assertEquals(0.5f, fade.valueForRender(10_500_000_000L), 0.0001f);
        assertEquals(0.0f, fade.valueForRender(11_000_000_000L), 0.0001f);
        assertTrue(fade.isFinished(11_000_000_000L));
    }

    @Test
    void restartWaitsForANewFirstFrame() {
        var fade = new FadeOutAnimation();
        fade.start(DURATION);
        fade.valueForRender(0L);
        fade.valueForRender(DURATION);

        fade.start(DURATION);

        assertEquals(1.0f, fade.valueForRender(20_000_000_000L));
    }

    @Test
    void invalidDurationIsRejected() {
        var fade = new FadeOutAnimation();
        assertThrows(IllegalArgumentException.class, () -> fade.start(0L));
    }
}
