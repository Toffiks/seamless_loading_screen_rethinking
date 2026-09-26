package com.minenash.seamless_loading_screen.config;

import com.minenash.seamless_loading_screen.DisplayMode;
import com.minenash.seamless_loading_screen.SeamlessLoadingScreen;
import net.fabricmc.loader.api.FabricLoader;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionEventListener;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public class SeamlessLoadingScreenConfig {
    public static final int MAX_FADE_TICKS = 1200;
    public static final int MIN_ARCHIVE_LIMIT = 1;
    public static final int MAX_ARCHIVE_LIMIT = 10_000;
    private static final SafeColorTypeAdapter COLOR_ADAPTER = new SafeColorTypeAdapter(() -> getDefaults().tintColor);
    private static final ConfigClassHandler<SeamlessLoadingScreenConfig> CONFIG_CLASS_HANDLER = ConfigClassHandler
            .createBuilder(SeamlessLoadingScreenConfig.class)
            .id(Identifier.fromNamespaceAndPath(SeamlessLoadingScreen.MODID, "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .appendGsonBuilder(builder -> builder.setPrettyPrinting()
                            .disableHtmlEscaping()
                            .serializeNulls()
                            .registerTypeHierarchyAdapter(Color.class, COLOR_ADAPTER))
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("seamless_loading_screen.json"))
                    .build()
            )
            .build();

    @SerialEntry
    public int fade = 20;
    @SerialEntry
    public ScreenshotRevealMode screenshotRevealMode = ScreenshotRevealMode.New;
    @SerialEntry
    public Color tintColor = new Color(0x212121);
    @SerialEntry
    public float tintStrength = 0.3f;
    @SerialEntry
    public boolean enableScreenshotBlur = true;
    @SerialEntry
    public boolean playSoundEffect = false;
    @SerialEntry
    public String soundEffect = "minecraft:ui.toast.out";
    @SerialEntry
    public float soundPitch = 1f;
    @SerialEntry
    public float soundVolume = 1f;
    @SerialEntry
    public ScreenshotResolution resolution = ScreenshotResolution.Native;
    @SerialEntry
    public boolean archiveScreenshots = false;
    @SerialEntry
    public int archiveLimit = 100;
    @SerialEntry
    public boolean updateWorldIcon = true;
    @SerialEntry
    public List<String> blacklistedAddresses = List.of("play.wynncraft.com");
    @SerialEntry
    public boolean saveScreenshotsByUsername = false;
    @SerialEntry
    public DisplayMode defaultServerMode = DisplayMode.DISABLED;

    private static SeamlessLoadingScreenConfig getDefaults() {
        return CONFIG_CLASS_HANDLER.defaults();
    }

    public static SeamlessLoadingScreenConfig get() {
        return CONFIG_CLASS_HANDLER.instance();
    }

    public static void load() {
        CONFIG_CLASS_HANDLER.load();

        var config = get();
        var defaults = getDefaults();
        boolean repaired = COLOR_ADAPTER.errored();

        if (config.tintColor == null) {
            config.tintColor = defaults.tintColor;
            repaired = true;
        }
        if (config.soundEffect == null || config.soundEffect.isBlank()) {
            config.soundEffect = defaults.soundEffect;
            repaired = true;
        }
        if (config.resolution == null) {
            config.resolution = defaults.resolution;
            repaired = true;
        }
        if (config.screenshotRevealMode == null) {
            config.screenshotRevealMode = defaults.screenshotRevealMode;
            repaired = true;
        }
        if (config.blacklistedAddresses == null) {
            config.blacklistedAddresses = defaults.blacklistedAddresses;
            repaired = true;
        }
        if (config.defaultServerMode == null) {
            config.defaultServerMode = defaults.defaultServerMode;
            repaired = true;
        }
        if (config.fade < 1 || config.fade > MAX_FADE_TICKS) {
            config.fade = defaults.fade;
            repaired = true;
        }
        if (config.archiveLimit < MIN_ARCHIVE_LIMIT || config.archiveLimit > MAX_ARCHIVE_LIMIT) {
            config.archiveLimit = defaults.archiveLimit;
            repaired = true;
        }
        if (!Float.isFinite(config.tintStrength) || config.tintStrength < 0f || config.tintStrength > 1f) {
            config.tintStrength = defaults.tintStrength;
            repaired = true;
        }
        if (!Float.isFinite(config.soundPitch) || config.soundPitch < 0f || config.soundPitch > 10f) {
            config.soundPitch = defaults.soundPitch;
            repaired = true;
        }
        if (!Float.isFinite(config.soundVolume) || config.soundVolume < 0f || config.soundVolume > 10f) {
            config.soundVolume = defaults.soundVolume;
            repaired = true;
        }

        if (repaired) CONFIG_CLASS_HANDLER.save();
    }

    private static Component getName(String id) {
        return Component.translatable("seamless_loading_screen.config." + id);
    }

    private static Component getDesc(String id) {
        return Component.translatable("seamless_loading_screen.config." + id + ".description");
    }
    public static YetAnotherConfigLib getInstance() {
        return YetAnotherConfigLib.create(CONFIG_CLASS_HANDLER,
                (defaults, config, builder) -> {

                    var fadeOpt = Option.<Integer>createBuilder()
                            .name(getName("fade"))
                            .description(OptionDescription.createBuilder()
                                    .text(getDesc("fade"))
                                    .build())
                            .binding(defaults.fade, () -> config.fade, (val) -> config.fade = val)
                            .controller(opt -> IntegerFieldControllerBuilder.create(opt).min(1).max(MAX_FADE_TICKS))
                            .build();

                    var defaultServerModeOpt = Option.<DisplayMode>createBuilder()
                            .name(getName("serverDisplayMode"))
                            .description(OptionDescription.createBuilder().text(getDesc("serverDisplayMode")).build())
                            .binding(defaults.defaultServerMode, () -> config.defaultServerMode, (val) -> config.defaultServerMode = val)
                            .controller(opt -> EnumControllerBuilder.create(opt)
                                    .enumClass(DisplayMode.class)
                                    .formatValue(val -> Component.translatable("seamless_loading_screen.config.displayMode." + val.name().toLowerCase(Locale.ROOT)))
                            ).build();

                    var soundOpt = Option.<String>createBuilder()
                            .name(getName("soundEffect"))
                            .description(OptionDescription.createBuilder().text(getDesc("soundEffect")).build())
                            .binding(defaults.soundEffect, () -> config.soundEffect, (val) -> config.soundEffect = val)
                            .controller(StringControllerBuilder::create)
                            .build();

                    var soundPitchOpt = Option.<Float>createBuilder()
                            .name(getName("soundPitch"))
                            .description(OptionDescription.createBuilder().text(getDesc("soundPitch")).build())
                            .binding(defaults.soundPitch, () -> config.soundPitch, (val) -> config.soundPitch = val)
                            .controller(opt -> FloatFieldControllerBuilder.create(opt).min(0f).max(10f))
                            .build();

                    var soundVolumeOpt = Option.<Float>createBuilder()
                            .name(getName("soundVolume"))
                            .description(OptionDescription.createBuilder().text(getDesc("soundVolume")).build())
                            .binding(defaults.soundVolume, () -> config.soundVolume, (val) -> config.soundVolume = val)
                            .controller(opt -> FloatFieldControllerBuilder.create(opt).min(0f).max(10f))
                            .build();

                    var playSoundEffectOpt = Option.<Boolean>createBuilder()
                            .name(getName("playSoundEffect"))
                            .description(OptionDescription.createBuilder().text(getDesc("playSoundEffect")).build())
                            .binding(defaults.playSoundEffect, () -> config.playSoundEffect, (val) -> config.playSoundEffect = val)
                            .addListener((opt, event) -> {
                                if (event != OptionEventListener.Event.STATE_CHANGE) return;
                                boolean enabled = opt.pendingValue();
                                soundOpt.setAvailable(enabled);
                                soundPitchOpt.setAvailable(enabled);
                                soundVolumeOpt.setAvailable(enabled);
                            })
                            .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
                            .build();

                    soundOpt.setAvailable(config.playSoundEffect);
                    soundPitchOpt.setAvailable(config.playSoundEffect);
                    soundVolumeOpt.setAvailable(config.playSoundEffect);

                    var enableScreenshotBlurOpt = Option.<Boolean>createBuilder()
                            .name(getName("enableScreenshotBlur"))
                            .description(OptionDescription.createBuilder().text(getDesc("enableScreenshotBlur")).build())
                            .binding(defaults.enableScreenshotBlur, () -> config.enableScreenshotBlur, (val) -> config.enableScreenshotBlur = val)
                            .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
                            .build();

                    var screenshotRevealModeOpt = Option.<ScreenshotRevealMode>createBuilder()
                            .name(getName("screenshotRevealMode"))
                            .description(OptionDescription.createBuilder()
                                    .text(getDesc("screenshotRevealMode"))
                                    .build())
                            .binding(defaults.screenshotRevealMode,
                                    () -> config.screenshotRevealMode,
                                    (val) -> config.screenshotRevealMode = val)
                            .controller(opt -> EnumControllerBuilder.create(opt)
                                    .enumClass(ScreenshotRevealMode.class)
                                    .formatValue(val -> Component.translatable(
                                            "seamless_loading_screen.config.screenshotRevealMode."
                                                    + val.name().toLowerCase(Locale.ROOT))))
                            .build();

                    var tintColorOpt = Option.<Color>createBuilder()
                            .name(getName("tintColor"))
                            .description(OptionDescription.createBuilder().text(getDesc("tintColor")).build())
                            .binding(defaults.tintColor, () -> config.tintColor, (val) -> config.tintColor = val)
                            .controller(opt -> ColorControllerBuilder.create(opt).allowAlpha(false))
                            .build();

                    var tintStrengthOpt = Option.<Float>createBuilder()
                            .name(getName("tintStrength"))
                            .description(OptionDescription.createBuilder().text(getDesc("tintStrength")).build())
                            .binding(defaults.tintStrength, () -> config.tintStrength, (val) -> config.tintStrength = val)
                            .controller(opt -> FloatSliderControllerBuilder.create(opt).range(0f, 1f).step(0.05f))
                            .build();

                    var archiveLimitOpt = Option.<Integer>createBuilder()
                            .name(getName("archiveLimit"))
                            .description(OptionDescription.createBuilder().text(getDesc("archiveLimit")).build())
                            .binding(defaults.archiveLimit, () -> config.archiveLimit, (val) -> config.archiveLimit = val)
                            .controller(opt -> IntegerFieldControllerBuilder.create(opt)
                                    .min(MIN_ARCHIVE_LIMIT).max(MAX_ARCHIVE_LIMIT))
                            .build();

                    var archiveScreenshotsOpt = Option.<Boolean>createBuilder()
                            .name(getName("archiveScreenshots"))
                            .description(OptionDescription.createBuilder().text(getDesc("archiveScreenshots")).build())
                            .binding(defaults.archiveScreenshots, () -> config.archiveScreenshots, (val) -> config.archiveScreenshots = val)
                            .addListener((opt, event) -> {
                                if (event == OptionEventListener.Event.STATE_CHANGE) {
                                    archiveLimitOpt.setAvailable(opt.pendingValue());
                                }
                            })
                            .controller(BooleanControllerBuilder::create)
                            .build();

                    archiveLimitOpt.setAvailable(config.archiveScreenshots);

                    var resolutionOpt = Option.<ScreenshotResolution>createBuilder()
                            .name(getName("resolution"))
                            .description(OptionDescription.createBuilder().text(getDesc("resolution")).build())
                            .binding(defaults.resolution, () -> config.resolution, (val) -> config.resolution = val)
                            .controller(opt -> EnumControllerBuilder.create(opt)
                                    .enumClass(ScreenshotResolution.class)
                                    .formatValue(SeamlessLoadingScreenConfig::formatResolution)
                            ).build();

                    var updateWorldIconOpt = Option.<Boolean>createBuilder()
                            .name(getName("updateWorldIcon"))
                            .description(OptionDescription.createBuilder().text(getDesc("updateWorldIcon")).build())
                            .binding(defaults.updateWorldIcon, () -> config.updateWorldIcon, (val) -> config.updateWorldIcon = val)
                            .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
                            .build();

                    var blacklistedAddressOpt = ListOption.<String>createBuilder()
                            .name(getName("blacklistedAddresses"))
                            .description(OptionDescription.createBuilder().text(getDesc("blacklistedAddresses")).build())
                            .binding(defaults.blacklistedAddresses, () -> config.blacklistedAddresses, val -> config.blacklistedAddresses = val)
                            .controller(StringControllerBuilder::create)
                            .initial("")
                            .build();

                    var saveScreenshotsByUsernameOpt = Option.<Boolean>createBuilder()
                            .name(getName("saveScreenshotsByUser"))
                            .description(OptionDescription.createBuilder().text(getDesc("saveScreenshotsByUser")).build())
                            .binding(defaults.saveScreenshotsByUsername, () -> config.saveScreenshotsByUsername, (val) -> config.saveScreenshotsByUsername = val)
                            .controller(opt -> BooleanControllerBuilder.create(opt).yesNoFormatter())
                            .build();

                    return builder
                            .title(getName("title"))
                            .category(ConfigCategory.createBuilder()
                                    .name(getName("display"))
                                    .tooltip(getDesc("display"))
                                    .option(fadeOpt)
                                    .option(screenshotRevealModeOpt)
                                    .group(OptionGroup.createBuilder().name(getName("soundEffects"))
                                            .options(List.of(playSoundEffectOpt, soundOpt, soundVolumeOpt, soundPitchOpt)).build())
                                    .group(OptionGroup.createBuilder().name(getName("screenshotBlur"))
                                            .option(enableScreenshotBlurOpt).build())
                                    .group(OptionGroup.createBuilder().name(getName("tint"))
                                            .options(List.of(tintColorOpt, tintStrengthOpt)).build())
                                    .build())
                            .category(ConfigCategory.createBuilder()
                                    .name(getName("capturing"))
                                    .tooltip(getDesc("capturing"))
                                    .options(List.of(saveScreenshotsByUsernameOpt, archiveScreenshotsOpt, archiveLimitOpt,
                                            resolutionOpt, updateWorldIconOpt))
                                    .build())
                            .category(ConfigCategory.createBuilder()
                                    .name(getName("server_settings"))
                                    .tooltip(getDesc("server_settings"))
                                    .options(List.of(defaultServerModeOpt))
                                    .group(blacklistedAddressOpt)
                                    .build());
                });
    }

    private static Component formatResolution(ScreenshotResolution resolution) {
        var window = Minecraft.getInstance().getWindow();
        ScreenshotResolution.Size size = resolution.resolve(window.getWidth(), window.getHeight());
        return Component.translatable("seamless_loading_screen.config.resolution."
                        + resolution.name().toLowerCase(Locale.ROOT))
                .append(Component.literal(" (" + size.width() + "x" + size.height() + ")"));
    }

    public enum ScreenshotRevealMode {
        Classic,
        New
    }

    public enum ScreenshotResolution {
        Native,
        Optimized;

        public Size resolve(int framebufferWidth, int framebufferHeight) {
            int width = Math.max(1, framebufferWidth);
            int height = Math.max(1, framebufferHeight);
            if (this == Native) return new Size(width, height);

            int optimizedWidth;
            if (width >= 7680) optimizedWidth = 3840;
            else if (width >= 3840) optimizedWidth = 2560;
            else if (width >= 2560) optimizedWidth = 1920;
            else if (width >= 1600) optimizedWidth = 1280;
            else optimizedWidth = Math.max(2, (int) Math.round(width * (2.0D / 3.0D)));

            optimizedWidth = Math.min(width, optimizedWidth);
            if ((optimizedWidth & 1) != 0 && optimizedWidth > 1) {
                optimizedWidth += optimizedWidth < width ? 1 : -1;
            }
            int optimizedHeight = Math.max(1,
                    (int) Math.round(height * (optimizedWidth / (double) width)));
            return new Size(optimizedWidth, optimizedHeight);
        }

        public record Size(int width, int height) {}
    }
}
