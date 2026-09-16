package com.minenash.seamless_loading_screen.mixin.realm;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RealmsMainScreen.class)
public abstract class RealmsMainScreenMixin {

    @Inject(method = "play(Lcom/mojang/realmsclient/dto/RealmsServer;Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("HEAD"))
    private static void getRealmNameID(RealmsServer realmsServer, Screen parent, CallbackInfo info) {
        if (realmsServer == null) return;
        String name = realmsServer.name == null || realmsServer.name.isBlank()
                ? "realm_" + realmsServer.id
                : realmsServer.name;
        ScreenshotLoader.setRealmScreenshot(realmsServer.id, name);
    }

}
