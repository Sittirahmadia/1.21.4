package com.crystalspk;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.gui.MacroScreen;
import com.crystalspk.macro.MacroEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CrystalSpKMod implements ClientModInitializer {
    public static final String MOD_ID = "crystalspk";

    private static KeyBinding openGuiKey;

    @Override
    public void onInitializeClient() {
        // Load config
        MacroConfig.get();

        // Register GUI keybinding (default: Right Shift)
        openGuiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crystalspk.open_gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.crystalspk"
        ));

        // Register tick handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Open GUI
            if (openGuiKey.wasPressed()) {
                client.setScreen(new MacroScreen());
            }

            // Tick macro engine (checks keybinds, dispatches macros)
            MacroEngine.get().tick();
        });

        System.out.println("[CrystalSpK] Mod initialized! Press Right Shift to open macro GUI.");
    }
}
