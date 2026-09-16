package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.WorldFadeTransition;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void seamless_loading_screen$renderWorldFade(
            GuiGraphicsExtractor context, DeltaTracker deltaTracker, CallbackInfo ci
    ) {
        WorldFadeTransition.render(context);
    }
}
