package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.OnLeaveHelper;
import net.minecraft.client.Minecraft;
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
    @Shadow
    public abstract void stop();

    @Inject(method = "stop", at = @At("HEAD"), cancellable = true)
    private void onWindowClose(CallbackInfo info) {
        if (!seamless_loading_screen$firstOccurrence) return;

        Runnable stopClient = () -> {
            this.seamless_loading_screen$firstOccurrence = false;
            this.stop();
        };

        if (instance.player != null) {
            OnLeaveHelper.beginScreenshotTask(stopClient, true);
            info.cancel();
        } else if (OnLeaveHelper.awaitPendingSaves(stopClient)) {
            info.cancel();
        }
    }
}
