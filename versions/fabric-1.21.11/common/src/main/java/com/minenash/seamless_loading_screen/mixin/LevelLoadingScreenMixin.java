package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    protected LevelLoadingScreenMixin() {
        super(Text.empty());
    }

    @Shadow
    private float loadProgress;

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void renderProgressiveScreenshot(DrawContext context, int mouseX, int mouseY,
                                             float delta, CallbackInfo ci) {
        if (!ScreenshotLoader.isLoadingScreenPending()) return;

        ScreenshotLoader.beginLoadingScreen();
        renderPanoramaBackground(context, delta);
        renderDarkening(context);
        ScreenshotLoader.render(this, context, loadProgress);
        ci.cancel();
    }

    /**
     * Preserve the original mod's translucent empty portion of the chunk map.
     * Generated chunk-status colors remain unchanged.
     */
    @ModifyArg(method = "drawChunkMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
                    ordinal = 1),
            index = 4)
    private static int makeEmptyChunksTranslucent(int color) {
        return color == 0xFF000000 ? 0xAA000000 : color;
    }
}
