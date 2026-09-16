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

    public static void render(DrawContext context) {
        if (!active) return;

        var client = MinecraftClient.getInstance();
        if (client.currentScreen != null || client.world == null || client.player == null) return;

        if (soundPending) {
            soundPending = false;
            playConfiguredSound();
        }

        float transitionAlpha = ANIMATION.valueForRender(System.nanoTime());
        ScreenshotLoader.renderWorldFade(context, context.getScaledWindowWidth(),
                context.getScaledWindowHeight(), transitionAlpha);
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
