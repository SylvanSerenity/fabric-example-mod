package com.sylvan.presence.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.sylvan.presence.Presence;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;

public class Config {
	private String configFileName;
	File configFile;

	public Config(final String configName) {
		configFileName = configName + ".json";
		configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), configFileName);

		// Return default if no file is not found
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				Presence.LOGGER.warn("Failed to create configuration file for \"" + configFileName + "\".");
				e.printStackTrace();
			}
		}
	}

	public <T> T getOrSetValue(final String key, final T defaultValue) {
		try {
			final Gson gson = new Gson();
			JsonElement element = JsonParser.parseReader(new FileReader(configFile));
			if (element.isJsonObject()) {
				JsonObject object = element.getAsJsonObject();
				// Get the value if it exists
				if (object.has(key)) {
					Type type = new TypeToken<T>() {}.getType();
					try {
                    				final T value = gson.fromJson(object.get(key), type);
						return value;
					} catch (JsonSyntaxException e) {
						// Remove key on invalid input and have it reset to default
						Presence.LOGGER.warn("Invalid type for JSON value for \"" + key + "\".");
						e.printStackTrace();
						object.remove(key);
					}
				}
				object.add(key, gson.toJsonTree(defaultValue));
				saveConfig(object);
			} else {
				JsonObject object = new JsonObject();
				object.add(key, gson.toJsonTree(defaultValue));
				saveConfig(object);
			}
			return defaultValue;
		} catch (IOException e) {
			Presence.LOGGER.warn("Failed to get/set JSON value for \"" + key + "\".");
			e.printStackTrace();
		}
		return defaultValue;
	}

	public void saveConfig(JsonObject object) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (FileWriter fileWriter = new FileWriter(configFile)) {
			fileWriter.write(gson.toJson(object));
		} catch(IOException e) {
			Presence.LOGGER.warn("Failed to save config file for \"" + configFileName + "\".");
			e.printStackTrace();
		}
	}
}
