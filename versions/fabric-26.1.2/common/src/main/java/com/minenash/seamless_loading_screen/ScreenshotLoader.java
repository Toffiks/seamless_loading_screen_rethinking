package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.SharedConstants;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.awt.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

public class ScreenshotLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern RESERVED_FILENAMES_PATTERN = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", Pattern.CASE_INSENSITIVE);
    public static Identifier SCREENSHOT = Identifier.fromNamespaceAndPath(SeamlessLoadingScreen.MODID, "screenshot");
    public static double imageRatio = 1;
    public static boolean loaded = false;
    public static DisplayMode displayMode = DisplayMode.ENABLED;
    public static boolean replacebg = false;
    private static boolean loadingScreenPending = false;
    private static String fileName = "";

    public static boolean isLoadingScreenPending() {
        return loaded && loadingScreenPending;
    }

    public static boolean isTransitionActive() {
        return loaded && replacebg;
    }

    public static void beginLoadingScreen() {
        replacebg = true;
    }

    public static void finishLoadingScreen() {
        loadingScreenPending = false;
        replacebg = false;
    }

    public static boolean shouldReplaceBackground() {
        return replacebg;
    }

    public static boolean shouldUseBlur() {
        return SeamlessLoadingScreenConfig.get().enableScreenshotBlur && replacebg;
    }

    public static String getFileName() {
        return fileName;
    }

    private static void setFileName(String newFileName) {
        var session = Minecraft.getInstance().getUser();

        var offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getName()).getBytes(StandardCharsets.UTF_8));
        var sessionUUID = session.getProfileId();

        var baseFileDir = "screenshots/worlds/";

        if(SeamlessLoadingScreenConfig.get().saveScreenshotsByUsername) {
            var userDir = (sessionUUID != null && !sessionUUID.equals(offlineUUID))
                    ? cleanFileName(session.getName())
                    : "offline";

            baseFileDir = "screenshots/" + userDir + "/worlds/";
        }

        fileName = baseFileDir + newFileName;
        setScreenshot();
    }

    public static void setScreenshot(String address, int port) {
        setFileName("screenshots/worlds/servers/" + cleanFileName(address) + "_" + port + ".png");
    }

    public static void setScreenshot(String worldName) {
        setFileName("screenshots/worlds/singleplayer/" + worldName + ".png");
    }

    public static void setRealmScreenshot(String realmName) {
        setFileName("screenshots/worlds/realms/" + cleanFileName(realmName) + ".png");
    }

    private static void setScreenshot() {
        loaded = false;
        loadingScreenPending = false;
        replacebg = false;

        if (displayMode == DisplayMode.DISABLED) return;

        try (InputStream in = new FileInputStream(ScreenshotLoader.fileName)) {
            if (PlatformFunctions.isDevEnv()) {
                LOGGER.info("Name: " + ScreenshotLoader.fileName);
            }

            DynamicTexture image = new DynamicTexture(
                    () -> "Seamless Loading Screen screenshot", NativeImage.read(in));
            Minecraft.getInstance().getTextureManager().register(SCREENSHOT, image);
            imageRatio = image.getPixels().getWidth() / (double) image.getPixels().getHeight();
            loaded = true;
            loadingScreenPending = true;
        } catch (FileNotFoundException ignore) {
        } catch (IOException e) {
            LOGGER.error("[SeamlessLoadingScreen]: An Issue has occurred when attempting to set a Screenshot: [name: {}]", ScreenshotLoader.fileName);
            LOGGER.error(String.valueOf(e));
        }
    }

    private static String cleanFileName(String fileName) {
        for (char c : "<>:\"/\\|?*".toCharArray()) fileName = fileName.replace(c, '_');
        fileName = fileName.chars()
                .map(c -> c < 32 ? '_' : c)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

        if (RESERVED_FILENAMES_PATTERN.matcher(fileName).matches()) fileName = "_" + fileName + "_";

        if (fileName.length() > 255 - 4) fileName = fileName.substring(0, 255 - 4);

        return fileName;
    }

    public static void render(Screen screen, GuiGraphicsExtractor context) {
        render(screen, context, 1.0f);
    }

    public static void render(Screen screen, GuiGraphicsExtractor context, float visibility) {
        float alpha = Math.max(0.0f, Math.min(visibility, 1.0f));
        // Smoothstep keeps the beginning and end of the reveal from visibly snapping.
        alpha = alpha * alpha * (3.0f - 2.0f * alpha);

        int w = (int) (imageRatio * screen.height);
        int color = getArgb(Math.round(alpha * 255.0f), 255, 255, 255);
        context.blit(RenderPipelines.GUI_TEXTURED, SCREENSHOT,
                screen.width / 2 - w / 2, 0, 0.0F, 0.0F,
                w, screen.height, w, screen.height, color);

        renderAfterEffects(screen, context, alpha);
    }

    public static void renderAfterEffects(Screen screen, GuiGraphicsExtractor context, float fadeValue) {
        renderTint(screen, context, fadeValue);

        if (SeamlessLoadingScreenConfig.get().enableScreenshotBlur && replacebg) {
            // In 1.21.11 applyBlur marks a boundary between GUI root layers.
            // Everything in the marked layer itself is considered AFTER the blur,
            // so the composed panorama/screenshot must be finalized first.
            context.blurBeforeThisStratum();
        }
    }

    public static void renderTint(Screen screen, GuiGraphicsExtractor context, float fadeValue) {
        Color color = SeamlessLoadingScreenConfig.get().tintColor;

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = Math.round(255 * (SeamlessLoadingScreenConfig.get().tintStrength * fadeValue));

        int argb_color = getArgb(alpha, red, green, blue);

        context.fill(0, 0, screen.width, screen.height, argb_color);
    }

    public static int getArgb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

}

