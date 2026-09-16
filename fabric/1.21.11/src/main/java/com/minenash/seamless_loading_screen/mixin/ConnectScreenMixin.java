package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ConnectScreenState;
import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.minenash.seamless_loading_screen.ServerInfoExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin implements ConnectScreenState {

    @Shadow
    volatile boolean connectingCancelled;

    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V", at = @At("HEAD"))
    private void seamless_loading_screen$setServerScreenshot(MinecraftClient client, ServerAddress address,
                                                               ServerInfo info, CookieStorage cookieStorage,
                                                               CallbackInfo ci) {
        ScreenshotLoader.setDisplayMode(((ServerInfoExtension) info).getDisplayMode());

        ScreenshotLoader.setScreenshot(address.getAddress(), address.getPort());
    }

    @Override
    public boolean seamless_loading_screen$isConnectingCancelled() {
        return connectingCancelled;
    }
}
