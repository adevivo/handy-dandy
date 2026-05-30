package com.neovetta.handydandy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ModConfig {
    public boolean debugging = false;
    public boolean enableHeal = true;
    public String healXpType = "variable";
    public float healXpCost = 0.5f;
    public boolean enableTp = true;
    public int maxLocations = 5;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("handydandy.json");
        File configFile = configPath.toFile();
        if (!configFile.exists()) {
            ModConfig defaults = new ModConfig();
            defaults.save(configFile);
            return defaults;
        }
        try (Reader reader = new FileReader(configFile)) {
            ModConfig cfg = GSON.fromJson(reader, ModConfig.class);
            return cfg != null ? cfg : new ModConfig();
        } catch (IOException e) {
            HandyDandy.LOGGER.error("Failed to load config, using defaults", e);
            return new ModConfig();
        }
    }

    private void save(File file) {
        try (Writer writer = new FileWriter(file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            HandyDandy.LOGGER.error("Failed to save default config", e);
        }
    }
}
