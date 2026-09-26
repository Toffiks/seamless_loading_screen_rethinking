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

    private static final FadeOutAnimation ANIMATION = new FadeOutAnimation();
    private static boolean active;
    private static boolean soundPending;

    private WorldFadeTransition() {}

    public static void start(int durationTicks) {
        if (active) return;
        int safeDurationTicks = Math.max(1,
                Math.min(durationTicks, SeamlessLoadingScreenConfig.MAX_FADE_TICKS));
        ANIMATION.start(safeDurationTicks * NANOS_PER_TICK,
                ScreenshotLoader.finishRevealForWorldFade());
        ChunkMapFade.start();
        active = true;
        soundPending = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getTransitionAlpha() {
        return active ? ANIMATION.currentValue(System.nanoTime()) : 0.0f;
    }

    public static void render(GuiGraphicsExtractor context) {
        if (!active) return;

        var minecraft = Minecraft.getInstance();
        if (minecraft.screen != null || minecraft.getOverlay() != null
                || minecraft.level == null || minecraft.player == null) return;

        if (soundPending) {
            soundPending = false;
            playConfiguredSound();
        }

        float transitionAlpha = ANIMATION.valueForRender(System.nanoTime());
        ScreenshotLoader.renderWorldFade(context, context.guiWidth(), context.guiHeight(), transitionAlpha);
        ChunkMapFade.render(context);
        if (transitionAlpha <= 0.0f) finish();
    }

    public static void clientTick() {
        if (active && ANIMATION.isFinished(System.nanoTime())) finish();
    }

    public static void finish() {
        active = false;
        soundPending = false;
        ChunkMapFade.clear();
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
