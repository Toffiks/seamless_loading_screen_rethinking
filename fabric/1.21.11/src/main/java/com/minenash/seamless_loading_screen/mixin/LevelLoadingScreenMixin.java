package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ChunkMapFade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    @Inject(method = "drawChunkMap", at = @At("HEAD"))
    private static void seamless_loading_screen$beginChunkMapCapture(
            DrawContext context, int centerX, int centerY, int tileSize, int gap,
            net.minecraft.world.chunk.ChunkLoadMap map, CallbackInfo ci) {
        ChunkMapFade.beginFrame(context.getScaledWindowWidth(), context.getScaledWindowHeight(),
                ScreenshotLoader.isLoadingScreenPending());
    }

    @ModifyArgs(method = "drawChunkMap",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private static void seamless_loading_screen$captureChunkMapRectangle(Args args) {
        ChunkMapFade.recordRect(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4));
    }

    protected LevelLoadingScreenMixin() {
        super(Text.empty());
    }

    @Shadow
    private float loadProgress;

    /**
     * Keeps the vanilla OTHER background pipeline intact:
     * panorama -> screenshot -> vanilla blur -> darkening.
     *
     * The extra root layer is only an ordering boundary. Minecraft 1.21.11
     * considers the root layer marked by applyBlur() to be after the blur, so
     * the screenshot must be finalized in the preceding layer.
     */
    @WrapOperation(method = "renderBackground",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screen/world/LevelLoadingScreen;applyBlur(Lnet/minecraft/client/gui/DrawContext;)V"),
            require = 1)
    private void renderScreenshotBeforeVanillaBlur(LevelLoadingScreen screen, DrawContext context,
                                                    Operation<Void> original) {
        if (!ScreenshotLoader.isLoadingScreenPending()) {
            original.call(screen, context);
            return;
        }

        ScreenshotLoader.beginLoadingScreen();
        ScreenshotLoader.renderLoadingBackground(this, context, loadProgress);

        if (SeamlessLoadingScreenConfig.get().enableScreenshotBlur) {
            context.createNewRootLayer();
            original.call(screen, context);
        }
    }

    /**
     * Preserve the original mod's translucent empty portion of the chunk map.
     * Generated chunk-status colors remain unchanged.
     */
    @ModifyArg(method = "drawChunkMap",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V",
                    ordinal = 1),
            index = 4,
            require = 0)
    private static int makeEmptyChunksTranslucent(int color) {
        return color == 0xFF000000 ? 0xAA000000 : color;
    }
}
