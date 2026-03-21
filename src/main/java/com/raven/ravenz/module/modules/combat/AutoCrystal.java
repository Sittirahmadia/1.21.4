package com.raven.ravenz.module.modules.combat;

import com.raven.ravenz.event.impl.player.TickEvent;
import com.raven.ravenz.module.Category;
import com.raven.ravenz.module.Module;
import com.raven.ravenz.module.setting.BooleanSetting;
import com.raven.ravenz.module.setting.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class AutoCrystal extends Module {

    private final BooleanSetting onRmb = new BooleanSetting("On RMB", false);
    private final NumberSetting placeDelay = new NumberSetting("Place Delay", 0, 10, 1, 1);
    private final NumberSetting breakDelay = new NumberSetting("Break Delay", 0, 10, 1, 1);

    private int tickTimer = 0;

    public AutoCrystal() {
        super("AutoCrystal", "Automatically places and breaks crystals", Category.COMBAT);
        addSettings(onRmb, placeDelay, breakDelay);
    }

    public void onTick(TickEvent event) {
        if (isNull()) return;
        if (mc.currentScreen != null) return;

        if (onRmb.getValue()) {
            long window = mc.getWindow().getHandle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) != GLFW.GLFW_PRESS)
                return;
        }

        placeCrystal();
        breakCrystal();
        tickTimer++;
    }

    private boolean passedTicks(int time) {
        return tickTimer >= time;
    }

    private void reset() {
        tickTimer = 0;
    }

    private HitResult getRaycast() {
        HitResult hit = mc.crosshairTarget;
        if (hit == null || hit.getType() == HitResult.Type.MISS) {
            hit = mc.player.raycast(4.5, mc.getTickDelta(), false);
        }
        return hit;
    }

    private boolean canPlace(BlockPos pos) {
        BlockState base = mc.world.getBlockState(pos);
        if (!(base.isOf(Blocks.OBSIDIAN) || base.isOf(Blocks.BEDROCK)))
            return false;
        return mc.world.isAir(pos.up()) && mc.world.isAir(pos.up(2));
    }

    private void placeCrystal() {
        if (!passedTicks(placeDelay.getValueInt())) return;
        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL)) return;

        HitResult hit = getRaycast();
        if (!(hit instanceof BlockHitResult blockHit)) return;

        BlockPos pos = blockHit.getBlockPos();
        if (!canPlace(pos)) return;

        BlockHitResult fixedHit = new BlockHitResult(
                blockHit.getPos(),
                blockHit.getSide(),
                pos,
                false
        );

        // Fixed for 1.21.4: mc.world as second parameter
        ActionResult result = mc.interactionManager.interactBlock(
                mc.player,
                mc.world,
                Hand.MAIN_HAND,
                fixedHit
        );

        if (result.isAccepted())
            mc.player.swingHand(Hand.MAIN_HAND);

        reset();
    }

    private void breakCrystal() {
        if (!passedTicks(breakDelay.getValueInt())) return;

        HitResult hit = getRaycast();
        if (!(hit instanceof EntityHitResult entityHit)) return;

        if (entityHit.getEntity() instanceof EndCrystalEntity crystal) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            reset();
        }
    }
}
