package com.minenash.seamless_loading_screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenshotPathPolicyTest {
    @Test
    void realmPathDependsOnlyOnItsStableId() {
        assertEquals("realms/realm_987654321.png", ScreenshotPathPolicy.realm(987654321L));
    }

    @Test
    void serverPortAndExtensionAlwaysFitInsideOneFileNameComponent() {
        String fileName = ScreenshotPathPolicy.pngFileName("a".repeat(251), "_65535");

        assertEquals(255, fileName.length());
        assertTrue(fileName.endsWith("_65535.png"));
    }

    @Test
    void truncationDoesNotSplitASurrogatePair() {
        String fileName = ScreenshotPathPolicy.pngFileName("a".repeat(250) + "\uD83D\uDE80", "");
        String stem = fileName.substring(0, fileName.length() - 4);

        assertFalse(Character.isHighSurrogate(stem.charAt(stem.length() - 1)));
    }
}
