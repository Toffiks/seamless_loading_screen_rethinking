package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public abstract class LevelLoadingScreenMixin extends Screen {

    protected LevelLoadingScreenMixin() {
        super(Component.empty());
    }

    @Shadow
    @Final
    private StoringChunkProgressListener progressListener;

    @Accessor("COLORS")
    private static Object2IntMap<ChunkStatus> seamless_loading_screen$getColors() {
        throw new AssertionError();
    }

    @Inject(method = "renderChunks", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("deprecation")
    private static void seamless_loading_screen$renderTranslucentChunkMap(
            GuiGraphics context, StoringChunkProgressListener listener,
            int centerX, int centerY, int cellSize, int borderSize, CallbackInfo ci
    ) {
        int step = cellSize + borderSize;
        int fullWidth = listener.getFullDiameter() * step - borderSize;
        int diameter = listener.getDiameter();
        int visibleWidth = diameter * step - borderSize;
        int left = centerX - visibleWidth / 2;
        int top = centerY - visibleWidth / 2;
        int borderRadius = fullWidth / 2 + 1;
        Object2IntMap<ChunkStatus> colors = seamless_loading_screen$getColors();

        context.drawManaged(() -> {
            if (borderSize != 0) {
                int borderColor = 0xFF0011FF;
                context.fill(centerX - borderRadius, centerY - borderRadius,
                        centerX - borderRadius + 1, centerY + borderRadius, borderColor);
                context.fill(centerX + borderRadius - 1, centerY - borderRadius,
                        centerX + borderRadius, centerY + borderRadius, borderColor);
                context.fill(centerX - borderRadius, centerY - borderRadius,
                        centerX + borderRadius, centerY - borderRadius + 1, borderColor);
                context.fill(centerX - borderRadius, centerY + borderRadius - 1,
                        centerX + borderRadius, centerY + borderRadius, borderColor);
            }

            for (int x = 0; x < diameter; x++) {
                for (int y = 0; y < diameter; y++) {
                    ChunkStatus status = listener.getStatus(x, y);
                    int alpha = status == null || status == ChunkStatus.EMPTY
                            ? 0xAA000000
                            : 0xFF000000;
                    int color = colors.getInt(status) | alpha;
                    int cellX = left + x * step;
                    int cellY = top + y * step;
                    context.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, color);
                }
            }
        });
        ci.cancel();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
            shift = At.Shift.AFTER))
    private void seamless_loading_screen$renderScreenshotOverVanillaBackground(
            GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci
    ) {
        if (!ScreenshotLoader.isLoadingScreenPending()) return;
        float progress = Mth.clamp(progressListener.getProgress() / 100.0F, 0.0F, 1.0F);
        ScreenshotLoader.renderChunkLoading(this, context, progress);
    }
}
