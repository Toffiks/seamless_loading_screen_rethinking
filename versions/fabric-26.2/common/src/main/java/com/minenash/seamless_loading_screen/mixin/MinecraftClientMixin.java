package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.OnLeaveHelper;
import com.minenash.seamless_loading_screen.PlatformFunctions;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Minecraft.class, priority = 900)
public abstract class MinecraftClientMixin {

    @Shadow
    static Minecraft instance;

    @Unique
    private boolean seamless_loading_screen$firstOccurrence = true;
    @Unique
    private boolean seamless_loading_screen$bypassDisconnectCapture = false;
    @Shadow
    public abstract void stop();
    @Shadow
    public abstract void disconnectFromWorld(Component message);

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
