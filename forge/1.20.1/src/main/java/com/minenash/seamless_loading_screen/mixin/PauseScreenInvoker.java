package com.minenash.seamless_loading_screen.mixin;

import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PauseScreen.class)
public interface PauseScreenInvoker {
    @Invoker("onDisconnect")
    void seamless_loading_screen$invokeOnDisconnect();
}
