package com.minenash.seamless_loading_screen.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.minenash.seamless_loading_screen.ScreenshotCapture;
import com.minenash.seamless_loading_screen.WorldFadeTransition;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V",
            shift = At.Shift.AFTER), require = 0)
    private void attemptToTakeScreenshot(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        if (ScreenshotCapture.shouldCapture()) ScreenshotCapture.captureFrame();
    }

    @Inject(method = "render",
            slice = @Slice(
                    from = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"),
                    to = @At(value = "INVOKE",
                            target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;")),
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V",
                    shift = At.Shift.AFTER))
    private void renderWorldFadeAfterHud(DeltaTracker tickCounter, boolean tick,
                                         CallbackInfo ci, @Local GuiGraphics graphics) {
        if (!WorldFadeTransition.isActive()) return;

        // Minecraft has cleared GUI depth here, as it does before rendering a Screen.
        // Drain buffered HUD elements before drawing the screenshot over the world.
        graphics.flush();
        RenderSystem.disableDepthTest();
        WorldFadeTransition.render(graphics);
        graphics.flush();
        RenderSystem.disableDepthTest();
    }

}
