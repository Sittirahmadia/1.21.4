package com.crystalspk.gui;

import com.crystalspk.config.MacroConfig;
import com.crystalspk.config.MacroDef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Full-screen GUI for configuring all macros.
 * Left sidebar for categories, main area shows macro cards with toggle, keybind, delay, and slot config.
 */
public class MacroScreen extends Screen {
    private static final int SIDEBAR_W = 120;
    private static final int CARD_H = 28;
    private static final int FIELD_H = 20;

    // Colors matching the dark theme
    private static final int COL_BG      = 0xFF0B0D12;
    private static final int COL_S1      = 0xFF111520;
    private static final int COL_S2      = 0xFF181C28;
    private static final int COL_S3      = 0xFF1F2334;
    private static final int COL_ACCENT  = 0xFF4FC8FF;
    private static final int COL_RED     = 0xFFF05E7A;
    private static final int COL_TEXT    = 0xFFE8EEFF;
    private static final int COL_TEXT2   = 0xFF7A88B0;
    private static final int COL_TEXT3   = 0xFF414D6A;
    private static final int COL_BORDER  = 0xFF192037;

    private String currentCategory = "crystal";
    private String expandedMacro = null;

    // Keybind capture state
    private boolean capturing = false;
    private String captureId = null;

    // Delay text fields (created dynamically)
    private final Map<String, TextFieldWidget> delayFields = new HashMap<>();
    // Slot buttons for expanded macro
    private final Map<String, Integer> slotEditing = new HashMap<>();

    private static final String[] CATEGORIES = {"crystal", "sword", "mace", "cart", "uhc"};
    private static final String[] CAT_LABELS = {"Crystal", "Sword", "Mace", "Cart", "UHC"};

    public MacroScreen() {
        super(Text.literal("CrystalSpK Macro"));
    }

    @Override
    protected void init() {
        delayFields.clear();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Background
        ctx.fill(0, 0, width, height, COL_BG);

        // Sidebar
        ctx.fill(0, 0, SIDEBAR_W, height, COL_S1);
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, height, COL_BORDER);

        // Title
        ctx.drawTextWithShadow(textRenderer, "CrystalSpK", 12, 10, COL_ACCENT);
        ctx.drawTextWithShadow(textRenderer, "Macro", 80, 10, COL_TEXT3);

        // Category buttons
        int catY = 35;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean active = CATEGORIES[i].equals(currentCategory);
            int bgColor = active ? 0xFF142050 : 0x00000000;
            ctx.fill(4, catY, SIDEBAR_W - 4, catY + 22, bgColor);
            if (active) {
                ctx.fill(4, catY + 4, 7, catY + 18, COL_ACCENT);
            }

            boolean hovered = mouseX >= 4 && mouseX < SIDEBAR_W - 4 && mouseY >= catY && mouseY < catY + 22;
            int textColor = active ? COL_TEXT : (hovered ? COL_TEXT : COL_TEXT2);
            ctx.drawTextWithShadow(textRenderer, CAT_LABELS[i], 14, catY + 7, textColor);
            catY += 26;
        }

        // Main content area
        int contentX = SIDEBAR_W + 12;
        int contentY = 10;
        int contentW = width - SIDEBAR_W - 24;

        // Page title
        String pageTitle = switch (currentCategory) {
            case "crystal" -> "Crystal Macros";
            case "sword" -> "Sword Macros";
            case "mace" -> "Mace Macros";
            case "cart" -> "Cart Macros";
            case "uhc" -> "UHC Macros";
            default -> "";
        };
        ctx.drawTextWithShadow(textRenderer, pageTitle, contentX, contentY, COL_TEXT);
        contentY += 18;

        // Macro cards
        MacroConfig cfg = MacroConfig.get();
        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;

            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            boolean isActive = entry.active;
            int cardH = expanded ? CARD_H + 8 + (2 + def.slotNames.size()) * (FIELD_H + 4) + 8 : CARD_H;

            // Card background
            int cardBg = isActive ? 0xFF141C30 : COL_S1;
            int cardBorder = isActive ? 0xFF1E3250 : COL_BORDER;
            ctx.fill(contentX, contentY, contentX + contentW, contentY + cardH, cardBg);
            drawBorder(ctx, contentX, contentY, contentW, cardH, cardBorder);

            // Active glow
            if (isActive) {
                for (int i = 0; i < Math.min(contentW / 2, 150); i++) {
                    int alpha = Math.max(0, 80 - i * 80 / 150);
                    ctx.fill(contentX + 8 + i, contentY + 1, contentX + 9 + i, contentY + 2,
                            (alpha << 24) | 0x4FC8FF);
                }
            }

            // Badge
            String badge = def.id.toUpperCase();
            int badgeBg = isActive ? 0xFF142640 : COL_S3;
            ctx.fill(contentX + 8, contentY + 6, contentX + 38, contentY + 22, badgeBg);
            ctx.drawTextWithShadow(textRenderer, badge, contentX + 12, contentY + 10,
                    isActive ? COL_ACCENT : COL_TEXT2);

            // Name
            ctx.drawTextWithShadow(textRenderer, def.name, contentX + 44, contentY + 6, COL_TEXT);

            // Keybind display
            String kbText = entry.keybind > 0 ? getKeyName(entry.keybind) : "None";
            boolean kbCapturing = capturing && def.id.equals(captureId);
            String kbDisplay = kbCapturing ? "Press a key..." : "[" + kbText + "]";
            int kbColor = kbCapturing ? COL_ACCENT : COL_TEXT3;
            boolean kbHov = !expanded && mouseX >= contentX + 44 && mouseX < contentX + 44 + textRenderer.getWidth(kbDisplay)
                    && mouseY >= contentY + 16 && mouseY < contentY + 26;
            if (kbHov && !capturing) kbColor = COL_ACCENT;
            ctx.drawTextWithShadow(textRenderer, kbDisplay, contentX + 44, contentY + 17, kbColor);

            // Toggle
            int togX = contentX + contentW - 36;
            int togY = contentY + 8;
            int togBg = isActive ? COL_ACCENT : COL_S3;
            ctx.fill(togX, togY, togX + 28, togY + 14, togBg);
            int dotX = isActive ? togX + 16 : togX + 4;
            ctx.fill(dotX, togY + 2, dotX + 10, togY + 12, isActive ? 0xFFFFFFFF : COL_TEXT3);

            // Expanded body
            if (expanded) {
                int fy = contentY + CARD_H + 4;
                ctx.fill(contentX + 8, fy - 2, contentX + contentW - 8, fy - 1, COL_BORDER);

                // Keybind field
                drawFieldRow(ctx, contentX, fy, contentW, "Keybind",
                        capturing && def.id.equals(captureId) ? "Press a key..." : kbText,
                        mouseX, mouseY, COL_ACCENT);
                fy += FIELD_H + 4;

                // Delay field
                drawFieldRow(ctx, contentX, fy, contentW, "Delay (ms)", String.valueOf(entry.delay),
                        mouseX, mouseY, COL_TEXT);
                fy += FIELD_H + 4;

                // Slot fields
                for (String slot : def.slotNames) {
                    String label = formatSlotLabel(slot);
                    int slotVal = entry.slots.getOrDefault(slot, -1);
                    String valStr = slotVal >= 0 ? "Slot " + (slotVal + 1) : "None";
                    drawFieldRow(ctx, contentX, fy, contentW, label, valStr, mouseX, mouseY, COL_TEXT);
                    fy += FIELD_H + 4;
                }
            }

            contentY += cardH + 6;
        }

        // Capture overlay
        if (capturing) {
            ctx.fill(0, 0, width, height, 0xCC050810);
            int cx = width / 2 - 140;
            int cy = height / 2 - 50;
            ctx.fill(cx, cy, cx + 280, cy + 100, COL_S1);
            drawBorder(ctx, cx, cy, 280, 100, COL_ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "Press any key", width / 2, cy + 20, COL_TEXT);
            ctx.drawCenteredTextWithShadow(textRenderer, "Binding: " + captureId, width / 2, cy + 45, COL_TEXT2);
            ctx.drawCenteredTextWithShadow(textRenderer, "ESC to cancel", width / 2, cy + 70, COL_TEXT3);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawFieldRow(DrawContext ctx, int x, int y, int w, String label, String value,
                              int mouseX, int mouseY, int valueColor) {
        ctx.drawTextWithShadow(textRenderer, label, x + 16, y + 5, COL_TEXT2);
        int fx = x + w - 100;
        boolean hov = mouseX >= fx && mouseX < fx + 88 && mouseY >= y && mouseY < y + FIELD_H;
        ctx.fill(fx, y, fx + 88, y + FIELD_H, hov ? COL_S3 : COL_S2);
        drawBorder(ctx, fx, y, 88, FIELD_H, hov ? COL_ACCENT : COL_BORDER);
        ctx.drawTextWithShadow(textRenderer, value, fx + 6, y + 5, hov ? COL_ACCENT : valueColor);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // Capture overlay — click outside dismisses
        if (capturing) {
            capturing = false;
            captureId = null;
            return true;
        }

        // Category clicks
        int catY = 35;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (mouseX >= 4 && mouseX < SIDEBAR_W - 4 && mouseY >= catY && mouseY < catY + 22) {
                currentCategory = CATEGORIES[i];
                expandedMacro = null;
                return true;
            }
            catY += 26;
        }

        // Macro card clicks
        MacroConfig cfg = MacroConfig.get();
        int contentX = SIDEBAR_W + 12;
        int contentY = 10 + 18;
        int contentW = width - SIDEBAR_W - 24;

        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            int cardH = expanded ? CARD_H + 8 + (2 + def.slotNames.size()) * (FIELD_H + 4) + 8 : CARD_H;

            // Toggle click
            int togX = contentX + contentW - 36;
            int togY = contentY + 8;
            if (mouseX >= togX && mouseX < togX + 28 && mouseY >= togY && mouseY < togY + 14) {
                entry.active = !entry.active;
                cfg.save();
                return true;
            }

            // Keybind click from collapsed card
            if (!expanded) {
                String kbText = entry.keybind > 0 ? getKeyName(entry.keybind) : "None";
                String kbDisplay = "[" + kbText + "]";
                int kbW = textRenderer.getWidth(kbDisplay);
                if (mouseX >= contentX + 44 && mouseX < contentX + 44 + kbW
                        && mouseY >= contentY + 16 && mouseY < contentY + 26) {
                    capturing = true;
                    captureId = def.id;
                    return true;
                }
            }

            // Header click (expand/collapse) — exclude toggle area
            if (mouseX >= contentX && mouseX < contentX + contentW - 40
                    && mouseY >= contentY && mouseY < contentY + CARD_H) {
                expandedMacro = expanded ? null : def.id;
                return true;
            }

            // Expanded body clicks
            if (expanded) {
                int fy = contentY + CARD_H + 4;
                int fx = contentX + contentW - 100;

                // Keybind field
                if (mouseX >= fx && mouseX < fx + 88 && mouseY >= fy && mouseY < fy + FIELD_H) {
                    capturing = true;
                    captureId = def.id;
                    return true;
                }
                fy += FIELD_H + 4;

                // Delay field — cycle through preset values
                if (mouseX >= fx && mouseX < fx + 88 && mouseY >= fy && mouseY < fy + FIELD_H) {
                    int[] presets = {1, 10, 20, 25, 30, 35, 45, 50, 75, 100};
                    int current = entry.delay;
                    int next = presets[0];
                    for (int i = 0; i < presets.length; i++) {
                        if (presets[i] > current) { next = presets[i]; break; }
                        if (i == presets.length - 1) next = presets[0];
                    }
                    entry.delay = next;
                    cfg.save();
                    return true;
                }
                fy += FIELD_H + 4;

                // Slot fields — cycle 0-8, -1
                for (String slot : def.slotNames) {
                    if (mouseX >= fx && mouseX < fx + 88 && mouseY >= fy && mouseY < fy + FIELD_H) {
                        int val = entry.slots.getOrDefault(slot, -1);
                        val = (val + 2) % 10 - 1; // cycles: -1, 0, 1, 2, ..., 8
                        entry.slots.put(slot, val);
                        cfg.save();
                        return true;
                    }
                    fy += FIELD_H + 4;
                }
            }

            contentY += cardH + 6;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                // Cancel capture
                capturing = false;
                captureId = null;
                return true;
            }
            // Set keybind
            MacroConfig cfg = MacroConfig.get();
            MacroConfig.MacroEntry entry = cfg.macros.get(captureId);
            if (entry != null) {
                entry.keybind = keyCode;
                cfg.save();
            }
            capturing = false;
            captureId = null;
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private String getKeyName(int keyCode) {
        if (keyCode <= 0) return "None";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        if (name != null) return name.toUpperCase();
        // Named keys
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L_SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R_SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L_CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R_CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L_ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R_ALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_INSERT -> "INSERT";
            case GLFW.GLFW_KEY_HOME -> "HOME";
            case GLFW.GLFW_KEY_END -> "END";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "CAPS";
            default -> {
                if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) {
                    yield "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
                }
                yield "KEY_" + keyCode;
            }
        };
    }

    private String formatSlotLabel(String slot) {
        // "anchorSlot" → "Anchor"
        String label = slot.replace("Slot", "");
        if (!label.isEmpty()) {
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        }
        // camelCase to space-separated
        StringBuilder sb = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (Character.isUpperCase(c) && sb.length() > 0) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }
}
