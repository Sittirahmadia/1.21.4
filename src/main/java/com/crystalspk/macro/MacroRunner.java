package com.crystalspk.macro;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.mixin.KeyBindingAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * All macro implementations matching the AHK source exactly.
 * Each macro runs on a worker thread and uses Thread.sleep for timing.
 * Slot switching is done via MinecraftClient.getInstance().player.getInventory().selectedSlot.
 * Clicks are simulated by pressing the MC attack/use keybindings.
 */
public class MacroRunner {
    private static final ConcurrentHashMap<String, AtomicBoolean> running = new ConcurrentHashMap<>();
    private static final AtomicBoolean globalCancel = new AtomicBoolean(false);

    public static boolean isRunning(String id) {
        AtomicBoolean flag = running.get(id);
        return flag != null && flag.get();
    }

    public static void cancelAll() {
        globalCancel.set(true);
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        globalCancel.set(false);
        running.clear();
    }

    private static boolean check() {
        return !globalCancel.get() && !Thread.currentThread().isInterrupted();
    }

    private static void sleep(int ms) throws InterruptedException {
        if (ms > 0) Thread.sleep(ms);
    }

    // ── Input helpers (run on render thread via execute) ──────────────────

    // Minimum delay between steps — one MC tick = 50ms.
    // Without this, slot switches can be missed because mc.execute() is async.
    private static final int MIN_STEP_MS = 50;

    private static void switchSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) mc.player.getInventory().selectedSlot = slot;
        });
    }

    private static void rightClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            KeyBinding useKey = mc.options.useKey;
            KeyBindingAccessor acc = (KeyBindingAccessor) useKey;
            acc.setTimesPressed(acc.getTimesPressed() + 1);
        });
    }

    private static void leftClick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            KeyBinding attackKey = mc.options.attackKey;
            KeyBindingAccessor acc = (KeyBindingAccessor) attackKey;
            acc.setTimesPressed(acc.getTimesPressed() + 1);
        });
    }

    /**
     * Atomic slot switch + right-click in ONE mc.execute() call.
     * This guarantees the slot is changed BEFORE the click fires,
     * both within the same game tick — no race condition possible.
     */
    private static void switchSlotAndRightClick(int slot) {
        if (slot < 0 || slot > 8) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;
            mc.player.getInventory().selectedSlot = slot;
            KeyBinding useKey = mc.options.useKey;
            KeyBindingAccessor acc = (KeyBindingAccessor) useKey;
            acc.setTimesPressed(acc.getTimesPressed() + 1);
        });
    }

    /**
     * Atomic slot switch + left-click in ONE mc.execute() call.
     */
    private static void switchSlotAndLeftClick(int slot) {
        if (slot < 0 || slot > 8) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;
            mc.player.getInventory().selectedSlot = slot;
            KeyBinding attackKey = mc.options.attackKey;
            KeyBindingAccessor acc = (KeyBindingAccessor) attackKey;
            acc.setTimesPressed(acc.getTimesPressed() + 1);
        });
    }

    private static int getSlot(MacroConfig.MacroEntry entry, String name) {
        return entry.slots.getOrDefault(name, -1);
    }

    // ── Dispatch ─────────────────────────────────────────────────────────

    public static void dispatch(String id, MacroConfig.MacroEntry entry) {
        AtomicBoolean flag = new AtomicBoolean(true);
        if (running.putIfAbsent(id, flag) != null) return; // already running

        try {
            switch (id) {
                case "sa"  -> runSA(entry);
                case "da"  -> runDA(entry);
                case "ap"  -> runAP(entry);
                case "hc"  -> runHC(entry);
                case "kp"  -> runKP(entry);
                case "idh" -> runIDH(entry);
                case "oht" -> runOHT(entry);
                case "asb" -> runASB(entry);
                case "sr"  -> runSR(entry);
                case "ls"  -> runLS(entry);
                case "es"  -> runES(entry);
                case "pc"  -> runPC(entry);
                case "ss"  -> runSS(entry);
                case "bs"  -> runBS(entry);
                case "ic"  -> runIC(entry);
                case "xb"  -> runXB(entry);
                case "dr"  -> runDR(entry);
                case "lw"  -> runLW(entry);
                case "la"  -> runLA(entry);
            }
        } catch (InterruptedException ignored) {
        } finally {
            running.remove(id);
        }
    }

    // ── SA — Single Anchor ───────────────────────────────────────────────
    // Sequence: anchor+rclick → glowstone+rclick (1 charge) → detonate+rclick
    private static void runSA(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int anchor = getSlot(e, "anchorSlot");
        int glowstone = getSlot(e, "glowstoneSlot");
        int explode = getSlot(e, "explodeSlot");
        int det = explode >= 0 ? explode : anchor;

        // 1. Place anchor
        switchSlotAndRightClick(anchor);
        sleep(d); if (!check()) return;

        // 2. Charge with glowstone (single charge)
        switchSlotAndRightClick(glowstone);
        sleep(d); if (!check()) return;

        // 3. Detonate
        switchSlotAndRightClick(det);
    }

    // ── DA — Double Anchor ───────────────────────────────────────────────
    // Two SA cycles back-to-back. Each: anchor+rclick → glowstone+rclick → det+rclick
    private static void runDA(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int anchor = getSlot(e, "anchorSlot");
        int glowstone = getSlot(e, "glowstoneSlot");
        int explode = getSlot(e, "explodeSlot");
        int det = explode >= 0 ? explode : anchor;

        for (int i = 0; i < 2; i++) {
            // 1. Place anchor
            switchSlotAndRightClick(anchor);
            sleep(d); if (!check()) return;

            // 2. Charge with glowstone (single charge)
            switchSlotAndRightClick(glowstone);
            sleep(d); if (!check()) return;

            // 3. Detonate
            switchSlotAndRightClick(det);

            // Gap before second cycle
            if (i == 0) {
                sleep(d); if (!check()) return;
            }
        }
    }

    // ── AP — Anchor Pearl (matches ap.ahk) ──────────────────────────────
    // Full SA cycle then immediately pearl throw
    private static void runAP(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int anchor = getSlot(e, "anchorSlot");
        int glowstone = getSlot(e, "glowstoneSlot");
        int explode = getSlot(e, "explodeSlot");
        int pearl = getSlot(e, "pearlSlot");
        int det = explode >= 0 ? explode : anchor;

        // SA cycle (1 charge)
        switchSlotAndRightClick(anchor);
        sleep(d); if (!check()) return;
        switchSlotAndRightClick(glowstone);
        sleep(d); if (!check()) return;
        switchSlotAndRightClick(det);
        sleep(d); if (!check()) return;

        // Pearl throw
        switchSlotAndRightClick(pearl);
    }

    // ── HC — Hit Crystal (matches hc.ahk) ───────────────────────────────
    // Sequential: switch obsidian → wait → place → wait → switch crystal → wait → place → wait → hit
    // Uses sequential switchSlot + sleep + click to ensure MC processes each step fully.
    private static void runHC(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int obsidian = getSlot(e, "obsidianSlot");
        int crystal = getSlot(e, "crystalSlot");

        // 1. Switch to obsidian
        switchSlot(obsidian);
        sleep(d); if (!check()) return;

        // 2. Place obsidian
        rightClick();
        sleep(d); if (!check()) return;

        // 3. Switch to crystal
        switchSlot(crystal);
        sleep(d); if (!check()) return;

        // 4. Place crystal
        rightClick();
        sleep(d); if (!check()) return;

        // 5. Hit crystal to detonate
        leftClick();
    }

    // ── KP — Key Pearl (matches kp.ahk) ─────────────────────────────────
    private static void runKP(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int pearl = getSlot(e, "pearlSlot");
        int ret = getSlot(e, "returnSlot");

        switchSlotAndRightClick(pearl);
        sleep(d); if (!check()) return;
        switchSlot(ret);
    }

    // ── IDH — Inventory D-Hand (matches idh.ahk) ────────────────────────
    // totem slot → swap to offhand → open inventory
    private static void runIDH(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int totem = getSlot(e, "totemSlot");
        int swap = getSlot(e, "swapSlot");

        switchSlot(totem); sleep(d); if (!check()) return;

        // Swap to offhand (press F key equivalent)
        if (swap >= 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    KeyBinding swapKey = mc.options.swapHandsKey;
                    KeyBindingAccessor acc = (KeyBindingAccessor) swapKey;
                    acc.setTimesPressed(acc.getTimesPressed() + 1);
                }
            });
            sleep(d); if (!check()) return;
        }

        // Open inventory
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                KeyBinding invKey = mc.options.inventoryKey;
                KeyBindingAccessor acc = (KeyBindingAccessor) invKey;
                acc.setTimesPressed(acc.getTimesPressed() + 1);
            }
        });
    }

    // ── OHT — Offhand Totem (matches oht.ahk) ──────────────────────────
    private static void runOHT(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int totem = getSlot(e, "totemSlot");

        sleep(30);
        switchSlot(totem); sleep(d); if (!check()) return;

        // Swap to offhand
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                KeyBinding swapKey = mc.options.swapHandsKey;
                KeyBindingAccessor acc = (KeyBindingAccessor) swapKey;
                acc.setTimesPressed(acc.getTimesPressed() + 1);
            }
        });
    }

    // ── ASB — Auto Shield Breaker (matches asb.ahk) ─────────────────────
    private static void runASB(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int axe = getSlot(e, "axeSlot");
        int sword = getSlot(e, "swordSlot");

        sleep(30);
        switchSlotAndLeftClick(axe);
        sleep(d); if (!check()) return;
        switchSlot(sword);
    }

    // ── SR — Sprint Reset (matches sr.ahk) ──────────────────────────────
    // left-click → double W tap
    private static void runSR(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);

        leftClick(); sleep(d); if (!check()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        // Double W tap for sprint reset
        mc.execute(() -> {
            KeyBinding fwd = mc.options.forwardKey;
            KeyBindingAccessor acc = (KeyBindingAccessor) fwd;
            acc.setPressed(true);
        });
        sleep(15); if (!check()) return;
        mc.execute(() -> {
            KeyBinding fwd = mc.options.forwardKey;
            ((KeyBindingAccessor) fwd).setPressed(false);
        });
        sleep(15); if (!check()) return;
        mc.execute(() -> {
            KeyBinding fwd = mc.options.forwardKey;
            ((KeyBindingAccessor) fwd).setPressed(true);
        });
        sleep(15); if (!check()) return;
        mc.execute(() -> {
            KeyBinding fwd = mc.options.forwardKey;
            ((KeyBindingAccessor) fwd).setPressed(false);
        });
    }

    // ── LS — Lunge Swap ─────────────────────────────────────────────────
    private static void runLS(MacroConfig.MacroEntry e) throws InterruptedException {
        int sword = getSlot(e, "swordSlot");
        int spear = getSlot(e, "spearSlot");

        switchSlot(sword); sleep(MIN_STEP_MS); if (!check()) return;
        switchSlotAndLeftClick(spear);
        sleep(MIN_STEP_MS); if (!check()) return;
        switchSlot(sword);
    }

    // ── ES — Elytra Swap ────────────────────────────────────────────────
    private static void runES(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int elytra = getSlot(e, "elytraSlot");
        int ret = getSlot(e, "returnSlot");

        sleep(30);
        switchSlotAndRightClick(elytra);
        sleep(d); if (!check()) return;
        switchSlot(ret);
    }

    // ── PC — Pearl Catch ────────────────────────────────────────────────
    private static void runPC(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int pearl = getSlot(e, "pearlSlot");
        int wind = getSlot(e, "windChargeSlot");

        sleep(30);
        switchSlotAndRightClick(pearl);
        sleep(d); if (!check()) return;
        switchSlotAndRightClick(wind);
    }

    // ── SS — Stun Slam ──────────────────────────────────────────────────
    private static void runSS(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int axe = getSlot(e, "axeSlot");
        int mace = getSlot(e, "maceSlot");

        sleep(30);
        switchSlotAndLeftClick(axe);
        sleep(d); if (!check()) return;
        switchSlotAndLeftClick(mace);
    }

    // ── BS — Breach Swap ────────────────────────────────────────────────
    private static void runBS(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int mace = getSlot(e, "maceSlot");
        int sword = getSlot(e, "swordSlot");

        sleep(30);
        switchSlotAndLeftClick(mace);
        sleep(d); if (!check()) return;
        switchSlot(sword);
    }

    // ── IC — Insta Cart ─────────────────────────────────────────────────
    private static void runIC(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int rail = getSlot(e, "railSlot");
        int bow = getSlot(e, "bowSlot");
        int cart = getSlot(e, "cartSlot");

        // Place rail
        switchSlotAndRightClick(rail);
        sleep(d); if (!check()) return;

        // Draw and fire bow
        switchSlot(bow); sleep(MIN_STEP_MS); if (!check()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> ((KeyBindingAccessor) mc.options.useKey).setPressed(true));
        sleep(150); // bow draw time
        mc.execute(() -> ((KeyBindingAccessor) mc.options.useKey).setPressed(false));
        sleep(d); if (!check()) return;

        // Place cart
        switchSlotAndRightClick(cart);
    }

    // ── XB — Crossbow Cart ──────────────────────────────────────────────
    private static void runXB(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int rail = getSlot(e, "railSlot");
        int cart = getSlot(e, "cartSlot");
        int fns = getSlot(e, "fnsSlot");
        int crossbow = getSlot(e, "crossbowSlot");

        switchSlotAndRightClick(rail);     sleep(d); if (!check()) return;
        switchSlotAndRightClick(cart);     sleep(d); if (!check()) return;
        switchSlotAndRightClick(fns);      sleep(d); if (!check()) return;
        switchSlotAndRightClick(crossbow);
    }

    // ── DR — Drain ──────────────────────────────────────────────────────
    private static void runDR(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int bucket = getSlot(e, "bucketSlot");

        switchSlotAndRightClick(bucket);
    }

    // ── LW — Lava Web ──────────────────────────────────────────────────
    private static void runLW(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int lava = getSlot(e, "lavaSlot");
        int cobweb = getSlot(e, "cobwebSlot");

        switchSlotAndRightClick(lava);  // place lava
        sleep(d); if (!check()) return;
        rightClick();                   // pick lava back up
        sleep(d); if (!check()) return;
        switchSlotAndRightClick(cobweb); // place cobweb
    }

    // ── LA — Lava ───────────────────────────────────────────────────────
    private static void runLA(MacroConfig.MacroEntry e) throws InterruptedException {
        int d = Math.max(MIN_STEP_MS, e.delay);
        int lava = getSlot(e, "lavaSlot");

        switchSlotAndRightClick(lava);
    }

    // ── FXP — Fast XP (hold-to-run loop) ────────────────────────────────
    public static void runFXPLoop(int delay, BooleanSupplier active) {
        int d = Math.max(MIN_STEP_MS, delay);
        try {
            while (active.getAsBoolean() && check()) {
                rightClick();
                sleep(d);
            }
        } catch (InterruptedException ignored) {}
    }

    // ── AC — Auto Crystal (hold-to-run loop) ────────────────────────────
    public static void runACLoop(int crystalSlot, int delay, BooleanSupplier active) {
        int d = Math.max(MIN_STEP_MS, delay);
        try {
            if (crystalSlot >= 0) switchSlot(crystalSlot);
            sleep(5);

            while (active.getAsBoolean() && check()) {
                rightClick(); sleep(d); // place
                if (!active.getAsBoolean() || !check()) break;
                leftClick();  sleep(d); // hit
            }
        } catch (InterruptedException ignored) {}
    }
}
