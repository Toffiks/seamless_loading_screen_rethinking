package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ConnectScreenState;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.ServerInfoExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin implements ConnectScreenState {

    @Shadow
    private volatile boolean aborted;

    @Inject(method = "connect", at = @At("HEAD"))
    private void getImage(Minecraft client, ServerAddress address, ServerData info,
                          TransferState transferState, CallbackInfo ci) {
        ScreenshotLoader.setDisplayMode(((ServerInfoExtension) info).getDisplayMode());

        ScreenshotLoader.setScreenshot(address.getHost(), address.getPort());
    }

    @Override
    public boolean seamless_loading_screen$isConnectingCancelled() {
        return aborted;
    }
}
