package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @ModifyArg(method = "extractChunksForRendering",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                    ordinal = 1),
            index = 4)
    private static int makeEmptyChunksTranslucent(int color) {
        return color == 0xFF000000 ? 0xAA000000 : color;
    }

    protected LevelLoadingScreenMixin() {
        super(Component.empty());
    }

    @Shadow
    private float smoothedProgress;

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
    private void renderProgressiveScreenshot(GuiGraphicsExtractor context, int mouseX, int mouseY,
                                             float delta, CallbackInfo ci) {
        if (!ScreenshotLoader.isLoadingScreenPending()) return;

        ScreenshotLoader.beginLoadingScreen();
        extractPanorama(context, delta);
        extractMenuBackground(context);
        ScreenshotLoader.render(this, context, smoothedProgress);
        ci.cancel();
    }
}
