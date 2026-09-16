package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.quickplay.QuickPlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(QuickPlay.class)
public abstract class QuickPlayMixin {

    @Inject(method = "joinSingleplayerWorld", at = @At("HEAD"))
    private static void getLevelName(Minecraft client, String levelName, CallbackInfo ci) {
        ScreenshotLoader.setScreenshot(levelName);
    }

}

