package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;

/**
 * Renders the final screenshot fade as a HUD overlay. It deliberately does not
 * use a Screen, so player input is available while the transition is visible.
 */
public final class WorldFadeTransition {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long NANOS_PER_TICK = 50_000_000L;

    private static long durationNanos;
    private static long startTimeNanos;
    private static boolean active;

    private WorldFadeTransition() {}

    public static void start(int durationTicks) {
        int safeDurationTicks = Math.max(1,
                Math.min(durationTicks, SeamlessLoadingScreenConfig.MAX_FADE_TICKS));
        durationNanos = safeDurationTicks * NANOS_PER_TICK;
        startTimeNanos = System.nanoTime();
        active = true;
        playConfiguredSound();
    }

    public static boolean isActive() {
        return active;
    }

    public static float getTransitionAlpha() {
        if (!active) return 0.0f;
        long elapsedNanos = Math.max(0L, System.nanoTime() - startTimeNanos);
        float progress = Math.max(0.0f, 1.0f - elapsedNanos / (float) durationNanos);
        return progress * progress * (3.0f - 2.0f * progress);
    }

    public static void render(DrawContext context) {
        if (!active) return;

        float alpha = getTransitionAlpha();
        ScreenshotLoader.render(context, context.getScaledWindowWidth(),
                context.getScaledWindowHeight(), alpha);
        if (alpha <= 0.0f) finish();
    }

    public static void clientTick() {
        if (active && getTransitionAlpha() <= 0.0f) finish();
    }

    public static void finish() {
        active = false;
        ScreenshotLoader.finishLoadingScreen();
    }

    private static void playConfiguredSound() {
        var client = MinecraftClient.getInstance();
        var config = SeamlessLoadingScreenConfig.get();
        if (!config.playSoundEffect) return;

        Identifier id = Identifier.tryParse(config.soundEffect);
        if (id == null) {
            LOGGER.warn("[SeamlessLoadingScreen] Invalid configured sound identifier: {}",
                    config.soundEffect);
            return;
        }

        Registries.SOUND_EVENT.getOptionalValue(id).ifPresentOrElse(
                sound -> client.getSoundManager().play(PositionedSoundInstance.ui(
                        sound, config.soundPitch, config.soundVolume)),
                () -> LOGGER.warn("[SeamlessLoadingScreen] Configured sound does not exist: {}", id)
        );
    }
}
