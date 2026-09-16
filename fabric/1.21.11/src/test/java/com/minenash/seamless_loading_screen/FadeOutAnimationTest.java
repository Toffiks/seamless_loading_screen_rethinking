package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FadeOutAnimationTest {
    private static final long DURATION = 1_000_000_000L;

    @Test
    void waitingForTheFirstWorldFrameDoesNotConsumeTheFade() {
        var fade = new FadeOutAnimation();
        fade.start(DURATION, 0.8f);
        assertEquals(0.8f, fade.currentValue(10_000_000_000L));
        assertFalse(fade.isFinished(10_000_000_000L));
        assertEquals(0.8f, fade.valueForRender(10_000_000_000L));
    }

    @Test
    void fadeRunsFromTheVisibilityShownByTheLoadingScreen() {
        var fade = new FadeOutAnimation();
        fade.start(DURATION, 0.8f);
        fade.valueForRender(10_000_000_000L);
        assertEquals(0.4f, fade.valueForRender(10_500_000_000L), 0.0001f);
        assertEquals(0.0f, fade.valueForRender(11_000_000_000L), 0.0001f);
        assertTrue(fade.isFinished(11_000_000_000L));
    }

}
