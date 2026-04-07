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
 * Macro implementations using split slot-switch + click pattern.
 * Each step is: switchSlot() → sleep(SWITCH_GAP) → click() → sleep(STEP_GAP)
 * This ensures the slot change is processed in one game tick before the click fires
 * in the next tick — preventing wrong-item-placed bugs at any delay setting.
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

    // ── Timing Constants ─────────────────────────────────────────────────
    // SWITCH_GAP: time between slot switch and the click that follows.
    // Must be >= 55ms (just over 1 MC tick = 50ms) so the slot change
    // is processed before the click fires.
    private static final int SWITCH_GAP = 55;

    // STEP_GAP: minimum time between a click and the next slot switch.
    // Ensures MC processes the click before we switch away.
    private static final int STEP_GAP = 40;

    // ── Input helpers ────────────────────────────────────────────────────

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
     * Split slot-switch + right-click: switch slot, wait SWITCH_GAP, then click.
     * This guarantees the slot is registered in MC before the click fires.
     */
    private static void slotThenRightClick(int slot) throws InterruptedException {
        switchSlot(slot);
        sleep(SWITCH_GAP);
        rightClick();
    }

    /**
     * Split slot-switch + left-click.
     */
    private static void slotThenLeftClick(int slot) throws InterruptedException {
        switchSlot(slot);
        sleep(SWITCH_GAP);
        leftClick();
    }

    private static int getSlot(MacroConfig.MacroEntry entry, String name) {
        return entry.slots.getOrDefault(name, -1);
    }

    // ── Dispatch ─────────────────────────────────────────────────────────

    public static void dispatch(String id, MacroConfig.MacroEntry entry) {
        AtomicBoolean flag = new AtomicBoolean(true);
        if (running.putIfAbsent(id, flag) != null) return;

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
    // Tuned per-step timings for fastest reliable execution.
    // anchor=18ms, glowstone=26ms, explode=32ms
    private static void runSA(MacroConfig.MacroEntry e) throws InterruptedException {
        int anchor = getSlot(e, "anchorSlot");
        int glowstone = getSlot(e, "glowstoneSlot");
        int explode = getSlot(e, "explodeSlot");
        int det = explode >= 0 ? explode : anchor;

        // 1. Switch to anchor → 18ms → place
        switchSlot(anchor);
        sleep(18);
        rightClick();
        sleep(26); if (!check()) return;

        // 2. Switch to glowstone → 26ms → charge
        switchSlot(glowstone);
        sleep(26);
        rightClick();
        sleep(32); if (!check()) return;

        // 3. Switch to detonate → 32ms → explode
        switchSlot(det);
        sleep(32);
        rightClick();
    }

    // ── DA — Double Anchor ───────────────────────────────────────────────
    // anchor=25ms, glowstone=30ms, explode with anchor=35ms
    // After 1st anchor explodes: immediately double rclick to place 2nd at same spot
    // Then charge and explode the 2nd anchor
    private static void runDA(MacroConfig.MacroEntry e) throws InterruptedException {
        int anchor = getSlot(e, "anchorSlot");
        int glowstone = getSlot(e, "glowstoneSlot");
        int explode = getSlot(e, "explodeSlot");
        int det = explode >= 0 ? explode : anchor;

        // === FIRST ANCHOR ===
        // 1. Switch to anchor → 25ms → place
        switchSlot(anchor);
        sleep(25);
        rightClick();
        sleep(30); if (!check()) return;

        // 2. Switch to glowstone → 30ms → charge
        switchSlot(glowstone);
        sleep(30);
        rightClick();
        sleep(35); if (!check()) return;

        // 3. Switch to anchor → 35ms → detonate 1st (rclick charged anchor = explode)
        switchSlot(anchor);
        sleep(35);
        rightClick();

        // 4. Immediately double rclick to place 2nd anchor at explosion spot
        //    Already on anchor slot — just spam rclick fast
        sleep(10); if (!check()) return;
        rightClick();
        sleep(10);
        rightClick();
        sleep(30); if (!check()) return;

        // === SECOND ANCHOR (now placed) ===
        // 5. Switch to glowstone → 30ms → charge 2nd
        switchSlot(glowstone);
        sleep(30);
        rightClick();
        sleep(35); if (!check()) return;

        // 6. Switch to explode slot → 35ms → detonate 2nd
        switchSlot(det);
        sleep(35);
        rightClick();
    }

    // ── AP — Anchor Pearl ────────────────────────────────────────────────
    private static void runAP(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int anchor = getSlot(e, "anchorSlot");
        int glowstone = getSlot(e, "glowstoneSlot");
        int explode = getSlot(e, "explodeSlot");
        int pearl = getSlot(e, "pearlSlot");
        int det = explode >= 0 ? explode : anchor;

        // SA sequence
        slotThenRightClick(anchor);
        sleep(stepGap); if (!check()) return;
        slotThenRightClick(glowstone);
        sleep(stepGap); if (!check()) return;
        slotThenRightClick(det);
        sleep(stepGap); if (!check()) return;

        // Pearl throw
        slotThenRightClick(pearl);
    }

    // ── HC — Hit Crystal ─────────────────────────────────────────────────
    private static void runHC(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int fastGap = Math.max(STEP_GAP, stepGap / 2);
        int obsidian = getSlot(e, "obsidianSlot");
        int crystal = getSlot(e, "crystalSlot");

        // 1. Place obsidian
        slotThenRightClick(obsidian);
        sleep(stepGap); if (!check()) return;

        // 2. Switch to crystal
        switchSlot(crystal);
        sleep(SWITCH_GAP); if (!check()) return;

        // 3. Place crystal → hit → place → hit
        rightClick(); sleep(fastGap); if (!check()) return;
        leftClick();  sleep(fastGap); if (!check()) return;
        rightClick(); sleep(fastGap); if (!check()) return;
        leftClick();
    }

    // ── KP — Key Pearl ───────────────────────────────────────────────────
    private static void runKP(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int pearl = getSlot(e, "pearlSlot");
        int ret = getSlot(e, "returnSlot");

        slotThenRightClick(pearl);
        sleep(stepGap); if (!check()) return;
        switchSlot(ret);
    }

    // ── IDH — Inventory D-Hand ───────────────────────────────────────────
    private static void runIDH(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int totem = getSlot(e, "totemSlot");
        int swap = getSlot(e, "swapSlot");

        switchSlot(totem);
        sleep(SWITCH_GAP); if (!check()) return;

        if (swap >= 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    KeyBindingAccessor acc = (KeyBindingAccessor) mc.options.swapHandsKey;
                    acc.setTimesPressed(acc.getTimesPressed() + 1);
                }
            });
            sleep(stepGap); if (!check()) return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                KeyBindingAccessor acc = (KeyBindingAccessor) mc.options.inventoryKey;
                acc.setTimesPressed(acc.getTimesPressed() + 1);
            }
        });
    }

    // ── OHT — Offhand Totem ──────────────────────────────────────────────
    private static void runOHT(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int totem = getSlot(e, "totemSlot");

        switchSlot(totem);
        sleep(SWITCH_GAP); if (!check()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                KeyBindingAccessor acc = (KeyBindingAccessor) mc.options.swapHandsKey;
                acc.setTimesPressed(acc.getTimesPressed() + 1);
            }
        });
    }

    // ── ASB — Auto Shield Breaker ────────────────────────────────────────
    private static void runASB(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int axe = getSlot(e, "axeSlot");
        int sword = getSlot(e, "swordSlot");

        slotThenLeftClick(axe);
        sleep(stepGap); if (!check()) return;
        switchSlot(sword);
    }

    // ── SR — Sprint Reset ────────────────────────────────────────────────
    private static void runSR(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);

        leftClick();
        sleep(stepGap); if (!check()) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> ((KeyBindingAccessor) mc.options.forwardKey).setPressed(true));
        sleep(15); if (!check()) return;
        mc.execute(() -> ((KeyBindingAccessor) mc.options.forwardKey).setPressed(false));
        sleep(15); if (!check()) return;
        mc.execute(() -> ((KeyBindingAccessor) mc.options.forwardKey).setPressed(true));
        sleep(15); if (!check()) return;
        mc.execute(() -> ((KeyBindingAccessor) mc.options.forwardKey).setPressed(false));
    }

    // ── LS — Lunge Swap ──────────────────────────────────────────────────
    private static void runLS(MacroConfig.MacroEntry e) throws InterruptedException {
        int sword = getSlot(e, "swordSlot");
        int spear = getSlot(e, "spearSlot");

        switchSlot(sword);
        sleep(SWITCH_GAP); if (!check()) return;
        slotThenLeftClick(spear);
        sleep(SWITCH_GAP); if (!check()) return;
        switchSlot(sword);
    }

    // ── ES — Elytra Swap ─────────────────────────────────────────────────
    private static void runES(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int elytra = getSlot(e, "elytraSlot");
        int ret = getSlot(e, "returnSlot");

        slotThenRightClick(elytra);
        sleep(stepGap); if (!check()) return;
        switchSlot(ret);
    }

    // ── PC — Pearl Catch ─────────────────────────────────────────────────
    private static void runPC(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int pearl = getSlot(e, "pearlSlot");
        int wind = getSlot(e, "windChargeSlot");

        slotThenRightClick(pearl);
        sleep(stepGap); if (!check()) return;
        slotThenRightClick(wind);
    }

    // ── SS — Stun Slam ───────────────────────────────────────────────────
    private static void runSS(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int axe = getSlot(e, "axeSlot");
        int mace = getSlot(e, "maceSlot");

        slotThenLeftClick(axe);
        sleep(stepGap); if (!check()) return;
        slotThenLeftClick(mace);
    }

    // ── BS — Breach Swap ─────────────────────────────────────────────────
    private static void runBS(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int mace = getSlot(e, "maceSlot");
        int sword = getSlot(e, "swordSlot");

        slotThenLeftClick(mace);
        sleep(stepGap); if (!check()) return;
        switchSlot(sword);
    }

    // ── IC — Insta Cart ──────────────────────────────────────────────────
    private static void runIC(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int rail = getSlot(e, "railSlot");
        int bow = getSlot(e, "bowSlot");
        int cart = getSlot(e, "cartSlot");

        slotThenRightClick(rail);
        sleep(stepGap); if (!check()) return;

        // Draw bow
        switchSlot(bow);
        sleep(SWITCH_GAP); if (!check()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> ((KeyBindingAccessor) mc.options.useKey).setPressed(true));
        sleep(150);
        mc.execute(() -> ((KeyBindingAccessor) mc.options.useKey).setPressed(false));
        sleep(stepGap); if (!check()) return;

        slotThenRightClick(cart);
    }

    // ── XB — Crossbow Cart ───────────────────────────────────────────────
    private static void runXB(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int rail = getSlot(e, "railSlot");
        int cart = getSlot(e, "cartSlot");
        int fns = getSlot(e, "fnsSlot");
        int crossbow = getSlot(e, "crossbowSlot");

        slotThenRightClick(rail);     sleep(stepGap); if (!check()) return;
        slotThenRightClick(cart);     sleep(stepGap); if (!check()) return;
        slotThenRightClick(fns);      sleep(stepGap); if (!check()) return;
        slotThenRightClick(crossbow);
    }

    // ── DR — Drain ───────────────────────────────────────────────────────
    private static void runDR(MacroConfig.MacroEntry e) throws InterruptedException {
        slotThenRightClick(getSlot(e, "bucketSlot"));
    }

    // ── LW — Lava Web ───────────────────────────────────────────────────
    private static void runLW(MacroConfig.MacroEntry e) throws InterruptedException {
        int stepGap = Math.max(STEP_GAP, e.delay);
        int lava = getSlot(e, "lavaSlot");
        int cobweb = getSlot(e, "cobwebSlot");

        slotThenRightClick(lava);       // place lava
        sleep(stepGap); if (!check()) return;
        rightClick();                    // pick lava back up
        sleep(stepGap); if (!check()) return;
        slotThenRightClick(cobweb);      // place cobweb
    }

    // ── LA — Lava ────────────────────────────────────────────────────────
    private static void runLA(MacroConfig.MacroEntry e) throws InterruptedException {
        slotThenRightClick(getSlot(e, "lavaSlot"));
    }

    // ── FXP — Fast XP (hold-to-run loop) ─────────────────────────────────
    public static void runFXPLoop(int delay, BooleanSupplier active) {
        int d = Math.max(STEP_GAP, delay);
        try {
            while (active.getAsBoolean() && check()) {
                rightClick();
                sleep(d);
            }
        } catch (InterruptedException ignored) {}
    }

    // ── AC — Auto Crystal (hold-to-run loop) ─────────────────────────────
    public static void runACLoop(int crystalSlot, int delay, BooleanSupplier active) {
        int d = Math.max(STEP_GAP, delay);
        try {
            if (crystalSlot >= 0) switchSlot(crystalSlot);
            sleep(SWITCH_GAP);

            while (active.getAsBoolean() && check()) {
                rightClick(); sleep(d);
                if (!active.getAsBoolean() || !check()) break;
                leftClick();  sleep(d);
            }
        } catch (InterruptedException ignored) {}
    }
}
