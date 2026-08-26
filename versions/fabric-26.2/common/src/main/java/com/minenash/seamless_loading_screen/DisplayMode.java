package com.minenash.seamless_loading_screen;

public enum DisplayMode {
    ENABLED, FREEZE, DISABLED;

    public DisplayMode next() {
        return switch (this) {
            case ENABLED -> FREEZE;
            case FREEZE -> DISABLED;
            case DISABLED -> ENABLED;
        };
    }
}
