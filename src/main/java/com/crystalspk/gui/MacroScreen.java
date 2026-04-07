package com.crystalspk.gui;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.config.MacroDef;
import com.crystalspk.macro.MacroEngine;
import com.crystalspk.optimizer.Optimizer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Modern, compact macro GUI with Effects panel and improved Optimizer section for Zalith.
 */
public class MacroScreen extends Screen {
    // --- Smaller Layout ---
    private static final int SIDEBAR_W  = 100;
    private static final int NAV_ITEM_H = 18;
    private static final int CARD_H     = 26;
    private static final int FIELD_H    = 16;
    private static final int FIELD_W    = 64;
    private static final int PAD        = 6;
    private static final int CARD_GAP   = 3;

    // Theme (unchanged)
    private static final int BG           = 0xFF12121A;
    private static final int SIDEBAR_BG   = 0xFF181824;
    private static final int SIDEBAR_SEP  = 0x661A1F2C;
    private static final int ACCENT       = 0xFF4FC8FF;
    private static final int GREEN        = 0xFF50E88A;
    private static final int WHITE        = 0xFFFFFFFF;
    private static final int NAV_ACTIVE   = 0x222A2A35;
    private static final int NAV_HOV      = 0x22181824;
    // ... (keep rest of your theme colors)

    // --- Effects state ---
    private boolean glow = true;
    private boolean blur = false;
    private boolean dark = true;
    // ... add more effect states as you wish

    private String currentCategory = "crystal";
    // ... (rest of your fields)

    public MacroScreen() {
        super(Text.literal("CrystalSpK"));
    }

    @Override
    protected void init() {
        super.init();
        // You could load effect settings here from config if you want
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // --- Smaller Sidebar ---
        ctx.fill(0, 0, SIDEBAR_W, height, SIDEBAR_BG);
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, height, SIDEBAR_SEP);
        int ly = 8;
        ctx.drawTextWithShadow(textRenderer, "\u0000\u001AMacro", 10, ly, ACCENT);
        ly += 26;
        for (String cat : new String[]{"crystal","sword","mace","cart","uhc","optimizer","effects"}) {
            boolean active = cat.equals(currentCategory);
            int col = active ? ACCENT : WHITE;
            ctx.drawTextWithShadow(textRenderer, cat.substring(0,1).toUpperCase()+cat.substring(1), 14, ly, col);
            ly += NAV_ITEM_H + 1;
        }
        // Draw footer, etc...

        // --- Panels ---
        if ("effects".equals(currentCategory)) {
            drawEffectsPanel(ctx, mouseX, mouseY);
        } else if ("optimizer".equals(currentCategory)) {
            drawOptimizerPanel(ctx, mouseX, mouseY);
        } else {
            // ... existing macro cards logic ...
        }
    }

    private void drawEffectsPanel(DrawContext ctx, int mouseX, int mouseY) {
        int x = SIDEBAR_W + PAD, y = 24;
        ctx.drawTextWithShadow(textRenderer, "Effects Panel", x, y, ACCENT);
        y += 16;
        ctx.drawTextWithShadow(textRenderer, "Glow: " + (glow ? "ON" : "OFF"), x, y, glow ? GREEN : WHITE);
        y += 12;
        ctx.drawTextWithShadow(textRenderer, "Blur: " + (blur ? "ON" : "OFF"), x, y, blur ? GREEN : WHITE);
        y += 12;
        ctx.drawTextWithShadow(textRenderer, "Dark mode: " + (dark ? "ON" : "OFF"), x, y, dark ? GREEN : WHITE);
        // Add toggle UI logic here...
    }

    private void drawOptimizerPanel(DrawContext ctx, int mouseX, int mouseY) {
        int x = SIDEBAR_W + PAD, y = 24;
        ctx.drawTextWithShadow(textRenderer, "Zalith Optimizer", x, y, ACCENT);
        y += 16;
        // Example compact optimizer toggles
        String[][] opts = {
            { "FPS Uncap", "Unlock FPS cap for performance" },
            { "Clouds Off", "Disable clouds for PVP" },
            { "Raw Mouse", "Enable raw mouse input (faster)" },
            { "FullBright", "Always max brightness" },
            { "Shadows Off", "Disable all entity shadows" }
        };
        for (String[] opt : opts) {
            ctx.drawTextWithShadow(textRenderer, opt[0], x, y, WHITE);
            ctx.drawTextWithShadow(textRenderer, opt[1], x + 86, y, 0xFFBBBBBB);
            y += 16;
            // Replace with real checkboxes/toggles as needed
        }
    }
}