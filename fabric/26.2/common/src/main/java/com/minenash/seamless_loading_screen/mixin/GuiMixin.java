package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ConnectScreenState;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeTransition;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = Gui.class, priority = 900)
public abstract class GuiMixin {
    @Shadow @Nullable private Screen screen;

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, index = 1)
    private Screen seamless_loading_screen$addWorldTransition(Screen nextScreen) {
        if (nextScreen instanceof LevelLoadingScreen) {
            ScreenshotLoader.prepareDeferredWorldScreenshot();
        }

        if (ScreenshotLoader.isTransitionReady()) {
            boolean connectionCancelled = screen instanceof ConnectScreen
                    && screen instanceof ConnectScreenState state
                    && state.seamless_loading_screen$isConnectingCancelled();
            boolean unusedLoadingScreenReplaced = screen instanceof LevelLoadingScreen
                    && nextScreen != null && nextScreen != screen;

            if (connectionCancelled || unusedLoadingScreenReplaced) {
                ScreenshotLoader.cancelPendingLoadingScreen();
            }
        }

        if (nextScreen instanceof DisconnectedScreen && ScreenshotLoader.isLoadingScreenPending()) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (WorldFadeTransition.isActive() && nextScreen != null) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (!(screen instanceof LevelLoadingScreen) || !ScreenshotLoader.isLoadingScreenPending()) {
            return nextScreen;
        }

        if (nextScreen != null) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        ScreenshotLoader.beginLoadingScreen();
        WorldFadeTransition.start(Math.max(1, SeamlessLoadingScreenConfig.get().fade));
        return null;
    }
}
