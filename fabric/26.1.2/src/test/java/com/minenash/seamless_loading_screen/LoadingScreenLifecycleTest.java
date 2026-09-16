package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadingScreenLifecycleTest {
    @Test
    void cancelledReadyScreenReleasesItsState() {
        LoadingScreenLifecycle lifecycle = new LoadingScreenLifecycle();
        lifecycle.prepare();

        assertTrue(lifecycle.isReady());
        assertTrue(lifecycle.cancelReady());
        assertFalse(lifecycle.isPending());
    }

    @Test
    void cancellationCannotInterruptAnActiveFade() {
        LoadingScreenLifecycle lifecycle = new LoadingScreenLifecycle();
        lifecycle.prepare();
        assertTrue(lifecycle.begin());

        assertFalse(lifecycle.cancelReady());
        assertTrue(lifecycle.isActive());

        lifecycle.finish();
        assertFalse(lifecycle.isPending());
    }

    @Test
    void replacementCanPrepareANewScreenshotAfterCancellation() {
        LoadingScreenLifecycle lifecycle = new LoadingScreenLifecycle();
        lifecycle.prepare();
        assertTrue(lifecycle.cancelReady());

        lifecycle.prepare();
        assertTrue(lifecycle.begin());
        assertTrue(lifecycle.isActive());
    }
}
