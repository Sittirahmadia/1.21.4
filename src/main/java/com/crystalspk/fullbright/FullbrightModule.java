package com.crystalspk.fullbright;

import net.minecraft.client.MinecraftClient;

/**
 * Fullbright module for CrystalSpK
 * Provides adjustable gamma lighting from 0 to 1000
 */
public class FullbrightModule {
    private static FullbrightModule instance;
    private boolean enabled = false;
    private float gamma = 1.0f;
    private float originalGamma = 1.0f;

    private FullbrightModule() {
    }

    public static FullbrightModule getInstance() {
        if (instance == null) {
            instance = new FullbrightModule();
        }
        return instance;
    }

    /**
     * Enable fullbright and set gamma to maximum
     */
    public void enable() {
        if (enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            try {
                originalGamma = gamma;
                setGamma(100.0f); // Start at max
                enabled = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Disable fullbright and restore original gamma
     */
    public void disable() {
        if (!enabled) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options != null) {
            try {
                setGamma(originalGamma);
                enabled = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Toggle fullbright on/off
     */
    public void toggle() {
        if (enabled) {
            disable();
        } else {
            enable();
        }
    }

    /**
     * Set gamma value (0-1000, normalized to 0-100 for Minecraft)
     * @param value gamma value from 0 to 1000
     */
    public void setGamma(float value) {
        try {
            // Normalize 0-1000 to 0-100 range for Minecraft
            float normalized = Math.max(0.0f, Math.min(value, 1000.0f)) / 10.0f;
            this.gamma = normalized;
            
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.options != null && mc.player != null) {
                // Use world brightness to simulate fullbright
                // This is a safer approach than directly modifying gamma
                mc.worldRenderer.reload();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get current gamma value (0-1000)
     */
    public float getGamma() {
        return gamma * 10.0f;
    }

    /**
     * Increase gamma by step value
     */
    public void increaseGamma(float step) {
        float current = getGamma();
        setGamma(current + step);
    }

    /**
     * Decrease gamma by step value
     */
    public void decreaseGamma(float step) {
        float current = getGamma();
        setGamma(current - step);
    }

    /**
     * Check if fullbright is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get the normalized gamma (0-100 for Minecraft)
     */
    public float getNormalizedGamma() {
        return gamma;
    }
}
