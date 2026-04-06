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
 * Compact, scrollable macro configuration GUI.
 * Top tab bar for categories, scrollable card list, smaller footprint.
 */
public class MacroScreen extends Screen {

    // ── Layout ──────────────────────────────────────────────────────────
    private static final int TAB_H      = 24;
    private static final int CARD_H     = 26;
    private static final int FIELD_H    = 18;
    private static final int FIELD_W    = 80;
    private static final int PAD        = 6;
    private static final int CARD_GAP   = 4;

    // ── Theme Colors ────────────────────────────────────────────────────
    private static final int BG         = 0xFF0A0D14;
    private static final int PANEL_BG   = 0xFF0E1320;
    private static final int TAB_BG     = 0xFF0D1120;
    private static final int TAB_ACT    = 0xFF142050;
    private static final int TAB_HOV    = 0xFF111830;
    private static final int CARD_BG    = 0xFF101828;
    private static final int CARD_ON    = 0xFF121E34;
    private static final int CARD_HOV   = 0xFF141F30;
    private static final int FIELD_BG   = 0xFF151D2C;
    private static final int FIELD_HOV  = 0xFF1A2438;
    private static final int BORDER     = 0xFF1A2240;
    private static final int BORDER_ON  = 0xFF1E3860;
    private static final int ACCENT     = 0xFF4FC8FF;
    private static final int ACCENT_DIM = 0xFF2A6890;
    private static final int GREEN      = 0xFF50E88A;
    private static final int TEXT_W     = 0xFFE8EEFF;
    private static final int TEXT_G     = 0xFF7A88B0;
    private static final int TEXT_D     = 0xFF414D6A;
    private static final int TEXT_DD    = 0xFF252E45;
    private static final int WHITE      = 0xFFFFFFFF;
    private static final int TOGGLE_OFF = 0xFF2A3048;
    private static final int SCROLL_BG  = 0xFF151D2C;
    private static final int SCROLL_FG  = 0xFF2A3858;
    private static final int SCROLL_HOV = 0xFF3A4C70;

    // ── State ───────────────────────────────────────────────────────────
    private String currentCategory = "crystal";
    private String expandedMacro = null;
    private double scrollY = 0;
    private int maxScroll = 0;

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

    // Scroll drag
    private boolean draggingScroll = false;
    private int dragStartY = 0;
    private double dragStartScroll = 0;

    // Panel geometry (computed in render)
    private int panelX, panelY, panelW, panelH;
    private int contentTop, contentH;

    private static final String[] CATEGORIES = {"crystal", "sword", "mace", "cart", "uhc"};
    private static final String[] CAT_LABELS = {"Crystal", "Sword", "Mace", "Cart", "UHC"};
    private static final String[] CAT_ICONS  = {"\u25C6", "\u2694", "\u2692", "\u26CF", "\u2665"};

    public MacroScreen() {
        super(Text.literal("CrystalSpK"));
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

        // Full screen dark bg
        ctx.fill(0, 0, width, height, BG);

        // Center panel — compact size
        panelW = Math.min(320, width - 20);
        panelH = Math.min(height - 20, 340);
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;

        // Panel background with border
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG);
        drawBorder(ctx, panelX, panelY, panelW, panelH, BORDER);

        // ── Title bar ───────────────────────────────────────────────────
        int titleH = 16;
        ctx.fill(panelX, panelY, panelX + panelW, panelY + titleH, TAB_BG);
        ctx.fill(panelX, panelY + titleH, panelX + panelW, panelY + titleH + 1, BORDER);
        ctx.drawTextWithShadow(textRenderer, "\u25C6 CrystalSpK", panelX + 5, panelY + 4, ACCENT);

        // Active macro count
        int totalActive = countAllActive();
        if (totalActive > 0) {
            String badge = totalActive + " active";
            int bw = textRenderer.getWidth(badge);
            ctx.drawTextWithShadow(textRenderer, badge, panelX + panelW - bw - 6, panelY + 4, GREEN);
        }

        // ── Tab bar ─────────────────────────────────────────────────────
        int tabY = panelY + titleH + 1;
        int tabW = panelW / CATEGORIES.length;

        for (int i = 0; i < CATEGORIES.length; i++) {
            int tx = panelX + i * tabW;
            int tw = (i == CATEGORIES.length - 1) ? panelX + panelW - tx : tabW;
            boolean active = CATEGORIES[i].equals(currentCategory);
            boolean hov = mouseX >= tx && mouseX < tx + tw && mouseY >= tabY && mouseY < tabY + TAB_H;

            if (active) {
                ctx.fill(tx, tabY, tx + tw, tabY + TAB_H, TAB_ACT);
                // Bottom accent line
                ctx.fill(tx + 4, tabY + TAB_H - 2, tx + tw - 4, tabY + TAB_H, ACCENT);
            } else if (hov) {
                ctx.fill(tx, tabY, tx + tw, tabY + TAB_H, TAB_HOV);
            }

            int count = countActiveMacros(CATEGORIES[i]);
            String label = CAT_ICONS[i] + " " + CAT_LABELS[i];
            int lw = textRenderer.getWidth(label);
            int col = active ? TEXT_W : (hov ? TEXT_G : TEXT_D);
            ctx.drawTextWithShadow(textRenderer, label, tx + (tw - lw) / 2, tabY + 8, col);

            // Badge
            if (count > 0) {
                String cb = String.valueOf(count);
                int cbw = textRenderer.getWidth(cb);
                ctx.fill(tx + tw - cbw - 7, tabY + 3, tx + tw - 3, tabY + 13, active ? ACCENT_DIM : 0xFF1A2040);
                ctx.drawTextWithShadow(textRenderer, cb, tx + tw - cbw - 5, tabY + 4, ACCENT);
            }

            // Separator
            if (i < CATEGORIES.length - 1) {
                ctx.fill(tx + tw, tabY + 4, tx + tw + 1, tabY + TAB_H - 4, BORDER);
            }
        }

        // ── Content area (scrollable) ───────────────────────────────────
        contentTop = tabY + TAB_H + 1;
        contentH = panelY + panelH - contentTop;
        ctx.fill(panelX, contentTop, panelX + panelW, contentTop + 1, BORDER);

        // Enable scissor for scroll clipping
        ctx.enableScissor(panelX, contentTop, panelX + panelW, contentTop + contentH);

        // Calculate total content height
        int totalContentH = calcContentHeight();
        maxScroll = Math.max(0, totalContentH - contentH + PAD);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        // Draw macro cards
        MacroConfig cfg = MacroConfig.get();
        int cy = contentTop + PAD - (int) scrollY;

        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            boolean isActive = entry.active;
            int fieldCount = 2 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 2 + fieldCount * (FIELD_H + 3) + 4 : CARD_H;

            // Skip cards fully outside view
            if (cy + cardH >= contentTop && cy <= contentTop + contentH) {
                renderCard(ctx, def, entry, expanded, isActive, cx(PAD), cy, panelW - PAD * 2 - 6, cardH, mouseX, mouseY);
            }

            cy += cardH + CARD_GAP;
        }

        ctx.disableScissor();

        // ── Scrollbar ───────────────────────────────────────────────────
        if (maxScroll > 0) {
            int sbX = panelX + panelW - 5;
            int sbH = contentH;
            ctx.fill(sbX, contentTop, sbX + 4, contentTop + sbH, SCROLL_BG);

            double thumbRatio = (double) contentH / (totalContentH + PAD);
            int thumbH = Math.max(12, (int)(sbH * thumbRatio));
            int thumbY = contentTop + (int)((sbH - thumbH) * (scrollY / Math.max(1, maxScroll)));

            boolean sbHov = mouseX >= sbX && mouseX < sbX + 4 && mouseY >= thumbY && mouseY < thumbY + thumbH;
            ctx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, (sbHov || draggingScroll) ? SCROLL_HOV : SCROLL_FG);
        }

        // ── Capture Overlay ─────────────────────────────────────────────
        if (capturing) {
            ctx.fill(0, 0, width, height, 0xDD050810);
            int ow = 240, oh = 80;
            int ox = (width - ow) / 2, oy = (height - oh) / 2;
            ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF0E1424);
            drawBorder(ctx, ox, oy, ow, oh, ACCENT);
            ctx.fill(ox + 16, oy + 1, ox + ow - 16, oy + 2, ACCENT_DIM);
            ctx.drawCenteredTextWithShadow(textRenderer, "Press key / mouse button", width / 2, oy + 14, TEXT_W);
            ctx.drawCenteredTextWithShadow(textRenderer, "Binding: " + captureId.toUpperCase(), width / 2, oy + 34, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "ESC=cancel  DEL=unbind", width / 2, oy + 56, TEXT_DD);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ── Card Rendering ──────────────────────────────────────────────────

    private void renderCard(DrawContext ctx, MacroDef def, MacroConfig.MacroEntry entry,
                            boolean expanded, boolean isActive, int x, int y, int w, int h,
                            int mouseX, int mouseY) {

        boolean headerHov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CARD_H;
        int bg = isActive ? CARD_ON : (headerHov && !expanded ? CARD_HOV : CARD_BG);
        ctx.fill(x, y, x + w, y + h, bg);
        drawBorder(ctx, x, y, w, h, isActive ? BORDER_ON : BORDER);

        // Active pulse top line
        if (isActive) {
            int pw = Math.min(w / 3, 80);
            int pulse = (int)(Math.sin(pulsePhase) * 15 + 40);
            for (int i = 0; i < pw; i++) {
                int a = Math.max(0, pulse - i * pulse / pw);
                ctx.fill(x + 4 + i, y + 1, x + 5 + i, y + 2, (a << 24) | 0x4FC8FF);
            }
        }

        // Badge
        String badge = def.id.toUpperCase();
        int badgeBg = isActive ? 0xFF142A50 : 0xFF1A2038;
        int badgeW = textRenderer.getWidth(badge) + 6;
        ctx.fill(x + 4, y + 5, x + 4 + badgeW, y + CARD_H - 5, badgeBg);
        ctx.drawTextWithShadow(textRenderer, badge, x + 7, y + 9, isActive ? ACCENT : TEXT_G);

        // Name
        ctx.drawTextWithShadow(textRenderer, def.name, x + 8 + badgeW, y + 5, TEXT_W);

        // Inline keybind (collapsed only)
        if (!expanded) {
            String kbName = MacroEngine.getBindName(entry.keybind);
            boolean kbCapturing = capturing && def.id.equals(captureId);
            String kbText = kbCapturing ? "[...]" : "[" + kbName + "]";
            int kbX = x + 8 + badgeW;
            int kbY = y + 16;
            int kbW = textRenderer.getWidth(kbText);
            boolean kbHov = mouseX >= kbX && mouseX < kbX + kbW && mouseY >= kbY && mouseY < kbY + 9;
            ctx.drawTextWithShadow(textRenderer, kbText, kbX, kbY, kbCapturing ? ACCENT : (kbHov ? ACCENT : TEXT_DD));
        }

        // Toggle
        int togX = x + w - 28;
        int togY = y + 6;
        drawToggle(ctx, togX, togY, isActive, mouseX, mouseY);

        // ── Expanded Fields ─────────────────────────────────────────────
        if (expanded) {
            int fy = y + CARD_H + 1;
            ctx.fill(x + 6, fy - 1, x + w - 6, fy, BORDER);

            // Keybind
            boolean kbCapturing = capturing && def.id.equals(captureId);
            String kbVal = kbCapturing ? "Press key..." : MacroEngine.getBindName(entry.keybind);
            fy = drawFieldRow(ctx, x, fy, w, "Keybind", kbVal, mouseX, mouseY, kbCapturing);
            fy += FIELD_H + 3;

            // Delay
            boolean delEditing = editingDelay && def.id.equals(editingDelayId);
            String delVal = delEditing ? delayBuffer + "_" : entry.delay + "ms";
            fy = drawFieldRow(ctx, x, fy, w, "Delay", delVal, mouseX, mouseY, delEditing);
            fy += FIELD_H + 3;

            // Slots
            for (String slot : def.slotNames) {
                String label = formatSlotLabel(slot);
                int slotVal = entry.slots.getOrDefault(slot, -1);
                String valStr = slotVal >= 0 ? "Slot " + (slotVal + 1) : "None";
                fy = drawFieldRow(ctx, x, fy, w, label, valStr, mouseX, mouseY, false);
                fy += FIELD_H + 3;
            }
        }
    }

    // ── Drawing Helpers ─────────────────────────────────────────────────

    private int cx(int offset) { return panelX + offset; }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on, int mx, int my) {
        int tw = 24, th = 12;
        boolean hov = mx >= x && mx < x + tw && my >= y && my < y + th;
        int bg = on ? ACCENT : TOGGLE_OFF;
        if (hov) bg = on ? 0xFF60D4FF : 0xFF343C58;

        ctx.fill(x, y, x + tw, y + th, bg);
        drawBorder(ctx, x, y, tw, th, on ? ACCENT_DIM : BORDER);

        int dotX = on ? x + 14 : x + 2;
        ctx.fill(dotX, y + 2, dotX + 8, y + 10, on ? WHITE : TEXT_D);
    }

    private int drawFieldRow(DrawContext ctx, int cx, int fy, int cw, String label, String value,
                              int mx, int my, boolean active) {
        int fx = cx + cw - FIELD_W - 8;
        boolean hov = mx >= fx && mx < fx + FIELD_W && my >= fy && my < fy + FIELD_H;

        ctx.drawTextWithShadow(textRenderer, label, cx + 10, fy + 5, TEXT_G);

        ctx.fill(fx, fy, fx + FIELD_W, fy + FIELD_H, active ? 0xFF1A2844 : (hov ? FIELD_HOV : FIELD_BG));
        drawBorder(ctx, fx, fy, FIELD_W, FIELD_H, active ? ACCENT : (hov ? ACCENT_DIM : BORDER));
        ctx.drawTextWithShadow(textRenderer, value, fx + 4, fy + 5, active ? WHITE : (hov ? ACCENT : TEXT_W));
        return fy;
    }

    // ── Content Height Calculation ──────────────────────────────────────

    private int calcContentHeight() {
        int total = 0;
        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            boolean expanded = def.id.equals(expandedMacro);
            int fieldCount = 2 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 2 + fieldCount * (FIELD_H + 3) + 4 : CARD_H;
            total += cardH + CARD_GAP;
        }
        return total;
    }

    // ── Mouse Scroll ────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (capturing) return true;
        if (mouseX >= panelX && mouseX < panelX + panelW &&
            mouseY >= contentTop && mouseY < contentTop + contentH) {
            scrollY -= vAmount * 18;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    // ── Mouse Clicks ────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Capture mode
        if (capturing) {
            if (button == 0) {
                capturing = false;
                captureId = null;
                return true;
            }
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

        if (editingDelay) finishDelayEdit();

        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // ── Tab clicks ──────────────────────────────────────────────────
        int tabY = panelY + 17;
        int tabW = panelW / CATEGORIES.length;
        for (int i = 0; i < CATEGORIES.length; i++) {
            int tx = panelX + i * tabW;
            int tw = (i == CATEGORIES.length - 1) ? panelX + panelW - tx : tabW;
            if (mouseX >= tx && mouseX < tx + tw && mouseY >= tabY && mouseY < tabY + TAB_H) {
                if (!CATEGORIES[i].equals(currentCategory)) {
                    currentCategory = CATEGORIES[i];
                    expandedMacro = null;
                    scrollY = 0;
                }
                return true;
            }
        }

        // ── Scrollbar drag ──────────────────────────────────────────────
        if (maxScroll > 0) {
            int sbX = panelX + panelW - 5;
            if (mouseX >= sbX && mouseX < sbX + 6 && mouseY >= contentTop && mouseY < contentTop + contentH) {
                draggingScroll = true;
                dragStartY = (int) mouseY;
                dragStartScroll = scrollY;
                return true;
            }
        }

        // ── Card clicks ─────────────────────────────────────────────────
        MacroConfig cfg = MacroConfig.get();
        int cx = panelX + PAD;
        int cy = contentTop + PAD - (int) scrollY;
        int cw = panelW - PAD * 2 - 6;

        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            int fieldCount = 2 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 2 + fieldCount * (FIELD_H + 3) + 4 : CARD_H;

            // Only process if visible in scroll area
            if (cy + cardH >= contentTop && cy <= contentTop + contentH) {

                // Toggle
                int togX = cx + cw - 28;
                int togY = cy + 6;
                if (mouseX >= togX && mouseX < togX + 24 && mouseY >= togY && mouseY < togY + 12) {
                    entry.active = !entry.active;
                    cfg.save();
                    return true;
                }

                // Keybind click (collapsed)
                if (!expanded) {
                    String badge = def.id.toUpperCase();
                    int badgeW = textRenderer.getWidth(badge) + 6;
                    String kbText = "[" + MacroEngine.getBindName(entry.keybind) + "]";
                    int kbW = textRenderer.getWidth(kbText);
                    int kbX = cx + 8 + badgeW;
                    int kbY = cy + 16;
                    if (mouseX >= kbX && mouseX < kbX + kbW && mouseY >= kbY && mouseY < kbY + 9) {
                        capturing = true;
                        captureId = def.id;
                        return true;
                    }
                }

                // Header click → expand/collapse
                if (mouseX >= cx && mouseX < cx + cw - 30 && mouseY >= cy && mouseY < cy + CARD_H) {
                    expandedMacro = expanded ? null : def.id;
                    return true;
                }

                // Expanded field clicks
                if (expanded) {
                    int fy = cy + CARD_H + 1;
                    int fx = cx + cw - FIELD_W - 8;

                    // Keybind field
                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        capturing = true;
                        captureId = def.id;
                        return true;
                    }
                    fy += FIELD_H + 3;

                    // Delay field
                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        editingDelay = true;
                        editingDelayId = def.id;
                        delayBuffer = String.valueOf(entry.delay);
                        return true;
                    }
                    fy += FIELD_H + 3;

                    // Slot fields
                    for (String slot : def.slotNames) {
                        if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                            int val = entry.slots.getOrDefault(slot, -1);
                            val = (val + 2) % 10 - 1; // -1,0,1,...,8
                            entry.slots.put(slot, val);
                            cfg.save();
                            return true;
                        }
                        fy += FIELD_H + 3;
                    }
                }
            }

            cy += cardH + CARD_GAP;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScroll && maxScroll > 0) {
            double ratio = (double) maxScroll / (contentH - 12);
            scrollY = dragStartScroll + (mouseY - dragStartY) * ratio;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ── Keyboard ────────────────────────────────────────────────────────

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            MacroConfig cfg = MacroConfig.get();
            MacroConfig.MacroEntry entry = cfg.macros.get(captureId);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = false; captureId = null; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (entry != null) { entry.keybind = 0; cfg.save(); }
                capturing = false; captureId = null; return true;
            }
            if (entry != null) { entry.keybind = keyCode; cfg.save(); }
            capturing = false; captureId = null; return true;
        }

        if (editingDelay) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingDelay = false; editingDelayId = null; return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                finishDelayEdit(); return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!delayBuffer.isEmpty())
                    delayBuffer = delayBuffer.substring(0, delayBuffer.length() - 1);
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (editingDelay) {
            if (chr >= '0' && chr <= '9' && delayBuffer.length() < 5) delayBuffer += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void finishDelayEdit() {
        if (editingDelayId != null && !delayBuffer.isEmpty()) {
            try {
                int val = Math.max(1, Math.min(99999, Integer.parseInt(delayBuffer)));
                MacroConfig cfg = MacroConfig.get();
                MacroConfig.MacroEntry entry = cfg.macros.get(editingDelayId);
                if (entry != null) { entry.delay = val; cfg.save(); }
            } catch (NumberFormatException ignored) {}
        }
        editingDelay = false; editingDelayId = null; delayBuffer = "";
    }

    // ── Utility ─────────────────────────────────────────────────────────

    private int countActiveMacros(String category) {
        MacroConfig cfg = MacroConfig.get();
        int count = 0;
        for (MacroDef def : MacroDef.ALL)
            if (def.category.equals(category)) {
                MacroConfig.MacroEntry e = cfg.macros.get(def.id);
                if (e != null && e.active) count++;
            }
        return count;
    }

    private int countAllActive() {
        MacroConfig cfg = MacroConfig.get();
        int count = 0;
        for (MacroDef def : MacroDef.ALL) {
            MacroConfig.MacroEntry e = cfg.macros.get(def.id);
            if (e != null && e.active) count++;
        }
        return count;
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
