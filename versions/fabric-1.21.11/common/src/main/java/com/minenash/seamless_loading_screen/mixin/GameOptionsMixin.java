package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeScreen;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

    @Inject(method = "getMenuBackgroundBlurrinessValue", at = @At("HEAD"), cancellable = true)
    private void useScreenshotBlurStrength(CallbackInfoReturnable<Integer> cir) {
        if (ScreenshotLoader.shouldUseBlur()) {
            int minecraftMenuBlur = ((GameOptions) (Object) this)
                    .getMenuBackgroundBlurriness()
                    .getValue();
            int loadingBlur = Math.max(minecraftMenuBlur,
                    Math.round(SeamlessLoadingScreenConfig.get().screenshotBlurStrength));
            float transition = MinecraftClient.getInstance().currentScreen instanceof WorldFadeScreen screen
                    ? screen.getTransitionAlpha()
                    : 1.0f;
            cir.setReturnValue(Math.max(0, Math.round(loadingBlur * transition)));
        }
    }
}
