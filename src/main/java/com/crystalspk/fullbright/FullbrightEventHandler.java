package com.crystalspk.fullbright;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Event handler for fullbright keybinds and tick updates
 */
public class FullbrightEventHandler {
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(FullbrightEventHandler::onClientTick);
    }

    private static void onClientTick(MinecraftClient mc) {
        if (mc == null) return;

        // Handle toggle key
        if (FullbrightKeybind.isTogglePressed()) {
            FullbrightModule fullbright = FullbrightModule.getInstance();
            fullbright.toggle();
        }

        // Handle settings key
        if (FullbrightKeybind.isSettingsPressed()) {
            mc.setScreen(new FullbrightScreen(mc.currentScreen));
        }
    }
}
