package com.crystalspk.macro;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.config.MacroDef;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.concurrent.*;

/**
 * Handles keybind detection and macro dispatch.
 * Called every client tick from the mod's tick handler.
 */
public class MacroEngine {
    private static final MacroEngine INSTANCE = new MacroEngine();
    private final Set<String> pressedKeys = new HashSet<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "CrystalSpK-Macro");
        t.setDaemon(true);
        return t;
    });

    // Hold-to-run state
    private volatile boolean fxpActive = false;
    private volatile boolean acActive = false;
    private Future<?> fxpFuture;
    private Future<?> acFuture;

    public static MacroEngine get() { return INSTANCE; }

    /**
     * Called every client tick. Checks all macro keybinds.
     */
    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return; // Don't fire macros when GUI is open

        MacroConfig cfg = MacroConfig.get();
        long window = mc.getWindow().getHandle();

        for (MacroDef def : MacroDef.ALL) {
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null || !entry.active || entry.keybind <= 0) continue;

            boolean isDown = GLFW.glfwGetKey(window, entry.keybind) == GLFW.GLFW_PRESS;
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

            // One-shot macros — fire on key down, don't repeat
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
}
