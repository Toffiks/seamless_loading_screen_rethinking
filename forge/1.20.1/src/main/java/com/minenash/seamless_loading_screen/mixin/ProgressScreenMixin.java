package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProgressScreen.class)
public abstract class ProgressScreenMixin extends Screen {
    @Shadow
    private Component header;

    protected ProgressScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/ProgressScreen;renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V",
            shift = At.Shift.AFTER))
    private void seamless_loading_screen$renderScreenshotWhileJoiningWorld(
            GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci
    ) {
        if (!ScreenshotLoader.isLoadingScreenPending()) return;

        // Several unrelated 1.20.1 tasks use ProgressScreen. Only the one
        // created by Minecraft.setLevel() is the final "Joining world" stage.
        if (ScreenshotLoader.hasChunkLoadingStarted() || seamless_loading_screen$isJoiningWorld()) {
            ScreenshotLoader.renderFinalLoading(this, context);
        }
    }

    private boolean seamless_loading_screen$isJoiningWorld() {
        return header != null
                && header.getContents() instanceof TranslatableContents contents
                && "connect.joining".equals(contents.getKey());
    }
}
