package com.crystalspk.fullbright;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Keybind handler for fullbright module
 */
public class FullbrightKeybind {
    private static KeyBinding toggleFullbrightKey;
    private static KeyBinding openSettingsKey;

    public static void register() {
        toggleFullbrightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crystalspk.fullbright.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F,
                "category.crystalspk"
        ));

        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.crystalspk.fullbright.settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.crystalspk"
        ));
    }

    public static KeyBinding getToggleKey() {
        return toggleFullbrightKey;
    }

    public static KeyBinding getSettingsKey() {
        return openSettingsKey;
    }

    public static boolean isTogglePressed() {
        return toggleFullbrightKey != null && toggleFullbrightKey.wasPressed();
    }

    public static boolean isSettingsPressed() {
        return openSettingsKey != null && openSettingsKey.wasPressed();
    }
}
