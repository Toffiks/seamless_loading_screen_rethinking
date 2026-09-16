package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig.ScreenshotRevealMode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CompletedLoadingTransitionTest {
    @Test
    void fastLoadingCompletesRevealBeforeFirstWorldFrameInBothModes() {
        for (var mode : ScreenshotRevealMode.values()) {
            var reveal = new ScreenshotRevealController(250_000_000L);
            reveal.duringChunkLoading(mode, 0.0f, 0L);
            reveal.duringChunkLoading(mode, 0.8f, 50_000_000L);
            float completed = reveal.finishLoading();
            assertEquals(1.0f, completed);
            var fade = new FadeOutAnimation();
            fade.start(1_000_000_000L);
            // A delayed first world frame must not consume the fade in advance.
            assertEquals(1.0f, fade.valueForRender(10_000_000_000L));
            assertEquals(0.5f, fade.valueForRender(10_500_000_000L), 0.0001f);
            assertEquals(0.0f, fade.valueForRender(11_000_000_000L));
        }
    }
}

