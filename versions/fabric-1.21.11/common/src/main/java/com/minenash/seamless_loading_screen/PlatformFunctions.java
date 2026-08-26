package com.minenash.seamless_loading_screen;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class PlatformFunctions {

    public static Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

}
