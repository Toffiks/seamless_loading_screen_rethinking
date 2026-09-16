package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig.ScreenshotResolution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenshotResolutionTest {
    @Test
    void nativeKeepsTheFramebufferSize() {
        assertEquals(new ScreenshotResolution.Size(3440, 1440),
                ScreenshotResolution.Native.resolve(3440, 1440));
    }

    @Test
    void optimizedUsesLowerResolutionTiers() {
        assertOptimized(1920, 1080, 1280, 720);
        assertOptimized(2560, 1440, 1920, 1080);
        assertOptimized(3840, 2160, 2560, 1440);
        assertOptimized(7680, 4320, 3840, 2160);
    }

    @Test
    void optimizedPreservesNonStandardAspectRatios() {
        assertOptimized(3440, 1440, 1920, 804);
        assertOptimized(1280, 720, 854, 480);
        assertOptimized(1080, 1920, 720, 1280);
    }

    private static void assertOptimized(int sourceWidth, int sourceHeight,
                                        int expectedWidth, int expectedHeight) {
        assertEquals(new ScreenshotResolution.Size(expectedWidth, expectedHeight),
                ScreenshotResolution.Optimized.resolve(sourceWidth, sourceHeight));
    }
}
