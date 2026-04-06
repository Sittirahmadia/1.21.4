package com.crystalspk.macro;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.config.MacroDef;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.*;

/**
 * Handles keybind detection and macro dispatch.
 * Supports both keyboard keys (positive keybind values) and mouse buttons (negative values).
 *
 * Mouse button encoding:
 *   -2 = Right Mouse Button
 *   -3 = Middle Mouse Button
 *   -4 = Mouse Button 4 (Side 1)
 *   -5 = Mouse Button 5 (Side 2)
 */
public class MacroEngine {
    private static final MacroEngine INSTANCE = new MacroEngine();
    private final Set<String> pressedKeys = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "CrystalSpK-Macro");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean fxpActive = false;
    private volatile boolean acActive = false;
    private Future<?> fxpFuture;
    private Future<?> acFuture;

    public static MacroEngine get() { return INSTANCE; }

    /**
     * Check if a keybind (keyboard key or mouse button) is currently pressed.
     */
    private boolean isBindDown(long window, int keybind) {
        if (keybind == 0) return false;
        if (keybind > 0) {
            // Keyboard key
            return GLFW.glfwGetKey(window, keybind) == GLFW.GLFW_PRESS;
        } else {
            // Mouse button: -2=right, -3=middle, -4=btn4, -5=btn5
            int mouseBtn = -(keybind) - 1; // -2 → 1 (right), -3 → 2 (middle), etc.
            if (mouseBtn < 0 || mouseBtn > 7) return false;
            return GLFW.glfwGetMouseButton(window, mouseBtn) == GLFW.GLFW_PRESS;
        }
    }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        MacroConfig cfg = MacroConfig.get();
        long window = mc.getWindow().getHandle();

        for (MacroDef def : MacroDef.ALL) {
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null || !entry.active || entry.keybind == 0) continue;

            boolean isDown = isBindDown(window, entry.keybind);
            String id = def.id;

            // Hold-to-run macros (FXP, AC)
            if (id.equals("fxp") || id.equals("ac")) {
                if (isDown && !pressedKeys.contains(id)) {
                    pressedKeys.add(id);
                    startHoldMacro(id, entry);
                } else if (!isDown && pressedKeys.contains(id)) {
                    pressedKeys.remove(id);
                    stopHoldMacro(id);
                }
                continue;
            }

            // One-shot macros
            if (isDown && !pressedKeys.contains(id)) {
                pressedKeys.add(id);
                if (!MacroRunner.isRunning(id)) {
                    final MacroConfig.MacroEntry e = entry;
                    executor.submit(() -> MacroRunner.dispatch(id, e));
                }
            } else if (!isDown) {
                pressedKeys.remove(id);
            }
        }
    }

    private void startHoldMacro(String id, MacroConfig.MacroEntry entry) {
        if (id.equals("fxp") && !fxpActive) {
            fxpActive = true;
            fxpFuture = executor.submit(() -> MacroRunner.runFXPLoop(entry.delay, () -> fxpActive));
        } else if (id.equals("ac") && !acActive) {
            acActive = true;
            int crystalSlot = entry.slots.getOrDefault("crystalSlot", -1);
            acFuture = executor.submit(() -> MacroRunner.runACLoop(crystalSlot, entry.delay, () -> acActive));
        }
    }

    private void stopHoldMacro(String id) {
        if (id.equals("fxp")) {
            fxpActive = false;
            if (fxpFuture != null) fxpFuture.cancel(true);
        } else if (id.equals("ac")) {
            acActive = false;
            if (acFuture != null) acFuture.cancel(true);
        }
    }

    public void stopAll() {
        MacroRunner.cancelAll();
        fxpActive = false;
        acActive = false;
        pressedKeys.clear();
    }

    /**
     * Get display name for a keybind value.
     * Positive = keyboard key, Negative = mouse button, 0 = None.
     */
    public static String getBindName(int keybind) {
        if (keybind == 0) return "None";
        if (keybind > 0) {
            String name = GLFW.glfwGetKeyName(keybind, 0);
            if (name != null) return name.toUpperCase();
            return switch (keybind) {
                case GLFW.GLFW_KEY_SPACE -> "SPACE";
                case GLFW.GLFW_KEY_LEFT_SHIFT -> "L_SHIFT";
                case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R_SHIFT";
                case GLFW.GLFW_KEY_LEFT_CONTROL -> "L_CTRL";
                case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R_CTRL";
                case GLFW.GLFW_KEY_LEFT_ALT -> "L_ALT";
                case GLFW.GLFW_KEY_RIGHT_ALT -> "R_ALT";
                case GLFW.GLFW_KEY_TAB -> "TAB";
                case GLFW.GLFW_KEY_ENTER -> "ENTER";
                case GLFW.GLFW_KEY_BACKSPACE -> "BKSP";
                case GLFW.GLFW_KEY_DELETE -> "DEL";
                case GLFW.GLFW_KEY_INSERT -> "INS";
                case GLFW.GLFW_KEY_HOME -> "HOME";
                case GLFW.GLFW_KEY_END -> "END";
                case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
                default -> {
                    if (keybind >= GLFW.GLFW_KEY_F1 && keybind <= GLFW.GLFW_KEY_F12)
                        yield "F" + (keybind - GLFW.GLFW_KEY_F1 + 1);
                    yield "KEY_" + keybind;
                }
            };
        }
        // Mouse buttons
        return switch (keybind) {
            case -2 -> "MOUSE_R";
            case -3 -> "MOUSE_M";
            case -4 -> "MOUSE_4";
            case -5 -> "MOUSE_5";
            default -> "MOUSE_" + (-(keybind) - 1);
        };
    }
}
