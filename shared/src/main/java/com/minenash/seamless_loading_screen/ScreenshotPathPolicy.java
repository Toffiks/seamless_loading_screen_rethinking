package com.minenash.seamless_loading_screen;

import java.util.Objects;

/** Stable primary screenshot paths which must not depend on display names. */
public final class ScreenshotPathPolicy {
    private ScreenshotPathPolicy() {}

    public static String realm(long realmId) {
        return "realms/realm_" + realmId + ".png";
    }

    public static String pngFileName(String baseName, String suffix) {
        Objects.requireNonNull(baseName, "baseName");
        Objects.requireNonNull(suffix, "suffix");
        if (suffix.length() > 250) throw new IllegalArgumentException("PNG suffix is too long");

        int maximumBaseLength = Math.max(1, 255 - 4 - suffix.length());
        return truncatePreservingSurrogates(baseName, maximumBaseLength) + suffix + ".png";
    }

    static String truncatePreservingSurrogates(String value, int maximumLength) {
        if (value.length() <= maximumLength) return value;
        int end = maximumLength;
        if (Character.isHighSurrogate(value.charAt(end - 1))) end--;
        return value.substring(0, end);
    }
}
