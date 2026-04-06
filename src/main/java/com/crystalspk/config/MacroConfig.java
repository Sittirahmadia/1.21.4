package com.crystalspk.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MacroConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("crystalspk.json");
    private static MacroConfig INSTANCE;

    // Per-macro settings
    public Map<String, MacroEntry> macros = new LinkedHashMap<>();

    // Mode: 0=single click, 1=hold, 2=loop
    public static final int MODE_SINGLE = 0;
    public static final int MODE_HOLD = 1;
    public static final int MODE_LOOP = 2;
    public static final String[] MODE_NAMES = {"Single", "Hold", "Loop"};

    public static class MacroEntry {
        public boolean active = false;
        public int keybind = -1; // GLFW key code, -1 = None
        public int delay = 30;
        public int mode = MODE_SINGLE; // 0=single, 1=hold, 2=loop
        public Map<String, Integer> slots = new LinkedHashMap<>(); // slotName → hotbar slot (0-8) or -1

        public MacroEntry() {}
        public MacroEntry(int defaultDelay) { this.delay = defaultDelay; }
    }

    public static MacroConfig get() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public static void reload() {
        INSTANCE = load();
    }

    private static MacroConfig load() {
        MacroConfig cfg = new MacroConfig();
        // Initialize defaults for all macros
        for (MacroDef def : MacroDef.ALL) {
            MacroEntry entry = new MacroEntry(def.defaultDelay);
            for (String slot : def.slotNames) {
                entry.slots.put(slot, -1);
            }
            cfg.macros.put(def.id, entry);
        }

        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("macros")) {
                    JsonObject jMacros = root.getAsJsonObject("macros");
                    for (Map.Entry<String, JsonElement> e : jMacros.entrySet()) {
                        MacroEntry entry = cfg.macros.get(e.getKey());
                        if (entry == null) continue;
                        JsonObject jm = e.getValue().getAsJsonObject();
                        if (jm.has("active")) entry.active = jm.get("active").getAsBoolean();
                        if (jm.has("keybind")) entry.keybind = jm.get("keybind").getAsInt();
                        if (jm.has("delay")) entry.delay = jm.get("delay").getAsInt();
                        if (jm.has("mode")) entry.mode = Math.max(0, Math.min(2, jm.get("mode").getAsInt()));
                        if (jm.has("slots")) {
                            JsonObject jSlots = jm.getAsJsonObject("slots");
                            for (Map.Entry<String, JsonElement> se : jSlots.entrySet()) {
                                if (entry.slots.containsKey(se.getKey())) {
                                    entry.slots.put(se.getKey(), se.getValue().getAsInt());
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[CrystalSpK] Config load error: " + e.getMessage());
            }
        }
        return cfg;
    }

    public void save() {
        JsonObject root = new JsonObject();
        JsonObject jMacros = new JsonObject();
        for (Map.Entry<String, MacroEntry> e : macros.entrySet()) {
            JsonObject jm = new JsonObject();
            MacroEntry entry = e.getValue();
            jm.addProperty("active", entry.active);
            jm.addProperty("keybind", entry.keybind);
            jm.addProperty("delay", entry.delay);
            jm.addProperty("mode", entry.mode);
            JsonObject jSlots = new JsonObject();
            for (Map.Entry<String, Integer> se : entry.slots.entrySet()) {
                jSlots.addProperty(se.getKey(), se.getValue());
            }
            jm.add("slots", jSlots);
            jMacros.add(e.getKey(), jm);
        }
        root.add("macros", jMacros);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(root, writer);
            }
        } catch (Exception e) {
            System.err.println("[CrystalSpK] Config save error: " + e.getMessage());
        }
    }
}
