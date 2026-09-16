package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkMapFadeTest {
    @Test
    void finalVanillaChunkMapFadesAfterItsScreenCloses() {
        ChunkMapFade.clear();
        ChunkMapFade.beginFrame(1920, 1080, true);
        ChunkMapFade.recordRect(950, 530, 960, 540, 0xFFFFFFFF);
        ChunkMapFade.start();

        assertEquals(1.0f, ChunkMapFade.alphaForFrame(10_000_000_000L));
        assertEquals(0.5f, ChunkMapFade.alphaForFrame(10_125_000_000L), 0.0001f);
        assertEquals(0.0f, ChunkMapFade.alphaForFrame(10_250_000_000L));
        ChunkMapFade.clear();
    }

    @Test
    void noSavedScreenshotDoesNotCreateAStaleCube() {
        ChunkMapFade.clear();
        ChunkMapFade.beginFrame(1920, 1080, false);
        ChunkMapFade.recordRect(950, 530, 960, 540, 0xFFFFFFFF);
        ChunkMapFade.start();
        assertEquals(0.0f, ChunkMapFade.alphaForFrame(10_000_000_000L));
        ChunkMapFade.clear();
    }
}
