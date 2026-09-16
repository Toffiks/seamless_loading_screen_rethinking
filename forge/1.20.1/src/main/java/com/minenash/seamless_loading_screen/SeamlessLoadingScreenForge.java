package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod(SeamlessLoadingScreen.MODID)
public final class SeamlessLoadingScreenForge {

    public SeamlessLoadingScreenForge(FMLJavaModLoadingContext context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.initialize(context));
    }

    private static final class Client {
        private static final KeyMapping OPEN_SETTINGS = new KeyMapping(
                "seamless_loading_screen.keybind.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.CATEGORY_MISC);

        private static void initialize(FMLJavaModLoadingContext context) {
            SeamlessLoadingScreen.onInitializeClient();

            IEventBus modBus = context.getModEventBus();
            modBus.addListener(Client::registerKeyMappings);
            MinecraftForge.EVENT_BUS.addListener(Client::onClientTick);
            MinecraftForge.EVENT_BUS.addListener(Client::onRenderGui);

            context.registerExtensionPoint(
                    ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (minecraft, parent) -> SeamlessLoadingScreenConfig.getInstance().generateScreen(parent)));
        }

        private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(OPEN_SETTINGS);
        }

        private static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;

            ScreenshotLoader.clientTick();
            while (OPEN_SETTINGS.consumeClick()) {
                SeamlessLoadingScreen.openSettingsScreen(Minecraft.getInstance());
            }
        }

        private static void onRenderGui(RenderGuiEvent.Post event) {
            if (!WorldFadeTransition.isActive()) return;

            var graphics = event.getGuiGraphics();
            graphics.flush();
            WorldFadeTransition.render(graphics);
            graphics.flush();
        }
    }
}
