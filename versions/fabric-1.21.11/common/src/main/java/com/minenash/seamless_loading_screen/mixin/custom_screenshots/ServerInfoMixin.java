package com.minenash.seamless_loading_screen.mixin.custom_screenshots;

import com.minenash.seamless_loading_screen.DisplayMode;
import com.minenash.seamless_loading_screen.ServerInfoExtension;
import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(ServerInfo.class)
public abstract class ServerInfoMixin implements ServerInfoExtension {
    @Unique private static final String DISPLAY_MODE_KEY = "seamless_loading_screen:screenshot_display_mode";
    @Unique private static final String LEGACY_DISPLAY_MODE_KEY = "screenshotDisplayMode";

    @Shadow
    public String address;
    @Unique
    private DisplayMode seamless_loading_screen$displayMode = SeamlessLoadingScreenConfig.get().defaultServerMode;

    @Inject(method = "fromNbt", at = @At("RETURN"))
    private static void deserialize(NbtCompound tag, CallbackInfoReturnable<ServerInfo> callback) {
        String key = tag.contains(DISPLAY_MODE_KEY) ? DISPLAY_MODE_KEY : LEGACY_DISPLAY_MODE_KEY;
        if (!tag.contains(key)) return;

        tag.getString(key).ifPresent(value -> {
            try {
                ((ServerInfoExtension) callback.getReturnValue()).setDisplayMode(DisplayMode.valueOf(value));
            } catch (IllegalArgumentException ignored) {
            }
        });
    }

    @Inject(method = "toNbt", at = @At("RETURN"))
    private void serialize(CallbackInfoReturnable<NbtCompound> callback) {
        callback.getReturnValue().putString(DISPLAY_MODE_KEY, seamless_loading_screen$displayMode.name());
    }

    @Inject(method = "copyFrom", at = @At("TAIL"))
    private void copyFrom(ServerInfo info, CallbackInfo callback) {
        seamless_loading_screen$displayMode = ((ServerInfoMixin) (Object) info).seamless_loading_screen$displayMode;
    }

    @Override
    public DisplayMode getDisplayMode() {
        var blacklistedAddresses = SeamlessLoadingScreenConfig.get().blacklistedAddresses;
        if (blacklistedAddresses != null) {
            for (String blacklistedAddress : blacklistedAddresses) {
                if (seamless_loading_screen$isBlockedAddress(this.address, blacklistedAddress)) return DisplayMode.DISABLED;
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
