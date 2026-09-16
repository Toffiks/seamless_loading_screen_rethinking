package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeTransition;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {

    @Inject(method = "getMenuBackgroundBlurrinessValue", at = @At("HEAD"), cancellable = true, require = 0)
    private void useScreenshotBlurStrength(CallbackInfoReturnable<Integer> cir) {
        if (ScreenshotLoader.shouldUseBlur() && WorldFadeTransition.isActive()) {
            int minecraftMenuBlur = ((GameOptions) (Object) this)
                    .getMenuBackgroundBlurriness()
                    .getValue();
            float transition = WorldFadeTransition.getTransitionAlpha();
            cir.setReturnValue(Math.max(0, Math.round(minecraftMenuBlur * transition)));
        }
    }
}
