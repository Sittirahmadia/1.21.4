package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.ModeSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import com.raven.ravenz.module.setting.RangeSetting;
import com.raven.ravenz.utils.math.TimerUtil;
import com.raven.ravenz.utils.mc.CombatUtil;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;

public class ShieldBreaker extends Module {

    public static boolean breakingShield = false;

    // Delay settings
    private final RangeSetting reactionDelay   = new RangeSetting("Reaction Time",   0, 250, 10, 25, 1);
    private final RangeSetting swapDelay       = new RangeSetting("Swap Delay",      0, 500, 10, 25, 1);
    private final RangeSetting attackDelay     = new RangeSetting("Attack Delay",    0, 500, 10, 25, 1);
    private final RangeSetting swapBackDelay   = new RangeSetting("Swap Back Delay", 0, 500, 10, 25, 1);

    // Behavior settings
    private final ModeSetting swap             = new ModeSetting("Swap Mode", "Swap", "Swap", "None");
    private final BooleanSetting unblockShield = new BooleanSetting("Unblock Own Shield", false);

    private int    oldSlot     = -1;
    private boolean unblocked  = false;

    private final TimerUtil reactionTimer   = new TimerUtil();
    private final TimerUtil initialSwapTimer = new TimerUtil();
    private final TimerUtil actionTimer     = new TimerUtil();
    private final TimerUtil swapBackTimer   = new TimerUtil();

    public ShieldBreaker() {
        super("Shield Breaker", "Stuns enemies by axe-swapping their shield", -1, Category.COMBAT);
        addSettings(reactionDelay, swapDelay, attackDelay, swapBackDelay, swap, unblockShield);
    }

    @Override
    public void onDisable() {
        oldSlot   = -1;
        unblocked = false;
        super.onDisable();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (isNull() || mc.currentScreen != null) return;

        if (!(mc.crosshairTarget instanceof EntityHitResult entityHit)) {
            initialSwapTimer.reset();
            return;
        }
        if (!(entityHit.getEntity() instanceof PlayerEntity target)) {
            initialSwapTimer.reset();
            return;
        }

        boolean isBlocking    = target.isUsingItem() && target.getActiveItem().getItem() instanceof ShieldItem;
        boolean canBreakShield = !CombatUtil.isShieldFacingAway(target);

        // Unblock own shield so axe attacks land
        if (unblockShield.getValue()
                && mc.player.getActiveItem().getItem() instanceof ShieldItem
                && mc.player.getActiveHand() == Hand.OFF_HAND) {
            mc.options.useKey.setPressed(false);
            unblocked = true;
        }

        if (isBlocking && canBreakShield) {
            var mainHand = mc.player.getMainHandStack();
            if (mainHand.getItem() instanceof AxeItem) {
                // Already holding axe — attack directly
                if (actionTimer.hasElapsedTime(getRandomLong(attackDelay), false)) {
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    actionTimer.reset();
                    swapBackTimer.reset();
                }
            } else {
                // Need to swap to axe first
                if (swap.getMode().equals("None")) return;
                if (!reactionTimer.hasElapsedTime(getRandomLong(reactionDelay), false)) return;

                int axeSlot = findAxeInHotbar();
                breakingShield = true;
            if (axeSlot != -1 && oldSlot == -1
                        && initialSwapTimer.hasElapsedTime(getRandomLong(swapDelay), false)) {
                    oldSlot = mc.player.getInventory().selectedSlot;
                    mc.player.getInventory().selectedSlot = axeSlot;
                    actionTimer.reset();
                }
            }
        } else {
            breakingShield = false;
            reactionTimer.reset();
            // Swap back
            if (oldSlot != -1 && swapBackTimer.hasElapsedTime(getRandomLong(swapBackDelay), false)) {
                mc.player.getInventory().selectedSlot = oldSlot;
                oldSlot = -1;
                if (unblocked) reblockShield();
            }
        }
    }

    private void reblockShield() {
        if (mc.player == null) return;
        if (mc.player.getOffHandStack().getItem() == Items.SHIELD) {
            mc.options.useKey.setPressed(true);
            unblocked = false;
        }
    }

    private int findAxeInHotbar() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private long getRandomLong(RangeSetting setting) {
        double min = setting.getMinValue();
        double max = setting.getMaxValue();
        return (long) (min + Math.random() * (max - min));
    }
}
