package com.minenash.seamless_loading_screen.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.seamless_loading_screen.LoadingProgressAccess;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Screen.class, priority = 900)
public abstract class ScreenBackgroundMixin {

    @WrapOperation(method = "renderBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;renderBlurredBackground(F)V"))
    private void seamless_loading_screen$insertScreenshotBeforeBlur(
            Screen screen, float blurPartialTick, Operation<Void> original,
            GuiGraphics context, int mouseX, int mouseY, float partialTick
    ) {
        if (!(screen instanceof LevelLoadingScreen)
                || !ScreenshotLoader.isLoadingScreenPending()) {
            original.call(screen, blurPartialTick);
            return;
        }

        ScreenshotLoader.beginLoadingScreen();
        float progress = ((LoadingProgressAccess) screen).seamless_loading_screen$getProgress();
        ScreenshotLoader.renderLoadingBackground(screen, context, progress);

        if (SeamlessLoadingScreenConfig.get().enableScreenshotBlur) {
            context.flush();
            original.call(screen, blurPartialTick);
        }
    }
}
