package com.minenash.seamless_loading_screen;

/**
 * Exposes only the connection-cancel state needed by the screen lifecycle
 * guard, without coupling it to a generated button callback method name.
 */
public interface ConnectScreenState {
    boolean seamless_loading_screen$isConnectingCancelled();
}
