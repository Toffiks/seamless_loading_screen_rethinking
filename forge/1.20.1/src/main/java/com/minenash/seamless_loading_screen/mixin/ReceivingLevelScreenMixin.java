package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeTransition;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin extends Screen {
    protected ReceivingLevelScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;renderDirtBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
            shift = At.Shift.AFTER))
    private void seamless_loading_screen$keepScreenshotUntilWorldIsReady(
            GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci
    ) {
        if (!ScreenshotLoader.isLoadingScreenPending()) return;

        ScreenshotLoader.renderFinalLoading(this, context);
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void seamless_loading_screen$startWorldFade(CallbackInfo ci) {
        if (!ScreenshotLoader.isLoadingScreenPending()) return;

        ScreenshotLoader.beginLoadingScreen();
        WorldFadeTransition.start(Math.max(1, SeamlessLoadingScreenConfig.get().fade));
    }
}
