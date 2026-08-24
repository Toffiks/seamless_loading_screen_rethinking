package com.minenash.seamless_loading_screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PlatformFunctions {

    /**
     * This is our actual method to {@link PlatformFunctions#getConfigDirectory()}.
     */
    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean isClientEnv() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    //---

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static boolean isDevEnv() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    /**
     * FastQuit saves/disconnects asynchronously, so it is safe to delay its
     * exit action by one rendered frame while we capture the screenshot.
     */
    public static boolean hasFastQuit() {
        return FabricLoader.getInstance().isModLoaded("fastquit");
    }
}
