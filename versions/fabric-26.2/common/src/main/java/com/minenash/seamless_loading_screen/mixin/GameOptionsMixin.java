package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeScreen;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class GameOptionsMixin {

    @Inject(method = "getMenuBackgroundBlurriness", at = @At("HEAD"), cancellable = true)
    private void useScreenshotBlurStrength(CallbackInfoReturnable<Integer> cir) {
        if (ScreenshotLoader.shouldUseBlur()) {
            int minecraftMenuBlur = ((Options) (Object) this).menuBackgroundBlurriness().get();
            int loadingBlur = Math.max(minecraftMenuBlur,
                    Math.round(SeamlessLoadingScreenConfig.get().screenshotBlurStrength));
            float transition = Minecraft.getInstance().gui.screen() instanceof WorldFadeScreen screen
                    ? screen.getTransitionAlpha()
                    : 1.0f;
            cir.setReturnValue(Math.max(0, Math.round(loadingBlur * transition)));
        }
    }
}
