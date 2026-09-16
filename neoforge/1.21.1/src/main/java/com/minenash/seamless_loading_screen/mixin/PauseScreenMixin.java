package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotCapture;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @Unique
    private boolean seamless_loading_screen$resumingDisconnect;

    @Invoker("onDisconnect")
    protected abstract void seamless_loading_screen$invokeOnDisconnect();

    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void seamless_loading_screen$captureBeforeExit(CallbackInfo ci) {
        if (seamless_loading_screen$resumingDisconnect) {
            seamless_loading_screen$resumingDisconnect = false;
            return;
        }

        ScreenshotCapture.begin(() -> {
            seamless_loading_screen$resumingDisconnect = true;
            seamless_loading_screen$invokeOnDisconnect();
        });
        ci.cancel();
    }
}
