package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScreenshotRevealAnimationTest {
    private static final long DURATION = 250_000_000L;

    @Test
    void smoothsAFullProgressJumpOver250Milliseconds() {
        var animation = new ScreenshotRevealAnimation(DURATION);

        assertEquals(0.0f, animation.update(1.0f, 1_000L));
        assertEquals(0.4f, animation.update(1.0f, 100_001_000L), 0.0001f);
        assertEquals(1.0f, animation.update(1.0f, 250_001_000L), 0.0001f);
    }

    @Test
    void resultDoesNotDependOnFrameCount() {
        var oneFrame = new ScreenshotRevealAnimation(DURATION);
        var fiveFrames = new ScreenshotRevealAnimation(DURATION);
        oneFrame.update(1.0f, 0L);
        fiveFrames.update(1.0f, 0L);

        assertEquals(1.0f, oneFrame.update(1.0f, DURATION), 0.0001f);
        for (int i = 1; i <= 5; i++) {
            fiveFrames.update(1.0f, i * 50_000_000L);
        }
        assertEquals(oneFrame.getValue(), fiveFrames.getValue(), 0.0001f);
    }

    @Test
    void lowerTargetStopsChasingTheOldHigherTarget() {
        var animation = new ScreenshotRevealAnimation(DURATION);
        animation.update(0.8f, 0L);
        animation.update(0.8f, 100_000_000L);

        assertEquals(0.4f, animation.update(0.1f, 150_000_000L), 0.0001f);
        assertEquals(0.4f, animation.update(0.1f, 250_000_000L), 0.0001f);
    }

    @Test
    void resetStartsANewRevealFromZero() {
        var animation = new ScreenshotRevealAnimation(DURATION);
        animation.update(1.0f, 0L);
        animation.update(1.0f, DURATION);
        animation.reset();

        assertEquals(0.0f, animation.getValue());
        assertEquals(0.0f, animation.update(0.5f, DURATION + 1L));
    }

    @Test
    void invalidDurationIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ScreenshotRevealAnimation(0L));
    }

    @Test
    void completeImmediatelySetsFullOpacity() {
        var animation = new ScreenshotRevealAnimation(DURATION);
        animation.update(0.5f, 0L);
        animation.update(0.5f, 50_000_000L);

        animation.complete();

        assertEquals(1.0f, animation.getValue());
    }

}
