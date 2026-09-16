package com.minenash.seamless_loading_screen.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReceivingLevelScreen.class)
public abstract class ReceivingLevelScreenMixin extends Screen {
    protected ReceivingLevelScreenMixin(Component title) {
        super(title);
    }

    @WrapOperation(method = "renderBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/ReceivingLevelScreen;renderBlurredBackground(F)V"))
    private void seamless_loading_screen$insertScreenshotBeforeBlur(
            ReceivingLevelScreen screen, float blurPartialTick, Operation<Void> original,
            GuiGraphics context, int mouseX, int mouseY, float partialTick
    ) {
        if (!ScreenshotLoader.isLoadingScreenPending()) {
            original.call(screen, blurPartialTick);
            return;
        }

        ScreenshotLoader.renderFinalLoading(this, context);

        if (SeamlessLoadingScreenConfig.get().enableScreenshotBlur) {
            context.flush();
            original.call(screen, blurPartialTick);
        }
    }
}
