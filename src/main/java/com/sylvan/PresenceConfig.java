package com.sylvan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PresenceConfig {
	public boolean footstepsEnabled = true;
	public int footstepsDelayMin = 60 * 45;
	public int footstepsDelayMax = 60 * 60 * 3;
	public int footstepsReflexMs = 500;
	public int footstepsMaxReflexVariance = 150;
	public int footstepsStepsMin = 1;
	public int footstepsStepsMax = 5;
	public int footstepsStepVarianceMax = 25;

	public static PresenceConfig loadConfig() {
		Gson gson = new Gson();
		File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "presence.json");

		// Return default if no file is not found
		if (!configFile.exists()) return new PresenceConfig();

		try (FileReader reader = new FileReader(configFile)) {
			return gson.fromJson(reader, PresenceConfig.class);
		} catch (IOException e) {
			e.printStackTrace();
			return new PresenceConfig();
		}
	}

	public void saveConfig() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		File configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), "presence.json");
		try (FileWriter writer = new FileWriter(configFile)) {
			gson.toJson(this, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
