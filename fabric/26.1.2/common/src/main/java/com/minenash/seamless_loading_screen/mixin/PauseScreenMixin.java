package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotCapture;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {

    @ModifyArg(
            method = "createPauseMenu",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;", ordinal = 0),
            index = 1,
            require = 0
    )
    private Button.OnPress seamless_loading_screen$captureBeforeExit(Button.OnPress onPress) {
        return button -> ScreenshotCapture.begin(() -> onPress.onPress(button));
    }
}
