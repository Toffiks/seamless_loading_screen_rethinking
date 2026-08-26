package com.minenash.seamless_loading_screen.config;

import com.minenash.seamless_loading_screen.DisplayMode;
import com.minenash.seamless_loading_screen.PlatformFunctions;
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

import java.awt.Color;
import java.util.List;
import java.util.Locale;

public class SeamlessLoadingScreenConfig {
    public static final int MAX_FADE_TICKS = 1200;
    private static final SafeColorTypeAdapter COLOR_ADAPTER = new SafeColorTypeAdapter(() -> getDefaults().tintColor);
    private static final ConfigClassHandler<SeamlessLoadingScreenConfig> CONFIG_CLASS_HANDLER = ConfigClassHandler
            .createBuilder(SeamlessLoadingScreenConfig.class)
            .id(Identifier.fromNamespaceAndPath("seamless_loading_screen", "config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .appendGsonBuilder(builder -> builder.setPrettyPrinting()
                            .disableHtmlEscaping()
                            .serializeNulls()
                            .registerTypeHierarchyAdapter(Color.class, COLOR_ADAPTER))
                    .setPath(PlatformFunctions.getConfigDirectory().resolve("seamless_loading_screen.json"))
                    .build()
            )
            .build();

    @SerialEntry
    public int fade = 20;
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

                    var archiveScreenshotsOpt = Option.<Boolean>createBuilder()
                            .name(getName("archiveScreenshots"))
                            .description(OptionDescription.createBuilder().text(getDesc("archiveScreenshots")).build())
                            .binding(defaults.archiveScreenshots, () -> config.archiveScreenshots, (val) -> config.archiveScreenshots = val)
                            .controller(BooleanControllerBuilder::create)
                            .build();

                    var resolutionOpt = Option.<ScreenshotResolution>createBuilder()
                            .name(getName("resolution"))
                            .description(OptionDescription.createBuilder().text(getDesc("resolution")).build())
                            .binding(defaults.resolution, () -> config.resolution, (val) -> config.resolution = val)
                            .controller(opt -> EnumControllerBuilder.create(opt)
                                    .enumClass(ScreenshotResolution.class)
                                    .formatValue(val -> Component.translatable("seamless_loading_screen.config.resolution." + val.name().toLowerCase(Locale.ROOT)))
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
                                    .options(List.of(saveScreenshotsByUsernameOpt, archiveScreenshotsOpt, resolutionOpt, updateWorldIconOpt))
                                    .build())
                            .category(ConfigCategory.createBuilder()
                                    .name(getName("server_settings"))
                                    .tooltip(getDesc("server_settings"))
                                    .options(List.of(defaultServerModeOpt))
                                    .group(blacklistedAddressOpt)
                                    .build());
                });
    }

    public enum ScreenshotResolution {
        Native(0, 0),
        Normal(4000, 1600),
        r4K(4000, 2160),
        r8K(7900, 4320);

        public final int width;
        public final int height;

        ScreenshotResolution(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
