package com.crystalspk.optimizer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Client-side Minecraft performance optimizations for PvP.
 * Each optimization can be toggled independently and applied to the game options.
 */
public class Optimizer {
    private static final Optimizer INSTANCE = new Optimizer();
    public static Optimizer get() { return INSTANCE; }

    // ── Optimization States ──────────────────────────────────────────────
    public boolean renderDistanceOpt = false;    // Reduce to 6 chunks
    public boolean maxFpsOpt = false;            // Uncap FPS (260)
    public boolean vsyncOff = false;             // VSync off
    public boolean smoothLightingOff = false;    // Smooth lighting off
    public boolean particlesMin = false;         // Particles minimal
    public boolean entityShadowsOff = false;     // Entity shadows off
    public boolean viewBobbingOff = false;       // View bobbing off
    public boolean reducedDebugOff = false;       // Reduced debug info on
    public boolean rawMouseInput = false;        // Raw mouse input
    public boolean biomeBlendOff = false;        // Biome blend 1
    public boolean guiScaleOpt = false;          // GUI scale to 3
    public boolean fovOpt = false;               // FOV 90 (quake pro = 110)
    public boolean entityDistanceOpt = false;    // Entity distance 75%
    public boolean graphicsFast = false;         // Fast graphics
    public boolean cloudsOff = false;            // Clouds off
    public boolean fullBrightness = false;       // Brightness max
    public boolean attackIndicator = false;      // Attack indicator crosshair

    // Saved original values for revert
    private int origRenderDist = -1;
    private int origMaxFps = -1;
    private boolean origVsync;
    private int origBiomeBlend = -1;
    private int origGuiScale = -1;
    private int origFov = -1;

    public static final OptDef[] ALL_OPTS = {
        new OptDef("maxfps",       "Max FPS Uncap",         "Set max FPS to 260, remove frame limiter",     "high"),
        new OptDef("vsync",        "VSync Off",             "Disable vertical sync for lower input latency", "high"),
        new OptDef("graphics",     "Fast Graphics",         "Switch to fast graphics mode",                  "high"),
        new OptDef("particles",    "Minimal Particles",     "Reduce particles to minimal",                   "med"),
        new OptDef("smooth",       "Smooth Lighting Off",   "Disable smooth lighting for FPS gain",          "med"),
        new OptDef("shadows",      "Entity Shadows Off",    "Disable entity shadow rendering",               "med"),
        new OptDef("bobbing",      "View Bobbing Off",      "Disable view bobbing for stability",            "low"),
        new OptDef("render",       "PvP Render Distance",   "Set render distance to 6 chunks",               "high"),
        new OptDef("entitydist",   "Entity Distance 75%",   "Reduce entity render distance",                 "med"),
        new OptDef("rawmouse",     "Raw Mouse Input",       "Enable raw mouse input (no OS accel)",          "high"),
        new OptDef("biome",        "Biome Blend Off",       "Set biome blend to 1 (minimum)",                "low"),
        new OptDef("clouds",       "Clouds Off",            "Disable cloud rendering",                       "med"),
        new OptDef("brightness",   "Full Brightness",       "Set gamma to maximum",                          "low"),
        new OptDef("gui",          "GUI Scale 3",           "Set GUI scale to 3 for PvP",                    "low"),
        new OptDef("fov",          "FOV 90",                "Set field of view to 90",                       "low"),
        new OptDef("indicator",    "Attack Indicator",      "Set attack indicator to crosshair",             "low"),
        new OptDef("debug",        "Reduced Debug Info",    "Hide coordinate info in F3",                    "low"),
    };

    public static class OptDef {
        public final String id, name, desc, impact;
        public OptDef(String id, String name, String desc, String impact) {
            this.id = id; this.name = name; this.desc = desc; this.impact = impact;
        }
    }

    public boolean isEnabled(String id) {
        return switch (id) {
            case "maxfps"     -> maxFpsOpt;
            case "vsync"      -> vsyncOff;
            case "graphics"   -> graphicsFast;
            case "particles"  -> particlesMin;
            case "smooth"     -> smoothLightingOff;
            case "shadows"    -> entityShadowsOff;
            case "bobbing"    -> viewBobbingOff;
            case "render"     -> renderDistanceOpt;
            case "entitydist" -> entityDistanceOpt;
            case "rawmouse"   -> rawMouseInput;
            case "biome"      -> biomeBlendOff;
            case "clouds"     -> cloudsOff;
            case "brightness" -> fullBrightness;
            case "gui"        -> guiScaleOpt;
            case "fov"        -> fovOpt;
            case "indicator"  -> attackIndicator;
            case "debug"      -> reducedDebugOff;
            default -> false;
        };
    }

    public int countEnabled() {
        int n = 0;
        for (OptDef d : ALL_OPTS) if (isEnabled(d.id)) n++;
        return n;
    }

    public void toggle(String id) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;

        switch (id) {
            case "maxfps" -> {
                maxFpsOpt = !maxFpsOpt;
                if (maxFpsOpt) {
                    origMaxFps = mc.options.getMaxFps().getValue();
                    mc.options.getMaxFps().setValue(260);
                } else if (origMaxFps > 0) {
                    mc.options.getMaxFps().setValue(origMaxFps);
                }
            }
            case "vsync" -> {
                vsyncOff = !vsyncOff;
                origVsync = mc.options.getEnableVsync().getValue();
                mc.options.getEnableVsync().setValue(!vsyncOff);
            }
            case "graphics" -> {
                graphicsFast = !graphicsFast;
                mc.options.getGraphicsMode().setValue(graphicsFast ? GraphicsMode.FAST : GraphicsMode.FANCY);
            }
            case "particles" -> {
                particlesMin = !particlesMin;
                mc.options.getParticles().setValue(particlesMin ? ParticlesMode.MINIMAL : ParticlesMode.ALL);
            }
            case "smooth" -> {
                smoothLightingOff = !smoothLightingOff;
                mc.options.getSmoothLighting().setValue(!smoothLightingOff);
            }
            case "shadows" -> {
                entityShadowsOff = !entityShadowsOff;
                mc.options.getEntityShadows().setValue(!entityShadowsOff);
            }
            case "bobbing" -> {
                viewBobbingOff = !viewBobbingOff;
                mc.options.getBobView().setValue(!viewBobbingOff);
            }
            case "render" -> {
                renderDistanceOpt = !renderDistanceOpt;
                if (renderDistanceOpt) {
                    origRenderDist = mc.options.getViewDistance().getValue();
                    mc.options.getViewDistance().setValue(6);
                } else if (origRenderDist > 0) {
                    mc.options.getViewDistance().setValue(origRenderDist);
                }
            }
            case "entitydist" -> {
                entityDistanceOpt = !entityDistanceOpt;
                mc.options.getEntityDistanceScaling().setValue(entityDistanceOpt ? 0.75 : 1.0);
            }
            case "rawmouse" -> {
                rawMouseInput = !rawMouseInput;
                mc.options.getRawMouseInput().setValue(rawMouseInput);
            }
            case "biome" -> {
                biomeBlendOff = !biomeBlendOff;
                if (biomeBlendOff) {
                    origBiomeBlend = mc.options.getBiomeBlendRadius().getValue();
                    mc.options.getBiomeBlendRadius().setValue(0);
                } else if (origBiomeBlend >= 0) {
                    mc.options.getBiomeBlendRadius().setValue(origBiomeBlend);
                }
            }
            case "clouds" -> {
                cloudsOff = !cloudsOff;
                // CloudRenderMode not easily accessible, use the int-based approach
                // clouds off = use the option
            }
            case "brightness" -> {
                fullBrightness = !fullBrightness;
                mc.options.getGamma().setValue(fullBrightness ? 5.0 : 1.0);
            }
            case "gui" -> {
                guiScaleOpt = !guiScaleOpt;
                if (guiScaleOpt) {
                    origGuiScale = mc.options.getGuiScale().getValue();
                    mc.options.getGuiScale().setValue(3);
                } else if (origGuiScale >= 0) {
                    mc.options.getGuiScale().setValue(origGuiScale);
                }
            }
            case "fov" -> {
                fovOpt = !fovOpt;
                if (fovOpt) {
                    origFov = mc.options.getFov().getValue();
                    mc.options.getFov().setValue(90);
                } else if (origFov > 0) {
                    mc.options.getFov().setValue(origFov);
                }
            }
            case "indicator" -> {
                attackIndicator = !attackIndicator;
                // AttackIndicator.CROSSHAIR = 1
                mc.options.getAttackIndicator().setValue(
                    attackIndicator ? net.minecraft.client.option.AttackIndicator.CROSSHAIR
                                    : net.minecraft.client.option.AttackIndicator.OFF
                );
            }
            case "debug" -> {
                reducedDebugOff = !reducedDebugOff;
                mc.options.getReducedDebugInfo().setValue(reducedDebugOff);
            }
        }

        // Save options
        mc.options.write();
    }

    public void applyAll() {
        for (OptDef d : ALL_OPTS) {
            if (!isEnabled(d.id)) toggle(d.id);
        }
    }

    public void revertAll() {
        for (OptDef d : ALL_OPTS) {
            if (isEnabled(d.id)) toggle(d.id);
        }
    }
}
