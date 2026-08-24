package com.minenash.seamless_loading_screen.mixin.custom_screenshots;

import com.minenash.seamless_loading_screen.ServerInfoExtension;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ManageServerScreen.class)
public abstract class ManageServerScreenMixin extends Screen {
    @Shadow @Final private ServerData serverData;
    @Unique private Button seamless_loading_screen$displayModeButton;

    protected ManageServerScreenMixin(Component title) {
        super(title);
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/Button$Builder;bounds(IIII)Lnet/minecraft/client/gui/components/Button$Builder;"),
            index = 1)
    private int seamless_loading_screen$moveBottomButtons(int y) {
        return y + 24;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void seamless_loading_screen$addDisplayModeButton(CallbackInfo ci) {
        seamless_loading_screen$displayModeButton = addRenderableWidget(Button.builder(displayModeText(), button -> {
            ServerInfoExtension extension = (ServerInfoExtension) serverData;
            extension.setDisplayMode(extension.getDisplayMode().next());
            seamless_loading_screen$displayModeButton.setMessage(displayModeText());
        }).bounds(width / 2 - 100, height / 4 + 96 + 18, 200, 20).build());
    }

    @Unique
    private Component displayModeText() {
        return Component.translatable("seamless_loading_screen.server.displayMode")
                .append(": ")
                .append(Component.translatable("seamless_loading_screen.config.displayMode."
                        + ((ServerInfoExtension) serverData).getDisplayMode().name().toLowerCase()));
    }
}
