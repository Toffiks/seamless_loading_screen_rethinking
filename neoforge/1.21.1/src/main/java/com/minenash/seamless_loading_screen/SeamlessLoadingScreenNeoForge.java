package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = SeamlessLoadingScreen.MODID, dist = Dist.CLIENT)
public final class SeamlessLoadingScreenNeoForge {
    private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
            "seamless_loading_screen.keybind.config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyMapping.CATEGORY_MISC);

    public SeamlessLoadingScreenNeoForge(IEventBus modEventBus, ModContainer container) {
        SeamlessLoadingScreen.onInitializeClient();
        modEventBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (ignored, parent) -> SeamlessLoadingScreenConfig.getInstance().generateScreen(parent));
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        WorldFadeTransition.clientTick();
        ScreenshotLoader.clientTick();
        while (OPEN_SETTINGS.consumeClick()) {
            SeamlessLoadingScreen.openSettingsScreen(Minecraft.getInstance());
        }
    }

}
