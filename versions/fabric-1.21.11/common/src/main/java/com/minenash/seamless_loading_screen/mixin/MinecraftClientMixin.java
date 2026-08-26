package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.OnLeaveHelper;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeTransition;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClient.class, priority = 900)
public abstract class MinecraftClientMixin {

    @Shadow
    static MinecraftClient instance;
    @Shadow
    @Nullable
    public Screen currentScreen;

    @Unique
    private boolean seamless_loading_screen$firstOccurrence = true;
    @Shadow
    public abstract void scheduleStop();

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, index = 1)
    private Screen addWorldTransition(Screen nextScreen) {
        if (nextScreen instanceof DisconnectedScreen && ScreenshotLoader.isLoadingScreenPending()) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (WorldFadeTransition.isActive() && nextScreen != null) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (!(currentScreen instanceof LevelLoadingScreen) || !ScreenshotLoader.isTransitionActive()) {
            return nextScreen;
        }

        if (nextScreen != null) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        WorldFadeTransition.start(Math.max(1, SeamlessLoadingScreenConfig.get().fade));
        return null;
    }

    @Inject(method = "scheduleStop", at = @At("HEAD"), cancellable = true)
    private void onWindowClose(CallbackInfo info) {
        if (!seamless_loading_screen$firstOccurrence) return;

        Runnable stopClient = () -> {
            this.seamless_loading_screen$firstOccurrence = false;
            this.scheduleStop();
        };

        if (instance.player != null) {
            OnLeaveHelper.beginScreenshotTask(stopClient, true);
            info.cancel();
        } else if (OnLeaveHelper.awaitPendingSaves(stopClient)) {
            info.cancel();
        }
    }
}
