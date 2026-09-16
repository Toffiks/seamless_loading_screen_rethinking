package com.minenash.seamless_loading_screen;

/**
 * Small state machine for a screenshot prepared before a world loading screen.
 * It has no Minecraft dependencies so cancellation and replacement paths can be
 * tested without launching the game.
 */
public final class LoadingScreenLifecycle {
    private State state = State.NONE;

    public boolean isPending() {
        return state != State.NONE;
    }

    public boolean isReady() {
        return state == State.READY;
    }

    public boolean isActive() {
        return state == State.ACTIVE;
    }

    public void prepare() {
        state = State.READY;
    }

    public boolean begin() {
        if (state != State.READY) return false;
        state = State.ACTIVE;
        return true;
    }

    public boolean cancelReady() {
        if (state != State.READY) return false;
        state = State.NONE;
        return true;
    }

    public void finish() {
        state = State.NONE;
    }

    private enum State {
        NONE,
        READY,
        ACTIVE
    }
}
