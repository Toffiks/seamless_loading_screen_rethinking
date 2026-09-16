package com.minenash.seamless_loading_screen.mixin.custom_screenshots;

import com.minenash.seamless_loading_screen.ServerInfoExtension;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.EditServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(EditServerScreen.class)
public abstract class EditServerScreenMixin extends Screen {
    @Shadow @Final private ServerData serverData;
    @Unique private Button seamless_loading_screen$displayModeButton;

    protected EditServerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void seamless_loading_screen$addDisplayModeButton(CallbackInfo ci) {
        seamless_loading_screen$displayModeButton = addRenderableWidget(Button.builder(displayModeText(), button -> {
            ServerInfoExtension extension = (ServerInfoExtension) serverData;
            extension.setDisplayMode(extension.getDisplayMode().next());
            seamless_loading_screen$displayModeButton.setMessage(displayModeText());
        }).bounds(width / 2 - 100, height / 4 + 94, 200, 18).build());
    }

    @Unique
    private Component displayModeText() {
        return Component.translatable("seamless_loading_screen.server.displayMode")
                .append(": ")
                .append(Component.translatable("seamless_loading_screen.config.displayMode."
                        + ((ServerInfoExtension) serverData).getDisplayMode().name().toLowerCase(Locale.ROOT)));
    }
}
