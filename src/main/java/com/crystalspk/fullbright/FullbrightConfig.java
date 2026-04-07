package com.crystalspk.fullbright;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Fullbright configuration persistence
 */
public class FullbrightConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_DIR = "config/crystalspk";
    private static final String CONFIG_FILE = "fullbright.json";

    private boolean enabled = false;
    private float gamma = 100.0f; // 0-1000

    private static FullbrightConfig instance;

    private FullbrightConfig() {
        load();
    }

    public static FullbrightConfig getInstance() {
        if (instance == null) {
            instance = new FullbrightConfig();
        }
        return instance;
    }

    /**
     * Load configuration from file
     */
    private void load() {
        try {
            File file = new File(CONFIG_DIR, CONFIG_FILE);
            if (file.exists()) {
                FileReader reader = new FileReader(file);
                FullbrightConfig loaded = GSON.fromJson(reader, FullbrightConfig.class);
                reader.close();

                if (loaded != null) {
                    this.enabled = loaded.enabled;
                    this.gamma = Math.max(0, Math.min(1000, loaded.gamma));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Save configuration to file
     */
    public void save() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, CONFIG_FILE);
            FileWriter writer = new FileWriter(file);
            GSON.toJson(this, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getGamma() {
        return gamma;
    }

    public void setGamma(float gamma) {
        this.gamma = Math.max(0, Math.min(1000, gamma));
    }
}
