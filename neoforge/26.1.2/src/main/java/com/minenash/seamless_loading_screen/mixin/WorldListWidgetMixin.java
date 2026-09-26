package com.minenash.seamless_loading_screen.mixin;

import com.minenash.seamless_loading_screen.ScreenshotLoader;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldSelectionList.WorldListEntry.class)
public abstract class WorldListWidgetMixin {

    @Shadow
    @Final
    LevelSummary summary;

    @Inject(method = "joinWorld", at = @At("HEAD"))
    private void seamless_loading_screen$setFilename(CallbackInfo info) {
        ScreenshotLoader.setScreenshot(summary.getLevelId());
    }

}
