package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ChunkMapFade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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

    @Inject(method = "extractChunksForRendering", at = @At("HEAD"))
    private static void seamless_loading_screen$beginChunkMapCapture(
            GuiGraphicsExtractor context, int centerX, int centerY, int tileSize, int gap,
            net.minecraft.server.level.progress.ChunkLoadStatusView view, CallbackInfo ci) {
        ChunkMapFade.beginFrame(context.guiWidth(), context.guiHeight(),
                ScreenshotLoader.isLoadingScreenPending());
    }

    @ModifyArgs(method = "extractChunksForRendering",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"))
    private static void seamless_loading_screen$captureChunkMapRectangle(Args args) {
        ChunkMapFade.recordRect(args.get(0), args.get(1), args.get(2), args.get(3), args.get(4));
    }

    @ModifyArg(method = "extractChunksForRendering",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V",
                    ordinal = 1),
            index = 4,
            require = 0)
    private static int makeEmptyChunksTranslucent(int color) {
        return color == 0xFF000000 ? 0xAA000000 : color;
    }

    protected LevelLoadingScreenMixin() {
        super(Component.empty());
    }

    @Shadow
    private float smoothedProgress;

    /**
     * Keeps Minecraft's background pipeline intact:
     * panorama -> screenshot -> vanilla blur -> menu darkening.
     */
    @WrapOperation(method = "extractBackground",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;extractBlurredBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;)V"))
    private void renderScreenshotBeforeVanillaBlur(LevelLoadingScreen screen,
                                                   GuiGraphicsExtractor context,
                                                   Operation<Void> original) {
        if (!ScreenshotLoader.isLoadingScreenPending()) {
            original.call(screen, context);
            return;
        }

        ScreenshotLoader.beginLoadingScreen();
        ScreenshotLoader.renderLoadingBackground(this, context, smoothedProgress);

        if (SeamlessLoadingScreenConfig.get().enableScreenshotBlur) {
            // Blur applies before the current extraction stratum, so finish the
            // screenshot stratum before delegating to Minecraft's blur method.
            context.nextStratum();
            original.call(screen, context);
        }
    }
}
