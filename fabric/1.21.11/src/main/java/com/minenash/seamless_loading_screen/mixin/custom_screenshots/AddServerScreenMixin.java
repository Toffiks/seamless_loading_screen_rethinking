package com.minenash.seamless_loading_screen.mixin.custom_screenshots;

import com.minenash.seamless_loading_screen.ServerInfoExtension;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(AddServerScreen.class)
public abstract class AddServerScreenMixin extends Screen {

    @Shadow
    @Final
    private ServerInfo server;

    @Unique
    private ButtonWidget seamless_loading_screen$buttonDisplayMode;

    protected AddServerScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), require = 0)
    private void buttonAllowCustomScreenshot(CallbackInfo info) {
        seamless_loading_screen$buttonDisplayMode = addDrawableChild(ButtonWidget.builder(getText(), buttonWidget -> {
            ((ServerInfoExtension) server).setDisplayMode(((ServerInfoExtension) server).getDisplayMode().next());
            seamless_loading_screen$buttonDisplayMode.setMessage(getText());
        }).dimensions(width / 2 - 100, height / 4 + 94, 200, 18).build());
    }

    @Unique
    private Text getText() {
        return Text.translatable("seamless_loading_screen.server.displayMode")
                .append(": ")
                .append(Text.translatable(
                        "seamless_loading_screen.config.displayMode."
                                + ((ServerInfoExtension) server).getDisplayMode().name().toLowerCase(Locale.ROOT)));
    }

}
