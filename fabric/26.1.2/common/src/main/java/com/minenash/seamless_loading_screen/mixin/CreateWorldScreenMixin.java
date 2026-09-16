package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Shadow
    @Final
    WorldCreationUiState uiState;

    @Inject(method = "createNewWorld", at = @At("HEAD"))
    private void setFilename(CallbackInfoReturnable<Boolean> ci) {
        ScreenshotLoader.setScreenshot(uiState.getTargetFolder());
    }
}

