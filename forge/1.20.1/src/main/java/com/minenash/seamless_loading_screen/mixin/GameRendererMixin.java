package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotCapture;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(FJLcom/mojang/blaze3d/vertex/PoseStack;)V",
            shift = At.Shift.AFTER), require = 0)
    private void attemptToTakeScreenshot(float partialTick, long nanoTime,
                                         boolean renderLevel, CallbackInfo ci) {
        if (ScreenshotCapture.shouldCapture()) ScreenshotCapture.captureFrame();
    }

}
