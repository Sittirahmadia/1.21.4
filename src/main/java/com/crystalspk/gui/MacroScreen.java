package com.crystalspk.gui;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.config.MacroDef;
import com.crystalspk.macro.MacroEngine;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Full-featured macro configuration GUI with visual effects.
 * Supports keyboard + mouse button keybinds, manual delay input, slot cycling.
 */
public class MacroScreen extends Screen {

    // ── Layout ──────────────────────────────────────────────────────────
    private static final int SIDEBAR_W = 130;
    private static final int CARD_H = 34;
    private static final int FIELD_H = 22;
    private static final int FIELD_W = 100;

    // ── Theme Colors ────────────────────────────────────────────────────
    private static final int BG         = 0xFF080A10;
    private static final int BG_GRAD    = 0xFF0C1018;
    private static final int SIDEBAR_BG = 0xFF0D1120;
    private static final int CARD_BG    = 0xFF101828;
    private static final int CARD_ON    = 0xFF121E34;
    private static final int CARD_HOV   = 0xFF141F30;
    private static final int FIELD_BG   = 0xFF151D2C;
    private static final int FIELD_HOV  = 0xFF1A2438;
    private static final int BORDER     = 0xFF1A2240;
    private static final int BORDER_ON  = 0xFF1E3860;
    private static final int ACCENT     = 0xFF4FC8FF;
    private static final int ACCENT_DIM = 0xFF2A6890;
    private static final int ACCENT_GL  = 0x304FC8FF;
    private static final int RED        = 0xFFF05E7A;
    private static final int GREEN      = 0xFF50E88A;
    private static final int YELLOW     = 0xFFF0C040;
    private static final int TEXT_W     = 0xFFE8EEFF;
    private static final int TEXT_G     = 0xFF7A88B0;
    private static final int TEXT_D     = 0xFF414D6A;
    private static final int TEXT_DD    = 0xFF252E45;
    private static final int WHITE      = 0xFFFFFFFF;
    private static final int TOGGLE_OFF = 0xFF2A3048;

    // ── State ───────────────────────────────────────────────────────────
    private String currentCategory = "crystal";
    private String expandedMacro = null;
    private int scrollY = 0;

    // Keybind capture
    private boolean capturing = false;
    private String captureId = null;

    // Delay editing
    private boolean editingDelay = false;
    private String editingDelayId = null;
    private String delayBuffer = "";

    // Animation
    private long openTime;
    private float pulsePhase = 0;

    private static final String[] CATEGORIES = {"crystal", "sword", "mace", "cart", "uhc"};
    private static final String[] CAT_LABELS = {"Crystal", "Sword", "Mace", "Cart", "UHC"};
    private static final String[] CAT_ICONS  = {"◆", "⚔", "⚒", "⛏", "♥"};

    public MacroScreen() {
        super(Text.literal("CrystalSpK Macro"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
    }

    @Override
    public boolean shouldPause() { return false; }

    // ── Render ───────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        pulsePhase += delta * 0.08f;
        float fadeIn = Math.min(1f, (System.currentTimeMillis() - openTime) / 200f);
        int fadeAlpha = (int)(fadeIn * 255) << 24;

        // Background gradient
        ctx.fill(0, 0, width, height, BG);
        // Radial glow top-left
        drawRadialGlow(ctx, 0, 0, 200, ACCENT_GL);
        // Radial glow bottom-right
        drawRadialGlow(ctx, width - 150, height - 150, 180, 0x20783CFF);

        // ── Sidebar ─────────────────────────────────────────────────────
        ctx.fill(0, 0, SIDEBAR_W, height, SIDEBAR_BG);
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, height, BORDER);

        // Logo area
        int logoY = 12;
        // Glow behind logo text
        drawHGlow(ctx, 10, logoY - 2, SIDEBAR_W - 20, 22, ACCENT_GL);
        ctx.drawTextWithShadow(textRenderer, "◆ CrystalSpK", 14, logoY + 3, ACCENT);
        ctx.drawTextWithShadow(textRenderer, "   Macro v1.0", 14, logoY + 16, TEXT_DD);

        // Category nav
        int catY = 52;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean active = CATEGORIES[i].equals(currentCategory);
            boolean hov = mouseX >= 6 && mouseX < SIDEBAR_W - 6 && mouseY >= catY && mouseY < catY + 28;

            if (active) {
                // Active: gradient fill + accent left bar
                ctx.fill(6, catY, SIDEBAR_W - 6, catY + 28, 0xFF142050);
                drawHGlow(ctx, 6, catY, SIDEBAR_W - 12, 28, 0x204FC8FF);
                ctx.fill(6, catY + 5, 9, catY + 23, ACCENT);
            } else if (hov) {
                ctx.fill(6, catY, SIDEBAR_W - 6, catY + 28, 0xFF111830);
            }

            int col = active ? TEXT_W : (hov ? TEXT_G : TEXT_D);
            ctx.drawTextWithShadow(textRenderer, CAT_ICONS[i] + " " + CAT_LABELS[i], 16, catY + 10, col);

            // Macro count badge
            int count = countActiveMacros(CATEGORIES[i]);
            if (count > 0) {
                String badge = String.valueOf(count);
                int bx = SIDEBAR_W - 24;
                ctx.fill(bx, catY + 8, bx + 14, catY + 20, active ? 0xFF1A3060 : 0xFF1A2040);
                ctx.drawTextWithShadow(textRenderer, badge, bx + 4, catY + 10, ACCENT);
            }
            catY += 32;
        }

        // Sidebar footer
        ctx.fill(8, height - 28, SIDEBAR_W - 8, height - 27, BORDER);
        ctx.drawTextWithShadow(textRenderer, "R-Shift: GUI", 14, height - 20, TEXT_DD);

        // ── Main Content ────────────────────────────────────────────────
        int cx = SIDEBAR_W + 16;
        int cy = 12;
        int cw = width - SIDEBAR_W - 32;

        // Page title with glow
        String title = getPageTitle();
        drawHGlow(ctx, cx - 4, cy - 2, cw, 22, 0x104FC8FF);
        ctx.drawTextWithShadow(textRenderer, title, cx, cy + 2, TEXT_W);
        ctx.drawTextWithShadow(textRenderer, getPageSub(), cx + textRenderer.getWidth(title) + 12, cy + 2, TEXT_D);
        cy += 24;

        // Macro cards
        MacroConfig cfg = MacroConfig.get();
        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            boolean isActive = entry.active;
            int fieldCount = 2 + def.slotNames.size(); // keybind + delay + slots
            int cardH = expanded ? CARD_H + 6 + fieldCount * (FIELD_H + 5) + 6 : CARD_H;

            // Card bg + border
            int bg = isActive ? CARD_ON : CARD_BG;
            boolean cardHov = mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + CARD_H;
            if (cardHov && !expanded) bg = CARD_HOV;

            ctx.fill(cx, cy, cx + cw, cy + cardH, bg);
            drawBorder(ctx, cx, cy, cw, cardH, isActive ? BORDER_ON : BORDER);

            // Active glow top line
            if (isActive) {
                int glowW = Math.min(cw / 2, 180);
                int pulse = (int)(Math.sin(pulsePhase) * 20 + 60);
                for (int i = 0; i < glowW; i++) {
                    int a = Math.max(0, pulse - i * pulse / glowW);
                    ctx.fill(cx + 8 + i, cy + 1, cx + 9 + i, cy + 2, (a << 24) | 0x4FC8FF);
                }
            }

            // Badge (macro ID)
            String badge = def.id.toUpperCase();
            int badgeBg = isActive ? 0xFF142A50 : 0xFF1A2038;
            ctx.fill(cx + 8, cy + 8, cx + 40, cy + 26, badgeBg);
            drawBorder(ctx, cx + 8, cy + 8, 32, 18, isActive ? ACCENT_DIM : BORDER);
            ctx.drawTextWithShadow(textRenderer, badge, cx + 13, cy + 12, isActive ? ACCENT : TEXT_G);

            // Name
            ctx.drawTextWithShadow(textRenderer, def.name, cx + 48, cy + 8, TEXT_W);

            // Keybind display (clickable)
            String kbName = MacroEngine.getBindName(entry.keybind);
            boolean kbCapturing = capturing && def.id.equals(captureId);
            String kbText = kbCapturing ? "[ ... ]" : "[" + kbName + "]";
            int kbX = cx + 48;
            int kbY = cy + 20;
            int kbW = textRenderer.getWidth(kbText);
            boolean kbHov = !expanded && mouseX >= kbX && mouseX < kbX + kbW && mouseY >= kbY && mouseY < kbY + 10;
            ctx.drawTextWithShadow(textRenderer, kbText, kbX, kbY, kbCapturing ? ACCENT : (kbHov ? ACCENT : TEXT_DD));
            if (kbHov) ctx.fill(kbX, kbY + 10, kbX + kbW, kbY + 11, ACCENT_DIM);

            // Toggle switch
            int togX = cx + cw - 40;
            int togY = cy + 10;
            drawToggle(ctx, togX, togY, isActive, mouseX, mouseY);

            // ── Expanded Body ───────────────────────────────────────────
            if (expanded) {
                int fy = cy + CARD_H + 4;
                ctx.fill(cx + 10, fy - 2, cx + cw - 10, fy - 1, BORDER);

                // Keybind field
                fy = drawKeyField(ctx, cx, fy, cw, "Keybind",
                        kbCapturing ? "Press key/mouse..." : kbName,
                        mouseX, mouseY, kbCapturing);
                fy += FIELD_H + 5;

                // Delay field (editable text)
                boolean delEditing = editingDelay && def.id.equals(editingDelayId);
                String delVal = delEditing ? delayBuffer + "_" : String.valueOf(entry.delay);
                fy = drawEditableField(ctx, cx, fy, cw, "Delay (ms)", delVal,
                        mouseX, mouseY, delEditing);
                fy += FIELD_H + 5;

                // Slot fields
                for (String slot : def.slotNames) {
                    String label = formatSlotLabel(slot);
                    int slotVal = entry.slots.getOrDefault(slot, -1);
                    String valStr = slotVal >= 0 ? "Slot " + (slotVal + 1) : "None";
                    fy = drawSlotField(ctx, cx, fy, cw, label, valStr, mouseX, mouseY);
                    fy += FIELD_H + 5;
                }
            }

            cy += cardH + 6;
        }

        // ── Capture Overlay ─────────────────────────────────────────────
        if (capturing) {
            ctx.fill(0, 0, width, height, 0xDD050810);

            int ow = 300, oh = 120;
            int ox = (width - ow) / 2, oy = (height - oh) / 2;

            // Outer glow
            drawRadialGlow(ctx, ox - 20, oy - 20, ow + 40, 0x304FC8FF);

            ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF0E1424);
            drawBorder(ctx, ox, oy, ow, oh, ACCENT);
            // Top accent line
            ctx.fill(ox + 20, oy + 1, ox + ow - 20, oy + 3, ACCENT_DIM);

            ctx.drawCenteredTextWithShadow(textRenderer, "Press any key or mouse button", width / 2, oy + 22, TEXT_W);
            ctx.drawCenteredTextWithShadow(textRenderer, "Binding: " + captureId.toUpperCase(), width / 2, oy + 50, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "ESC = cancel  |  DEL = unbind", width / 2, oy + 80, TEXT_DD);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Drawing Helpers ─────────────────────────────────────────────────

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawHGlow(DrawContext ctx, int x, int y, int w, int h, int color) {
        // Horizontal gradient glow (fades to transparent at edges)
        int a = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        for (int i = 0; i < w; i++) {
            float t = 1f - Math.abs(i - w / 2f) / (w / 2f);
            int alpha = (int)(a * t * t);
            if (alpha > 0) {
                ctx.fill(x + i, y, x + i + 1, y + h, (alpha << 24) | rgb);
            }
        }
    }

    private void drawRadialGlow(DrawContext ctx, int x, int y, int size, int color) {
        int a = (color >>> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;
        int steps = Math.min(size / 4, 20);
        for (int i = 0; i < steps; i++) {
            float t = 1f - (float) i / steps;
            int alpha = (int)(a * t * t);
            if (alpha > 0) {
                int shrink = i * size / (steps * 2);
                ctx.fill(x + shrink, y + shrink, x + size - shrink, y + size - shrink,
                        (alpha << 24) | rgb);
            }
        }
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on, int mx, int my) {
        boolean hov = mx >= x && mx < x + 30 && my >= y && my < y + 16;
        int bg = on ? ACCENT : TOGGLE_OFF;
        if (hov) bg = on ? 0xFF60D4FF : 0xFF343C58;

        ctx.fill(x, y, x + 30, y + 16, bg);
        drawBorder(ctx, x, y, 30, 16, on ? ACCENT_DIM : BORDER);

        int dotX = on ? x + 17 : x + 3;
        ctx.fill(dotX, y + 3, dotX + 10, y + 13, on ? WHITE : TEXT_D);

        // Glow when on
        if (on) {
            ctx.fill(x - 1, y - 1, x + 31, y, 0x204FC8FF);
            ctx.fill(x - 1, y + 16, x + 31, y + 17, 0x204FC8FF);
        }
    }

    private int drawKeyField(DrawContext ctx, int cx, int fy, int cw, String label, String value,
                              int mx, int my, boolean active) {
        ctx.drawTextWithShadow(textRenderer, label, cx + 16, fy + 6, TEXT_G);
        int fx = cx + cw - FIELD_W - 10;
        boolean hov = mx >= fx && mx < fx + FIELD_W && my >= fy && my < fy + FIELD_H;
        ctx.fill(fx, fy, fx + FIELD_W, fy + FIELD_H, hov ? FIELD_HOV : FIELD_BG);
        drawBorder(ctx, fx, fy, FIELD_W, FIELD_H, active ? ACCENT : (hov ? ACCENT_DIM : BORDER));
        ctx.drawTextWithShadow(textRenderer, value, fx + 6, fy + 6, active ? ACCENT : (hov ? ACCENT : TEXT_W));
        return fy;
    }

    private int drawEditableField(DrawContext ctx, int cx, int fy, int cw, String label, String value,
                                   int mx, int my, boolean active) {
        ctx.drawTextWithShadow(textRenderer, label, cx + 16, fy + 6, TEXT_G);
        int fx = cx + cw - FIELD_W - 10;
        boolean hov = mx >= fx && mx < fx + FIELD_W && my >= fy && my < fy + FIELD_H;
        ctx.fill(fx, fy, fx + FIELD_W, fy + FIELD_H, active ? 0xFF1A2844 : (hov ? FIELD_HOV : FIELD_BG));
        drawBorder(ctx, fx, fy, FIELD_W, FIELD_H, active ? ACCENT : (hov ? ACCENT_DIM : BORDER));
        ctx.drawTextWithShadow(textRenderer, value, fx + 6, fy + 6, active ? WHITE : (hov ? ACCENT : TEXT_W));
        return fy;
    }

    private int drawSlotField(DrawContext ctx, int cx, int fy, int cw, String label, String value,
                               int mx, int my) {
        ctx.drawTextWithShadow(textRenderer, label, cx + 16, fy + 6, TEXT_G);
        int fx = cx + cw - FIELD_W - 10;
        boolean hov = mx >= fx && mx < fx + FIELD_W && my >= fy && my < fy + FIELD_H;
        ctx.fill(fx, fy, fx + FIELD_W, fy + FIELD_H, hov ? FIELD_HOV : FIELD_BG);
        drawBorder(ctx, fx, fy, FIELD_W, FIELD_H, hov ? ACCENT_DIM : BORDER);
        ctx.drawTextWithShadow(textRenderer, value, fx + 6, fy + 6, hov ? ACCENT : TEXT_W);
        return fy;
    }

    // ── Mouse Clicks ────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Capture mode: any mouse button (except left which dismisses if outside)
        if (capturing) {
            if (button == 0) {
                // Left click = dismiss capture
                capturing = false;
                captureId = null;
                return true;
            }
            // Right(1), Middle(2), Side buttons(3,4) → bind as mouse button
            // Store as -(button+1): right=-2, middle=-3, btn4=-4, btn5=-5
            MacroConfig cfg = MacroConfig.get();
            MacroConfig.MacroEntry entry = cfg.macros.get(captureId);
            if (entry != null) {
                entry.keybind = -(button + 1);
                cfg.save();
            }
            capturing = false;
            captureId = null;
            return true;
        }

        // Finish delay editing on any click
        if (editingDelay) {
            finishDelayEdit();
        }

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // ── Category clicks ─────────────────────────────────────────────
        int catY = 52;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (mouseX >= 6 && mouseX < SIDEBAR_W - 6 && mouseY >= catY && mouseY < catY + 28) {
                currentCategory = CATEGORIES[i];
                expandedMacro = null;
                return true;
            }
            catY += 32;
        }

        // ── Macro card clicks ───────────────────────────────────────────
        MacroConfig cfg = MacroConfig.get();
        int cx = SIDEBAR_W + 16;
        int cy = 12 + 24; // after title
        int cw = width - SIDEBAR_W - 32;

        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            int fieldCount = 2 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 6 + fieldCount * (FIELD_H + 5) + 6 : CARD_H;

            // Toggle
            int togX = cx + cw - 40;
            int togY = cy + 10;
            if (mouseX >= togX && mouseX < togX + 30 && mouseY >= togY && mouseY < togY + 16) {
                entry.active = !entry.active;
                cfg.save();
                return true;
            }

            // Keybind click (collapsed card)
            if (!expanded) {
                String kbText = "[" + MacroEngine.getBindName(entry.keybind) + "]";
                int kbW = textRenderer.getWidth(kbText);
                if (mouseX >= cx + 48 && mouseX < cx + 48 + kbW
                        && mouseY >= cy + 20 && mouseY < cy + 30) {
                    capturing = true;
                    captureId = def.id;
                    return true;
                }
            }

            // Header click → expand/collapse
            if (mouseX >= cx && mouseX < cx + cw - 44 && mouseY >= cy && mouseY < cy + CARD_H) {
                expandedMacro = expanded ? null : def.id;
                return true;
            }

            // Expanded field clicks
            if (expanded) {
                int fy = cy + CARD_H + 4;
                int fx = cx + cw - FIELD_W - 10;

                // Keybind field
                if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                    capturing = true;
                    captureId = def.id;
                    return true;
                }
                fy += FIELD_H + 5;

                // Delay field → start editing
                if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                    editingDelay = true;
                    editingDelayId = def.id;
                    delayBuffer = String.valueOf(entry.delay);
                    return true;
                }
                fy += FIELD_H + 5;

                // Slot fields → cycle
                for (String slot : def.slotNames) {
                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        int val = entry.slots.getOrDefault(slot, -1);
                        val = (val + 2) % 10 - 1; // -1,0,1,...,8
                        entry.slots.put(slot, val);
                        cfg.save();
                        return true;
                    }
                    fy += FIELD_H + 5;
                }
            }

            cy += cardH + 6;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // ── Keyboard ────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Capture mode
        if (capturing) {
            MacroConfig cfg = MacroConfig.get();
            MacroConfig.MacroEntry entry = cfg.macros.get(captureId);

            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = false;
                captureId = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                // Unbind
                if (entry != null) { entry.keybind = 0; cfg.save(); }
                capturing = false;
                captureId = null;
                return true;
            }
            // Set keyboard keybind
            if (entry != null) { entry.keybind = keyCode; cfg.save(); }
            capturing = false;
            captureId = null;
            return true;
        }

        // Delay editing mode
        if (editingDelay) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingDelay = false;
                editingDelayId = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                finishDelayEdit();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!delayBuffer.isEmpty()) {
                    delayBuffer = delayBuffer.substring(0, delayBuffer.length() - 1);
                }
                return true;
            }
            return true; // consume all keys while editing
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingDelay) {
            if (chr >= '0' && chr <= '9' && delayBuffer.length() < 5) {
                delayBuffer += chr;
            }
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void finishDelayEdit() {
        if (editingDelayId != null && !delayBuffer.isEmpty()) {
            try {
                int val = Integer.parseInt(delayBuffer);
                val = Math.max(1, Math.min(99999, val));
                MacroConfig cfg = MacroConfig.get();
                MacroConfig.MacroEntry entry = cfg.macros.get(editingDelayId);
                if (entry != null) {
                    entry.delay = val;
                    cfg.save();
                }
            } catch (NumberFormatException ignored) {}
        }
        editingDelay = false;
        editingDelayId = null;
        delayBuffer = "";
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private int countActiveMacros(String category) {
        MacroConfig cfg = MacroConfig.get();
        int count = 0;
        for (MacroDef def : MacroDef.ALL) {
            if (def.category.equals(category)) {
                MacroConfig.MacroEntry e = cfg.macros.get(def.id);
                if (e != null && e.active) count++;
            }
        }
        return count;
    }

    private String getPageTitle() {
        return switch (currentCategory) {
            case "crystal" -> "Crystal Macros";
            case "sword" -> "Sword Macros";
            case "mace" -> "Mace Macros";
            case "cart" -> "Cart Macros";
            case "uhc" -> "UHC Macros";
            default -> "";
        };
    }

    private String getPageSub() {
        return switch (currentCategory) {
            case "crystal" -> "Anchor, crystal & utility";
            case "sword" -> "Sword & shield macros";
            case "mace" -> "Mace & elytra combos";
            case "cart" -> "Minecart TNT macros";
            case "uhc" -> "Bucket & trap macros";
            default -> "";
        };
    }

    private String formatSlotLabel(String slot) {
        String label = slot.replace("Slot", "");
        if (!label.isEmpty()) label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        StringBuilder sb = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (Character.isUpperCase(c) && !sb.isEmpty()) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }
}
