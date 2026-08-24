package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.OnLeaveHelper;
import com.minenash.seamless_loading_screen.PlatformFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.network.chat.Component;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.WorldFadeScreen;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import org.jspecify.annotations.Nullable;
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
    @Shadow @Nullable public Screen screen;

    @Unique
    private boolean seamless_loading_screen$firstOccurrence = true;
    @Unique
    private boolean seamless_loading_screen$bypassDisconnectCapture = false;
    @Shadow
    public abstract void stop();
    @Shadow
    public abstract void disconnectFromWorld(Component message);

    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, index = 1)
    private Screen seamless_loading_screen$addWorldTransition(Screen nextScreen) {
        if (!(screen instanceof LevelLoadingScreen) || !ScreenshotLoader.isTransitionActive()) {
            return nextScreen;
        }

        if (nextScreen != null) {
            ScreenshotLoader.finishLoadingScreen();
            return nextScreen;
        }

        return new WorldFadeScreen(Math.max(1, SeamlessLoadingScreenConfig.get().fade));
    }

    @Inject(method = "disconnectFromWorld", at = @At("HEAD"), cancellable = true)
    private void seamless_loading_screen$captureBeforeDisconnect(Component message, CallbackInfo info) {
        if (!PlatformFunctions.hasFastQuit()) return;
        if (seamless_loading_screen$bypassDisconnectCapture || instance.player == null) {
            seamless_loading_screen$bypassDisconnectCapture = false;
            return;
        }

        OnLeaveHelper.beginScreenshotTask(() -> {
            seamless_loading_screen$bypassDisconnectCapture = true;
            this.disconnectFromWorld(message);
        });
        info.cancel();
    }

    //----

    @Inject(method = "stop", at = @At("HEAD"), cancellable = true)
    private void onWindowClose(CallbackInfo info) {
        if (!PlatformFunctions.hasFastQuit()) return;
        if (!seamless_loading_screen$firstOccurrence || instance.player == null) return;

        OnLeaveHelper.beginScreenshotTask(() -> {
            this.seamless_loading_screen$firstOccurrence = false;

            this.stop();
        }, true);

        info.cancel();
    }
}
