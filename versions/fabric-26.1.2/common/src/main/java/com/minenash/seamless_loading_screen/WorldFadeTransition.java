package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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

    public static void render(GuiGraphicsExtractor context) {
        if (!active) return;

        float alpha = getTransitionAlpha();
        ScreenshotLoader.render(context, context.guiWidth(), context.guiHeight(), alpha);
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
        var minecraft = Minecraft.getInstance();
        var config = SeamlessLoadingScreenConfig.get();
        if (!config.playSoundEffect) return;

        Identifier soundId = Identifier.tryParse(config.soundEffect);
        if (soundId == null) {
            LOGGER.warn("[SeamlessLoadingScreen] Invalid sound identifier: {}", config.soundEffect);
            return;
        }

        BuiltInRegistries.SOUND_EVENT.getOptional(soundId).ifPresentOrElse(
                sound -> minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(sound, config.soundPitch, config.soundVolume)),
                () -> LOGGER.warn("[SeamlessLoadingScreen] Unknown sound identifier: {}", soundId)
        );
    }
}
