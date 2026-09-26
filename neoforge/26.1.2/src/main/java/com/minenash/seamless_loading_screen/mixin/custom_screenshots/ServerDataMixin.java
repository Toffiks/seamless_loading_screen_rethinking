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

import java.util.Locale;

@Mixin(ServerData.class)
public abstract class ServerDataMixin implements ServerInfoExtension {
    @Unique private static final String DISPLAY_MODE_KEY = "seamless_loading_screen:screenshot_display_mode";
    @Unique private static final String LEGACY_DISPLAY_MODE_KEY = "screenshotDisplayMode";
    @Shadow public String ip;

    @Unique
    private DisplayMode seamless_loading_screen$displayMode = SeamlessLoadingScreenConfig.get().defaultServerMode;

    @Inject(method = "read", at = @At("RETURN"))
    private static void seamless_loading_screen$read(CompoundTag tag,
                                                       CallbackInfoReturnable<ServerData> cir) {
        String value = tag.getStringOr(DISPLAY_MODE_KEY,
                tag.getStringOr(LEGACY_DISPLAY_MODE_KEY, ""));
        if (value.isEmpty()) return;
        try {
            ((ServerInfoExtension) cir.getReturnValue()).setDisplayMode(DisplayMode.valueOf(value));
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void seamless_loading_screen$write(CallbackInfoReturnable<CompoundTag> cir) {
        cir.getReturnValue().putString(DISPLAY_MODE_KEY, seamless_loading_screen$displayMode.name());
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void seamless_loading_screen$copyFrom(ServerData other, CallbackInfo ci) {
        seamless_loading_screen$displayMode = ((ServerDataMixin) (Object) other).seamless_loading_screen$displayMode;
    }

    @Override
    public DisplayMode getDisplayMode() {
        var blockedAddresses = SeamlessLoadingScreenConfig.get().blacklistedAddresses;
        if (blockedAddresses != null) {
            for (String blocked : blockedAddresses) {
                if (seamless_loading_screen$isBlockedAddress(ip, blocked)) return DisplayMode.DISABLED;
            }
        }
        return seamless_loading_screen$displayMode;
    }

    @Override
    public void setDisplayMode(DisplayMode mode) {
        seamless_loading_screen$displayMode = mode == null ? DisplayMode.DISABLED : mode;
    }

    @Unique
    private static boolean seamless_loading_screen$isBlockedAddress(String address, String blockedAddress) {
        String host = seamless_loading_screen$normalizeHost(address);
        String blockedHost = seamless_loading_screen$normalizeHost(blockedAddress);
        return !host.isEmpty() && !blockedHost.isEmpty()
                && (host.equals(blockedHost) || host.endsWith("." + blockedHost));
    }

    @Unique
    private static String seamless_loading_screen$normalizeHost(String address) {
        if (address == null || address.isBlank()) return "";
        String value = address.strip().toLowerCase(Locale.ROOT);

        if (value.startsWith("[")) {
            int closingBracket = value.indexOf(']');
            if (closingBracket > 1) return value.substring(1, closingBracket);
        }

        int firstColon = value.indexOf(':');
        int lastColon = value.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon
                && value.substring(firstColon + 1).chars().allMatch(Character::isDigit)) {
            value = value.substring(0, firstColon);
        }
        while (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
