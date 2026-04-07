package com.crystalspk.optimizer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.AttackIndicator;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/**
 * Client-side Minecraft performance optimizations for PvP (improved for 1.21.4+).
 * 
 * Fixed: All braces are now correctly closed → no more "reached end of file while parsing" error.
 */
public class Optimizer {
    private static final Optimizer INSTANCE = new Optimizer();
    public static Optimizer get() { return INSTANCE; }

    // ── Optimization States ──────────────────────────────────────────────
    public boolean renderDistanceOpt = false;
    public boolean maxFpsOpt = false;
    public boolean vsyncOff = false;
    public boolean particlesMin = false;
    public boolean entityShadowsOff = false;
    public boolean viewBobbingOff = false;
    public boolean reducedDebugOff = false;
    public boolean rawMouseInput = false;
    public boolean biomeBlendOff = false;
    public boolean guiScaleOpt = false;
    public boolean fovOpt = false;
    public boolean entityDistanceOpt = false;
    public boolean graphicsFast = false;
    public boolean cloudsOff = false;
    public boolean fullBrightness = false;
    public boolean attackIndicator = false;

    // New high-FPS options (1.21.4+)
    public boolean simulationDistanceOpt = false;
    public boolean mipmapLevelsOpt = false;

    // Saved original values
    private int origRenderDist = -1;
    private int origMaxFps = -1;
    private boolean origVsync;
    private int origBiomeBlend = -1;
    private int origGuiScale = -1;
    private int origFov = -1;
    private CloudRenderMode origClouds;
    private double origGamma = -1.0;
    private int origSimulationDistance = -1;
    private int origMipmapLevels = -1;

    public static final OptDef[] ALL_OPTS = {
        new OptDef("maxfps",       "Max FPS Uncap",         "Set max FPS to 260 (unlimited)",               "high"),
        new OptDef("vsync",        "VSync Off",             "Disable vertical sync for lower input latency", "high"),
        new OptDef("graphics",     "Fast Graphics",         "Switch to fast graphics mode",                  "high"),
        new OptDef("particles",    "Minimal Particles",     "Reduce particles to minimal",                   "med"),
        new OptDef("shadows",      "Entity Shadows Off",    "Disable entity shadow rendering",               "med"),
        new OptDef("bobbing",      "View Bobbing Off",      "Disable view bobbing for stability",            "low"),
        new OptDef("render",       "PvP Render Distance",   "Set render distance to 6 chunks",               "high"),
        new OptDef("entitydist",   "Entity Distance 75%",   "Reduce entity render distance",                 "med"),
        new OptDef("rawmouse",     "Raw Mouse Input",       "Enable raw mouse input (no OS accel)",          "high"),
        new OptDef("biome",        "Biome Blend Off",       "Set biome blend to 0 (disabled)",               "low"),
        new OptDef("clouds",       "Clouds Off",            "Disable cloud rendering",                       "med"),
        new OptDef("brightness",   "Full Brightness",       "Set gamma to fullbright (5.0)",                "low"),
        new OptDef("gui",          "GUI Scale 3",           "Set GUI scale to 3 for PvP",                    "low"),
        new OptDef("fov",          "FOV 90",                "Set field of view to 90",                       "low"),
        new OptDef("indicator",    "Attack Indicator",      "Set attack indicator to crosshair",             "low"),
        new OptDef("debug",        "Reduced Debug Info",    "Hide coordinate info in F3",                    "low"),
        new OptDef("simdist",      "Simulation Distance 6", "Lower simulation distance (less ticking load)", "high"),
        new OptDef("mipmap",       "Mipmap Levels 0",       "Disable mipmapping (big FPS boost)",           "med"),
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
            case "simdist"    -> simulationDistanceOpt;
            case "mipmap"     -> mipmapLevelsOpt;
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
                if (vsyncOff) {
                    origVsync = mc.options.getEnableVsync().getValue();
                    mc.options.getEnableVsync().setValue(false);
                } else {
                    mc.options.getEnableVsync().setValue(origVsync);
                }
            }
            case "graphics" -> {
                graphicsFast = !graphicsFast;
                mc.options.getGraphicsMode().setValue(graphicsFast ? GraphicsMode.FAST : GraphicsMode.FANCY);
            }
            case "particles" -> {
                particlesMin = !particlesMin;
                mc.options.getParticles().setValue(particlesMin ? ParticlesMode.MINIMAL : ParticlesMode.ALL);
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
                if (cloudsOff) {
                    origClouds = mc.options.getCloudRenderMode().getValue();
                    mc.options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                } else if (origClouds != null) {
                    mc.options.getCloudRenderMode().setValue(origClouds);
                }
            }
            case "brightness" -> {
                fullBrightness = !fullBrightness;
                if (fullBrightness) {
                    origGamma = mc.options.getGamma().getValue();
                    mc.options.getGamma().setValue(5.0);
                } else if (origGamma != -1.0) {
                    mc.options.getGamma().setValue(origGamma);
                }
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
                mc.options.getAttackIndicator().setValue(
                    attackIndicator ? AttackIndicator.CROSSHAIR : AttackIndicator.OFF
                );
            }
            case "debug" -> {
                reducedDebugOff = !reducedDebugOff;
                mc.options.getReducedDebugInfo().setValue(reducedDebugOff);
            }
            case "simdist" -> {
                simulationDistanceOpt = !simulationDistanceOpt;
                if (simulationDistanceOpt) {
                    origSimulationDistance = mc.options.getSimulationDistance().getValue();
                    mc.options.getSimulationDistance().setValue(6);
                } else if (origSimulationDistance > 0) {
                    mc.options.getSimulationDistance().setValue(origSimulationDistance);
                }
            }
            case "mipmap" -> {
                mipmapLevelsOpt = !mipmapLevelsOpt;
                if (mipmapLevelsOpt) {
                    origMipmapLevels = mc.options.getMipmapLevels().getValue();
                    mc.options.getMipmapLevels().setValue(0);
                } else if (origMipmapLevels >= 0) {
                    mc.options.getMipmapLevels().setValue(origMipmapLevels);
                }
            }
        }

        // Save options to disk
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
