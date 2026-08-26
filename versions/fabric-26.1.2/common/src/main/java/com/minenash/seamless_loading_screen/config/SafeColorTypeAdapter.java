package com.minenash.seamless_loading_screen.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.awt.Color;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class SafeColorTypeAdapter implements JsonSerializer<Color>, JsonDeserializer<Color> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Supplier<Color> fallbackColor;
    private boolean error;

    public SafeColorTypeAdapter(Supplier<Color> fallbackColor) {
        this.fallbackColor = fallbackColor;
    }

    public boolean errored() {
        boolean result = error;
        error = false;
        return result;
    }

    @Override
    public Color deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            if (jsonElement instanceof JsonPrimitive primitive) {
                if (primitive.isNumber()) {
                    return new Color(jsonElement.getAsInt(), true);
                } else if (primitive.getAsString().startsWith("#")) {
                    error = true;

                    return new Color(Integer.parseInt(primitive.getAsString().substring(1), 16), false);
                }
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Exception thrown during Color Deserialization", e);
        }

        error = true;

        LOGGER.warn("Unable to parse the color from the config file; using the default value instead. [Value: {}]", jsonElement);

        return fallbackColor.get();
    }

    @Override
    public JsonElement serialize(Color color, Type type, JsonSerializationContext context) {
        return new JsonPrimitive(color.getRGB());
    }
}
