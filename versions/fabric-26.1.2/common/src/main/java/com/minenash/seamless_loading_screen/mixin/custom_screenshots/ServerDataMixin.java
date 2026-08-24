package com.minenash.seamless_loading_screen.mixin.custom_screenshots;

import com.minenash.seamless_loading_screen.DisplayMode;
import com.minenash.seamless_loading_screen.ServerInfoExtension;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerData.class)
public abstract class ServerDataMixin implements ServerInfoExtension {
    @Shadow public String ip;

    @Unique
    private DisplayMode seamless_loading_screen$displayMode = DisplayMode.ENABLED;

    @Inject(method = "read", at = @At("RETURN"))
    private static void seamless_loading_screen$read(CompoundTag tag,
                                                       CallbackInfoReturnable<ServerData> cir) {
        String value = tag.getStringOr("screenshotDisplayMode", "");
        if (value.isEmpty()) return;
        try {
            ((ServerInfoExtension) cir.getReturnValue()).setDisplayMode(DisplayMode.valueOf(value));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void seamless_loading_screen$write(CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putString("screenshotDisplayMode", seamless_loading_screen$displayMode.name());
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void seamless_loading_screen$copyFrom(ServerData other, CallbackInfo ci) {
        seamless_loading_screen$displayMode = ((ServerInfoExtension) other).getDisplayMode();
    }

    @Override
    public DisplayMode getDisplayMode() {
        for (String blocked : SeamlessLoadingScreenConfig.get().blacklistedAddresses) {
            if (ip.contains(blocked)) return DisplayMode.DISABLED;
        }
        return seamless_loading_screen$displayMode;
    }

    @Override
    public void setDisplayMode(DisplayMode mode) {
        seamless_loading_screen$displayMode = mode;
    }
}

