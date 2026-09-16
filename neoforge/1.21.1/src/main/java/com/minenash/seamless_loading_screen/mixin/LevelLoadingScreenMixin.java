package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.LoadingProgressAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen implements LoadingProgressAccess {

    protected LevelLoadingScreenMixin() {
        super(Component.empty());
    }

    @Shadow
    @Final
    private StoringChunkProgressListener progressListener;

    // NeoForge dev and production name this synthetic chunk-map lambda differently.
    @ModifyArg(method = {"lambda$renderChunks$1", "lambda$renderChunks$0"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V",
                    ordinal = 4),
            index = 4,
            require = 0)
    private static int seamless_loading_screen$makeEmptyChunksTranslucent(int color) {
        return color == 0xFF000000 ? 0xAA000000 : color;
    }

    @Override
    public float seamless_loading_screen$getProgress() {
        return Mth.clamp(progressListener.getProgress() / 100.0F, 0.0F, 1.0F);
    }
}
