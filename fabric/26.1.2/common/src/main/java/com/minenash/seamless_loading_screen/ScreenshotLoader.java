package com.minenash.seamless_loading_screen;

import com.minenash.seamless_loading_screen.config.SeamlessLoadingScreenConfig;
import com.mojang.logging.LogUtils;
import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public class ScreenshotLoader {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SHARED_WORLD_SCREENSHOTS_DIRECTORY = "screenshots/worlds/";
    private static final Pattern RESERVED_FILENAMES_PATTERN = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", Pattern.CASE_INSENSITIVE);
    private static final Identifier SCREENSHOT_TEXTURE = Identifier.fromNamespaceAndPath(SeamlessLoadingScreen.MODID, "screenshot");
    private static final LoadingScreenLifecycle LIFECYCLE = new LoadingScreenLifecycle();
    private static final ScreenshotRevealController REVEAL =
            new ScreenshotRevealController(250_000_000L);
    private static double imageAspectRatio = 1;
    private static DisplayMode displayMode = DisplayMode.ENABLED;
    private static String fileName = "";
    private static String targetRelativeFileName = "";
    private static List<String> fallbackRelativeFileNames = List.of();
    private static List<String> legacyFileNames = List.of();
    private static boolean textureLoaded;
    private static boolean releasePending;
    private static boolean worldScreenshotDeferred;

    public static boolean isLoadingScreenPending() {
        return LIFECYCLE.isPending();
    }

    public static boolean isTransitionActive() {
        return LIFECYCLE.isActive();
    }

    public static boolean isTransitionReady() {
        return LIFECYCLE.isReady();
    }

    public static void beginLoadingScreen() {
        LIFECYCLE.begin();
    }

    public static void finishLoadingScreen() {
        LIFECYCLE.finish();
        REVEAL.reset();
        worldScreenshotDeferred = false;
        if (textureLoaded) releasePending = true;
    }

    public static void cancelPendingLoadingScreen() {
        worldScreenshotDeferred = false;
        if (!LIFECYCLE.cancelReady()) return;
        REVEAL.reset();
        if (textureLoaded) releasePending = true;
    }

    public static void clientTick() {
        if (releasePending && !LIFECYCLE.isPending()) releaseScreenshotTexture();
    }

    public static boolean shouldUseBlur() {
        return SeamlessLoadingScreenConfig.get().enableScreenshotBlur && isTransitionActive();
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) {
        displayMode = mode == null ? DisplayMode.ENABLED : mode;
    }

    public static String getFileName() {
        refreshFilePaths();
        return fileName;
    }

    private static void setFileName(String relativeFileName, String... legacyRelativeFileNames) {
        targetRelativeFileName = relativeFileName;
        fallbackRelativeFileNames = List.of(legacyRelativeFileNames);
        refreshFilePaths();
        worldScreenshotDeferred = false;
        setScreenshot();
    }

    private static void deferWorldScreenshot(String relativeFileName) {
        targetRelativeFileName = relativeFileName;
        fallbackRelativeFileNames = List.of();
        refreshFilePaths();
        LIFECYCLE.finish();
        REVEAL.reset();
        releaseScreenshotTexture();
        worldScreenshotDeferred = true;
    }

    public static void prepareDeferredWorldScreenshot() {
        if (!worldScreenshotDeferred) return;
        worldScreenshotDeferred = false;
        setScreenshot();
    }

    private static void refreshFilePaths() {
        if (targetRelativeFileName.isBlank()) {
            fileName = "";
            legacyFileNames = List.of();
            return;
        }

        var session = Minecraft.getInstance().getUser();

        var offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + session.getName()).getBytes(StandardCharsets.UTF_8));
        var sessionUUID = session.getProfileId();

        var baseFileDir = SHARED_WORLD_SCREENSHOTS_DIRECTORY;

        if (SeamlessLoadingScreenConfig.get().saveScreenshotsByUsername) {
            var userDir = (sessionUUID != null && !sessionUUID.equals(offlineUUID))
                    ? cleanFileName(session.getName())
                    : "offline-" + cleanFileName(session.getName());

            baseFileDir = "screenshots/" + userDir + "/worlds/";
        }

        fileName = baseFileDir + targetRelativeFileName;

        var candidates = new ArrayList<String>();
        addLegacyCandidate(candidates, SHARED_WORLD_SCREENSHOTS_DIRECTORY + targetRelativeFileName);
        for (String relativeFallback : fallbackRelativeFileNames) {
            addLegacyCandidate(candidates, baseFileDir + relativeFallback);
            addLegacyCandidate(candidates, SHARED_WORLD_SCREENSHOTS_DIRECTORY + relativeFallback);
        }
        legacyFileNames = List.copyOf(candidates);
    }

    private static void addLegacyCandidate(List<String> candidates, String candidate) {
        if (!candidate.equals(fileName) && !candidates.contains(candidate)) candidates.add(candidate);
    }

    public static void setScreenshot(String address, int port) {
        setFileName("servers/" + ScreenshotPathPolicy.pngFileName(
                cleanFileName(address), "_" + port));
    }

    public static void setScreenshot(String worldName) {
        setDisplayMode(DisplayMode.ENABLED);
        deferWorldScreenshot("singleplayer/" + ScreenshotPathPolicy.pngFileName(
                cleanFileName(worldName), ""));
    }

    public static void setRealmScreenshot(long realmId, String realmName) {
        setDisplayMode(DisplayMode.ENABLED);
        String cleanName = cleanFileName(realmName);
        setFileName(ScreenshotPathPolicy.realm(realmId),
                "realms/" + ScreenshotPathPolicy.pngFileName(
                        "realm_" + realmId + "_" + cleanName, ""),
                "realms/" + ScreenshotPathPolicy.pngFileName(cleanName, ""));
    }

    private static void setScreenshot() {
        LIFECYCLE.finish();
        REVEAL.reset();
        releaseScreenshotTexture();

        if (displayMode == DisplayMode.DISABLED) return;

        NativeImage decodedImage = null;
        DynamicTexture texture = null;
        try (InputStream in = openScreenshot()) {
            decodedImage = NativeImage.read(in);
            if (decodedImage.getWidth() <= 0 || decodedImage.getHeight() <= 0) {
                throw new IOException("Screenshot has invalid dimensions");
            }
            int width = decodedImage.getWidth();
            int height = decodedImage.getHeight();

            texture = new DynamicTexture(
                    () -> "Seamless Loading Screen screenshot", decodedImage);
            decodedImage = null; // ownership moved to the texture
            Minecraft.getInstance().getTextureManager().register(SCREENSHOT_TEXTURE, texture);
            texture = null; // ownership moved to the texture manager
            textureLoaded = true;
            imageAspectRatio = width / (double) height;
            LIFECYCLE.prepare();
        } catch (FileNotFoundException ignore) {
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            LOGGER.error("[SeamlessLoadingScreen] Unable to load screenshot: {}", fileName, e);
        } finally {
            if (texture != null) texture.close();
            else if (decodedImage != null) decodedImage.close();
        }
    }

    private static InputStream openScreenshot() throws FileNotFoundException {
        try {
            return new FileInputStream(FabricLoader.getInstance().getGameDir().resolve(fileName).toFile());
        } catch (FileNotFoundException currentMissing) {
            for (String legacyFileName : legacyFileNames) {
                try {
                    return new FileInputStream(FabricLoader.getInstance().getGameDir().resolve(legacyFileName).toFile());
                } catch (FileNotFoundException ignored) {
                }
            }
            throw currentMissing;
        }
    }

    private static void releaseScreenshotTexture() {
        releasePending = false;
        if (!textureLoaded) return;
        Minecraft.getInstance().getTextureManager().release(SCREENSHOT_TEXTURE);
        textureLoaded = false;
    }

    private static String cleanFileName(String fileName) {
        if (fileName == null) return "_";
        for (char c : "<>:\"/\\|?*".toCharArray()) fileName = fileName.replace(c, '_');
        fileName = fileName.chars()
                .map(c -> c < 32 ? '_' : c)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .stripTrailing();

        if (fileName.isEmpty()) fileName = "_";

        if (RESERVED_FILENAMES_PATTERN.matcher(fileName).matches()) fileName = "_" + fileName + "_";

        fileName = ScreenshotPathPolicy.truncatePreservingSurrogates(fileName, 255 - 4);

        return fileName;
    }

    public static void renderLoadingBackground(Screen screen, GuiGraphicsExtractor context,
                                               float targetVisibility) {
        var mode = SeamlessLoadingScreenConfig.get().screenshotRevealMode;
        float visibility = REVEAL.duringChunkLoading(mode, targetVisibility, System.nanoTime());
        render(context, screen.width, screen.height, visibility, false);
    }

    public static void renderFinalLoading(Screen screen, GuiGraphicsExtractor context) {
        beginLoadingScreen();
        render(context, screen.width, screen.height, REVEAL.finishLoading(), false);
    }

    public static float finishRevealForWorldFade() {
        return REVEAL.finishLoading();
    }

    public static void renderWorldFade(GuiGraphicsExtractor context, int screenWidth,
                                       int screenHeight, float visibility) {
        render(context, screenWidth, screenHeight, visibility, true);
    }

    private static void render(GuiGraphicsExtractor context, int screenWidth, int screenHeight,
                               float visibility, boolean applyWorldFadeBlur) {
        float alpha = Math.max(0.0f, Math.min(visibility, 1.0f));

        double screenAspectRatio = screenWidth / (double) Math.max(1, screenHeight);
        int w = imageAspectRatio >= screenAspectRatio
                ? (int) Math.ceil(imageAspectRatio * screenHeight)
                : screenWidth;
        int h = imageAspectRatio >= screenAspectRatio
                ? screenHeight
                : (int) Math.ceil(screenWidth / imageAspectRatio);
        int color = getArgb(Math.round(alpha * 255.0f), 255, 255, 255);
        context.blit(RenderPipelines.GUI_TEXTURED, SCREENSHOT_TEXTURE,
                screenWidth / 2 - w / 2, screenHeight / 2 - h / 2, 0.0F, 0.0F,
                w, h, w, h, color);

        renderAfterEffects(context, screenWidth, screenHeight, alpha, applyWorldFadeBlur);
    }

    private static void renderAfterEffects(GuiGraphicsExtractor context, int screenWidth,
                                           int screenHeight, float fadeValue,
                                           boolean applyWorldFadeBlur) {
        renderTint(context, screenWidth, screenHeight, fadeValue);

        if (applyWorldFadeBlur && shouldUseBlur()) {
            // Loading screens use Minecraft's own blur path. The world fade is
            // drawn by the HUD and therefore still needs its own blur boundary.
            context.nextStratum();
            context.blurBeforeThisStratum();
        }
        if (applyWorldFadeBlur) {
            int shadeAlpha = Math.round(64.0f * fadeValue);
            context.fill(0, 0, screenWidth, screenHeight, shadeAlpha << 24);
        }
    }

    private static void renderTint(GuiGraphicsExtractor context, int screenWidth, int screenHeight, float fadeValue) {
        var config = SeamlessLoadingScreenConfig.get();
        Color color = config.tintColor;

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int alpha = Math.round(255 * (config.tintStrength * fadeValue));

        int argb_color = getArgb(alpha, red, green, blue);

        context.fill(0, 0, screenWidth, screenHeight, argb_color);
    }

    private static int getArgb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

}
