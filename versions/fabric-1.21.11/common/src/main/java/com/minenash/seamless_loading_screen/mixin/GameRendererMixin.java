package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.OnLeaveHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/fog/FogRenderer;rotate()V"))
    private void attemptToTakeScreenshot(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        if (OnLeaveHelper.shouldTakeScreenshot()) OnLeaveHelper.takeScreenShot();
    }

}
