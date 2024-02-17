package com.sylvan.presence.util;

import com.google.gson.Gson;
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

	public Config(final String configName) {
		configFileName = configName + ".json";
		File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), configFileName);

		// Return default if no file is not found
		if (!configFile.exists()) {
			try {
				configFile.createNewFile();
			} catch (IOException e) {
				Presence.LOGGER.warn("Failed to create configuration file for \"" + configFileName + "\":\n" + e.getStackTrace().toString());
			}
		}
	}

	public <T> T getOrSetValue(final String key, final T defaultValue) {
		try {
			JsonElement element = JsonParser.parseReader(new FileReader(configFileName));
			if (element.isJsonObject()) {
				JsonObject object = element.getAsJsonObject();
				// Get the value if it exists
				if (object.has(key)) {
					Gson gson = new Gson();
					Type type = new TypeToken<T>() {}.getType();
					try {
                    				return gson.fromJson(object.get(key), type);
					} catch (JsonSyntaxException e) {
						// Remove key on invalid input and have it reset to default
						Presence.LOGGER.warn("Invalid type for JSON value for \"" + key + "\":\n" + e.getStackTrace().toString());
						object.remove(key);
					}
				}
				object.addProperty(key, defaultValue.toString());
				saveConfig(object);
			} else {
				JsonObject object = new JsonObject();
				object.addProperty(key, defaultValue.toString());
				saveConfig(object);
			}
			return defaultValue;
		} catch (IOException e) {
			Presence.LOGGER.warn("Failed to get/set JSON value for \"" + key + "\":\n" + e.getStackTrace().toString());
		}
		return defaultValue;
	}

	public void saveConfig(JsonObject object) {
		try (FileWriter fileWriter = new FileWriter(configFileName)) {
			fileWriter.write(object.toString());
		} catch(IOException e) {
			Presence.LOGGER.warn("Failed to save config file for \"" + configFileName + "\":\n" + e.getStackTrace().toString());
		}
	}
}
