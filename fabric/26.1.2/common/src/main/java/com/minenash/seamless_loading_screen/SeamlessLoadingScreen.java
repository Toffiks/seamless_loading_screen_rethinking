package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import dev.isxander.yacl3.gui.YACLScreen;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SeamlessLoadingScreen {

    public static final String MODID = "seamless_loading_screen";

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void onInitializeClient() {
        SeamlessLoadingScreenConfig.load();

        try {
            Path path = FabricLoader.getInstance().getGameDir().resolve("screenshots/worlds");
            Files.createDirectories(path.resolve("singleplayer"));
            Files.createDirectories(path.resolve("servers"));
            Files.createDirectories(path.resolve("realms"));
            Files.createDirectories(path.resolve("archive"));
        } catch (IOException e) {
            LOGGER.error("[SeamlessLoadingScreen] A problem when creating the various needed Directories, there might be problems!", e);
        }
    }

    public static void openSettingsScreen(Minecraft client) {
        if (client.screen instanceof YACLScreen) return;
        client.setScreen(SeamlessLoadingScreenConfig.getInstance().generateScreen(client.screen));
    }
}
