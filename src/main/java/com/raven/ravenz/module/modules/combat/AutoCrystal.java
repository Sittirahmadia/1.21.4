package net.caffeinemc.phosphor.module.modules.combat;

import net.caffeinemc.phosphor.api.event.events.PlayerTickEvent;
import net.caffeinemc.phosphor.api.event.orbit.EventHandler;
import net.caffeinemc.phosphor.api.util.KeyUtils;
import net.caffeinemc.phosphor.common.Phosphor;
import net.caffeinemc.phosphor.module.Module;
import net.caffeinemc.phosphor.module.setting.settings.BooleanSetting;
import net.caffeinemc.phosphor.module.setting.settings.NumberSetting;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.*;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

public class AutoCrystalModule extends Module {

    private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", this, true);
    public final BooleanSetting onRmb = new BooleanSetting("On RMB", this, false);
    public final NumberSetting placeDelay = new NumberSetting("Place Delay", this, 1, 0, 10, 1);
    public final NumberSetting breakDelay = new NumberSetting("Break Delay", this, 1, 0, 10, 1);

    private int tickTimer;

    public AutoCrystalModule() {
        super("AutoCrystal", "Automatically places and breaks crystals", Category.COMBAT);
    }

    private boolean passedTicks(double time) {
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
        if (!passedTicks(placeDelay.getIValue()))
            return;

        if (!mc.player.getMainHandStack().isOf(Items.END_CRYSTAL))
            return;

        HitResult hit = getRaycast();

        if (hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();

            if (!canPlace(pos))
                return;

            BlockHitResult fixedHit = new BlockHitResult(
                    blockHit.getPos(),
                    blockHit.getSide(),
                    pos,
                    false
            );

            if (clickSimulation.isEnabled())
                Phosphor.mouseSimulation().mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

            // Fixed for 1.21.4: added mc.world parameter
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
    }

    private void breakCrystal() {
        if (!passedTicks(breakDelay.getIValue()))
            return;

        HitResult hit = getRaycast();

        if (hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof EndCrystalEntity crystal) {

                if (clickSimulation.isEnabled())
                    Phosphor.mouseSimulation().mouseClick(GLFW.GLFW_MOUSE_BUTTON_LEFT);

                mc.interactionManager.attackEntity(mc.player, crystal);
                mc.player.swingHand(Hand.MAIN_HAND);

                reset();
            }
        }
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {

        if (onRmb.isEnabled() && !KeyUtils.isKeyPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT))
            return;

        if (mc.currentScreen != null)
            return;

        placeCrystal();
        breakCrystal();

        tickTimer++;
    }
}
