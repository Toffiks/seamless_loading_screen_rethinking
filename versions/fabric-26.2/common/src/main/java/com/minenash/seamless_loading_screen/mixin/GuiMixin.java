package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeScreen;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.Gui;
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
        if (!(screen instanceof LevelLoadingScreen) || !ScreenshotLoader.isTransitionActive()) {
            return nextScreen;
        }

        if (nextScreen != null) {
            ScreenshotLoader.finishLoadingScreen();
            return nextScreen;
        }

        return new WorldFadeScreen(Math.max(1, SeamlessLoadingScreenConfig.get().fade));
    }
}
