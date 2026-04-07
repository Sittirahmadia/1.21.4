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
 * Improved 3D-style GUI for CrystalSpK.
 * Fixes module visibility and adds depth effects.
 */
public class MacroScreen extends Screen {
    // --- Layout Constants ---
    private static final int SIDEBAR_W  = 110;
    private static final int NAV_ITEM_H = 22;
    private static final int CARD_W     = 180;
    private static final int CARD_H     = 45;
    private static final int PAD        = 10;
    private static final int CARD_GAP   = 8;

    // --- 3D Theme Colors ---
    private static final int BG           = 0xEE0A0A0F; // Deep dark semi-transparent
    private static final int SIDEBAR_BG   = 0xFF12121B;
    private static final int CARD_BG      = 0xFF1A1A26;
    private static final int CARD_BORDER  = 0xFF2A2A3A;
    private static final int CARD_SHADOW  = 0xFF050508;
    private static final int ACCENT       = 0xFF4FC8FF;
    private static final int ACCENT_GLOW  = 0x444FC8FF;
    private static final int GREEN        = 0xFF50E88A;
    private static final int RED          = 0xFFFF5555;
    private static final int WHITE        = 0xFFFFFFFF;
    private static final int GRAY         = 0xFFAAAAAA;

    private String currentCategory = "crystal";
    private final List<String> categories = List.of("crystal", "sword", "mace", "cart", "uhc", "optimizer", "effects");

    public MacroScreen() {
        super(Text.literal("CrystalSpK 3D"));
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Full screen dim
        ctx.fill(0, 0, width, height, BG);

        // --- Sidebar with 3D Shadow ---
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 2, height, 0x44000000); // Shadow
        ctx.fill(0, 0, SIDEBAR_W, height, SIDEBAR_BG);
        
        int ly = 15;
        ctx.drawTextWithShadow(textRenderer, "§b§lCrystalSpK", 12, ly, WHITE);
        ly += 30;

        for (String cat : categories) {
            boolean active = cat.equals(currentCategory);
            boolean hovered = mouseX > 5 && mouseX < SIDEBAR_W - 5 && mouseY > ly - 2 && mouseY < ly + NAV_ITEM_H - 2;
            
            if (active) {
                ctx.fill(5, ly - 4, SIDEBAR_W - 5, ly + NAV_ITEM_H - 6, 0x334FC8FF);
                ctx.fill(0, ly - 4, 3, ly + NAV_ITEM_H - 6, ACCENT);
            } else if (hovered) {
                ctx.fill(5, ly - 4, SIDEBAR_W - 5, ly + NAV_ITEM_H - 6, 0x11FFFFFF);
            }

            int col = active ? ACCENT : (hovered ? WHITE : GRAY);
            String label = cat.substring(0, 1).toUpperCase() + cat.substring(1);
            ctx.drawTextWithShadow(textRenderer, label, 15, ly, col);
            ly += NAV_ITEM_H;
        }

        // --- Main Content Area ---
        if ("optimizer".equals(currentCategory)) {
            drawOptimizerPanel(ctx, mouseX, mouseY);
        } else if ("effects".equals(currentCategory)) {
            drawEffectsPanel(ctx, mouseX, mouseY);
        } else {
            drawMacroCards(ctx, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawMacroCards(DrawContext ctx, int mouseX, int mouseY) {
        int startX = SIDEBAR_W + PAD + 10;
        int startY = 25;
        int x = startX;
        int y = startY;

        List<MacroDef> filtered = MacroDef.ALL.stream()
                .filter(m -> m.category.equals(currentCategory))
                .toList();

        for (MacroDef def : filtered) {
            MacroConfig.MacroEntry entry = MacroConfig.get().macros.get(def.id);
            if (entry == null) continue;

            boolean hovered = mouseX > x && mouseX < x + CARD_W && mouseY > y && mouseY < y + CARD_H;
            
            // 3D Card Effect (Shadow + Border + Fill)
            ctx.fill(x + 2, y + 2, x + CARD_W + 2, y + CARD_H + 2, CARD_SHADOW); // Bottom-right shadow
            ctx.fill(x, y, x + CARD_W, y + CARD_H, CARD_BORDER); // Border
            ctx.fill(x + 1, y + 1, x + CARD_W - 1, y + CARD_H - 1, hovered ? 0xFF252535 : CARD_BG); // Main body

            // Status Indicator (Glow if active)
            int statusCol = entry.active ? GREEN : RED;
            if (entry.active) {
                ctx.fill(x + 5, y + 5, x + 7, y + CARD_H - 5, GREEN);
            } else {
                ctx.fill(x + 5, y + 5, x + 7, y + CARD_H - 5, RED);
            }

            // Text
            ctx.drawTextWithShadow(textRenderer, "§l" + def.name, x + 15, y + 8, WHITE);
            
            String bind = entry.keybind == -1 ? "NONE" : GLFW.glfwGetKeyName(entry.keybind, 0);
            if (bind == null) bind = "KEY " + entry.keybind;
            ctx.drawTextWithShadow(textRenderer, "Bind: §b" + bind.toUpperCase(), x + 15, y + 22, GRAY);
            
            String mode = MacroConfig.MODE_NAMES[entry.mode];
            ctx.drawTextWithShadow(textRenderer, "Mode: §e" + mode, x + 15, y + 32, GRAY);

            // Delay on the right
            String delay = entry.delay + "ms";
            int dw = textRenderer.getWidth(delay);
            ctx.drawTextWithShadow(textRenderer, delay, x + CARD_W - dw - 10, y + 8, 0xFF888888);

            // Move to next position
            x += CARD_W + CARD_GAP;
            if (x + CARD_W > width - PAD) {
                x = startX;
                y += CARD_H + CARD_GAP;
            }
        }
    }

    private void drawOptimizerPanel(DrawContext ctx, int mouseX, int mouseY) {
        int x = SIDEBAR_W + PAD + 20;
        int y = 30;
        ctx.drawTextWithShadow(textRenderer, "§b§lZalith Optimizer", x, y, WHITE);
        y += 25;

        Optimizer opt = Optimizer.get();
        for (Optimizer.OptDef def : Optimizer.ALL_OPTS) {
            boolean hovered = mouseX > x && mouseX < x + 250 && mouseY > y - 2 && mouseY < y + 12;
            boolean enabled = opt.isEnabled(def.id);

            // Row background on hover
            if (hovered) ctx.fill(x - 5, y - 2, x + 260, y + 12, 0x22FFFFFF);

            ctx.drawTextWithShadow(textRenderer, (enabled ? "§a[ON] " : "§c[OFF] ") + "§f" + def.name, x, y, WHITE);
            ctx.drawTextWithShadow(textRenderer, "§7" + def.desc, x + 100, y, GRAY);
            
            y += 16;
            if (y > height - 40) break; // Simple scroll prevention
        }
        
        ctx.drawTextWithShadow(textRenderer, "§8Click to toggle options", x, height - 20, GRAY);
    }

    private void drawEffectsPanel(DrawContext ctx, int mouseX, int mouseY) {
        int x = SIDEBAR_W + PAD + 20;
        int y = 30;
        ctx.drawTextWithShadow(textRenderer, "§b§lVisual Effects", x, y, WHITE);
        y += 25;

        String[] effects = {"Glow", "Blur", "Dark Mode", "Chroma HUD", "3D Shadows"};
        for (String effect : effects) {
            ctx.drawTextWithShadow(textRenderer, "§7[ ] §f" + effect, x, y, WHITE);
            y += 18;
        }
        ctx.drawTextWithShadow(textRenderer, "§8(Coming Soon)", x, y, GRAY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Sidebar clicks
        if (mouseX < SIDEBAR_W) {
            int ly = 45;
            for (String cat : categories) {
                if (mouseY > ly - 4 && mouseY < ly + NAV_ITEM_H - 6) {
                    currentCategory = cat;
                    return true;
                }
                ly += NAV_ITEM_H;
            }
        }

        // Macro Card clicks
        if (!"optimizer".equals(currentCategory) && !"effects".equals(currentCategory)) {
            int startX = SIDEBAR_W + PAD + 10;
            int startY = 25;
            int x = startX;
            int y = startY;

            List<MacroDef> filtered = MacroDef.ALL.stream()
                    .filter(m -> m.category.equals(currentCategory))
                    .toList();

            for (MacroDef def : filtered) {
                if (mouseX > x && mouseX < x + CARD_W && mouseY > y && mouseY < y + CARD_H) {
                    MacroConfig.MacroEntry entry = MacroConfig.get().macros.get(def.id);
                    if (entry != null) {
                        if (button == 0) { // Left click toggle
                            entry.active = !entry.active;
                            MacroConfig.get().save();
                        } else if (button == 1) { // Right click cycle mode
                            entry.mode = (entry.mode + 1) % 3;
                            MacroConfig.get().save();
                        }
                    }
                    return true;
                }
                x += CARD_W + CARD_GAP;
                if (x + CARD_W > width - PAD) {
                    x = startX;
                    y += CARD_H + CARD_GAP;
                }
            }
        }

        // Optimizer clicks
        if ("optimizer".equals(currentCategory)) {
            int x = SIDEBAR_W + PAD + 20;
            int y = 55;
            Optimizer opt = Optimizer.get();
            for (Optimizer.OptDef def : Optimizer.ALL_OPTS) {
                if (mouseX > x && mouseX < x + 250 && mouseY > y - 2 && mouseY < y + 12) {
                    opt.toggle(def.id);
                    return true;
                }
                y += 16;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        // Keybind setting logic could be added here (e.g., if a card is "focused")
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
