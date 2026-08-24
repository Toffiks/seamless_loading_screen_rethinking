package com.minenash.seamless_loading_screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * A short, text-free transition from the blurred loading screenshot to the
 * already rendered world underneath it.
 */
public class WorldFadeScreen extends Screen {
    private final int duration;
    private int frames;
    private boolean finished;

    public WorldFadeScreen(int duration) {
        super(Text.empty());
        this.duration = duration;
        this.frames = duration;
    }

    public float getTransitionAlpha() {
        if (frames <= 0) return 0.0f;
        float progress = Math.min(frames / (float) duration, 1.0f);
        return progress * progress * (3.0f - 2.0f * progress);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // The world is already rendered underneath. ScreenshotLoader adds the
        // single correctly positioned blur boundary during render().
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (finished || client == null) return;

        ScreenshotLoader.render(this, context, getTransitionAlpha());

        frames--;
        if (frames <= 0) finish();
    }

    @Override
    public void removed() {
        ScreenshotLoader.finishLoadingScreen();
        super.removed();
    }

    private void finish() {
        if (finished || client == null) return;
        finished = true;
        ScreenshotLoader.finishLoadingScreen();
        if (client.currentScreen == this) client.setScreen(null);
    }
}
