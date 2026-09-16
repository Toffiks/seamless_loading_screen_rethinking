package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig.ScreenshotRevealMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenshotRevealControllerTest {
    private static final long DURATION = 250_000_000L;

    @Test
    void newModeStaysBoundToProgress() {
        var reveal = new ScreenshotRevealController(DURATION);
        assertEquals(0.0f, reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.0f, 0L));
        assertEquals(0.2f, reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.2f, 50_000_000L), 0.0001f);
        assertEquals(0.4f, reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.8f, 100_000_000L), 0.0001f);
        assertEquals(0.8f, reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.8f, 200_000_000L), 0.0001f);
    }

    @Test
    void classicModeFadesInIndependentlyOfProgress() {
        var reveal = new ScreenshotRevealController(DURATION);
        assertEquals(0.0f, reveal.duringChunkLoading(ScreenshotRevealMode.Classic, 0.0f, 2_000_000_000L));
        assertEquals(0.5f, reveal.duringChunkLoading(ScreenshotRevealMode.Classic, 0.0f, 2_125_000_000L), 0.0001f);
        assertEquals(1.0f, reveal.duringChunkLoading(ScreenshotRevealMode.Classic, 0.0f, 2_250_000_000L), 0.0001f);
    }

    @Test
    void worldFadeStartsAtTheLastActuallyRenderedVisibility() {
        var reveal = new ScreenshotRevealController(DURATION);
        reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.0f, 0L);
        reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.8f, 100_000_000L);
        assertEquals(0.4f, reveal.currentVisibility(ScreenshotRevealMode.New), 0.0001f);
    }

    @Test
    void finalLoadingStageIsFullyVisible() {
        var reveal = new ScreenshotRevealController(DURATION);
        reveal.duringChunkLoading(ScreenshotRevealMode.New, 0.4f, 0L);
        assertEquals(1.0f, reveal.finishLoading());
        assertEquals(1.0f, reveal.currentVisibility(ScreenshotRevealMode.New));
    }
}

