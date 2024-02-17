package com.sylvan.presence.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.sylvan.presence.Presence;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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
				Presence.LOGGER.error("Failed to create configuration file for \"" + configFileName + "\".", e);
			}
		}
	}

	private <T> JsonElement setValue(JsonObject jsonObject, final String key, final T value) {
		final Gson gson = new Gson();
		if (jsonObject == null) {
			jsonObject = new JsonObject();
		}
		jsonObject.add(key, gson.toJsonTree(value));
		saveConfig(jsonObject);
		return jsonObject.get(key);
	}

	public <T> JsonElement getOrSetValue(final String key, final T defaultValue) {
		try {
			JsonElement root = JsonParser.parseReader(new FileReader(configFile));
			if (root.isJsonObject()) {
				JsonObject object = root.getAsJsonObject();
				// Get the value if it exists
				if (object.has(key)) {
					try {
						return object.get(key);
					} catch (JsonSyntaxException e) {
						// Remove key on invalid input and have it reset to default
						Presence.LOGGER.error("JSON syntax exception for \"" + key + "\" in \"" + configFileName + "\".", e);
						object.remove(key);
					}
				}
				return setValue(object, key, defaultValue);
			}
		} catch (IOException e) {
			Presence.LOGGER.error("IOException while attempting to get/set JSON value for \"" + key + "\" in \"" + configFileName + "\".", e);
		} catch (JsonSyntaxException e) {
			// Remove key on invalid input and have it reset to default
			Presence.LOGGER.error("JSON syntax exception for \"" + key + "\" in \"" + configFileName + "\".", e);
		}
		return setValue(null, key, defaultValue);
	}

	public void saveConfig(JsonObject object) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try (FileWriter fileWriter = new FileWriter(configFile)) {
			fileWriter.write(gson.toJson(object));
		} catch(IOException e) {
			Presence.LOGGER.error("Failed to save config file for \"" + configFileName + "\".", e);
		}
	}

	public void clearConfig() {
		try {
			configFile.delete();
			configFile.createNewFile();
		} catch (IOException e) {
			Presence.LOGGER.error("Failed to clear configuration file for \"" + configFileName + "\".", e);
		}
	}
}
