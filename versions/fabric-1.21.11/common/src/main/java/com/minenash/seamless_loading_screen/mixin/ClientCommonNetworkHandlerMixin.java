package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.OnLeaveHelper;
import com.minenash.seamless_loading_screen.PlatformFunctions;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.DisconnectionInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow public abstract void onDisconnected(DisconnectionInfo info);

    @Unique
    private boolean seamless_loading_screen$bypassNextCapture = false;

    @Inject(method = "onDisconnected", at = @At("HEAD"), cancellable = true)
    private void onServerOrderedDisconnect(DisconnectionInfo info, CallbackInfo ci) {
        if (!PlatformFunctions.hasFastQuit()) return;
        if (seamless_loading_screen$bypassNextCapture) {
            seamless_loading_screen$bypassNextCapture = false;
            return;
        }

        OnLeaveHelper.beginScreenshotTask(() -> {
            seamless_loading_screen$bypassNextCapture = true;
            this.onDisconnected(info);
        });
        ci.cancel();
    }
}
