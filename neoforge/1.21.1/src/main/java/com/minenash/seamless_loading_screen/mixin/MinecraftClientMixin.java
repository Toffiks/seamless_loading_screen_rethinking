package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ConnectScreenState;
import com.minenash.seamless_loading_screen.ScreenshotCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeTransition;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 900)
public abstract class MinecraftClientMixin {

    @Shadow
    static Minecraft instance;
    @Shadow public Screen screen;

    @Unique
    private boolean seamless_loading_screen$firstOccurrence = true;
    @Shadow
    public abstract void stop();

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, index = 1)
    private Screen seamless_loading_screen$addWorldTransition(Screen nextScreen) {
        if (nextScreen instanceof LevelLoadingScreen) {
            ScreenshotLoader.prepareDeferredWorldScreenshot();
        }

        if (ScreenshotLoader.isTransitionReady()) {
            boolean connectionCancelled = screen instanceof ConnectScreen
                    && screen instanceof ConnectScreenState state
                    && state.seamless_loading_screen$isConnectingCancelled();
            if (connectionCancelled) ScreenshotLoader.cancelPendingLoadingScreen();
        }

        if (nextScreen instanceof DisconnectedScreen && ScreenshotLoader.isLoadingScreenPending()) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (WorldFadeTransition.isActive() && nextScreen != null) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (!ScreenshotLoader.isLoadingScreenPending()) {
            return nextScreen;
        }

        if (screen instanceof LevelLoadingScreen) {
            // The chunk screen can close before "Loading Terrain" appears.
            // Keep its screenshot alive; the world fade belongs to the final screen.
            if (nextScreen == null
                    || nextScreen instanceof LevelLoadingScreen
                    || nextScreen instanceof ReceivingLevelScreen) return nextScreen;

            WorldFadeTransition.finish();
            return nextScreen;
        }

        if (!(screen instanceof ReceivingLevelScreen)) return nextScreen;

        if (nextScreen instanceof LevelLoadingScreen
                || nextScreen instanceof ReceivingLevelScreen) return nextScreen;

        if (nextScreen != null) {
            WorldFadeTransition.finish();
            return nextScreen;
        }

        ScreenshotLoader.beginLoadingScreen();
        WorldFadeTransition.start(Math.max(1, SeamlessLoadingScreenConfig.get().fade));
        return null;
    }

    @Inject(method = "stop", at = @At("HEAD"), cancellable = true)
    private void onWindowClose(CallbackInfo info) {
        if (!seamless_loading_screen$firstOccurrence) return;

        Runnable stopClient = () -> {
            this.seamless_loading_screen$firstOccurrence = false;
            this.stop();
        };

        if (instance.player != null) {
            ScreenshotCapture.begin(stopClient, true);
            info.cancel();
        } else if (ScreenshotCapture.awaitPendingSave(stopClient)) {
            info.cancel();
        }
    }
}
