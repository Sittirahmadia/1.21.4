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
 * Modern full-screen macro GUI with sidebar navigation.
 * Sidebar shows categories + status. Main area shows scrollable macro cards.
 */
public class MacroScreen extends Screen {

    // ── Layout ──────────────────────────────────────────────────────────
    private static final int SIDEBAR_W  = 140;
    private static final int NAV_ITEM_H = 22;
    private static final int CARD_H     = 32;
    private static final int FIELD_H    = 20;
    private static final int FIELD_W    = 90;
    private static final int PAD        = 10;
    private static final int CARD_GAP   = 5;

    // ── Theme ───────────────────────────────────────────────────────────
    private static final int BG           = 0xFF0B0D14;
    private static final int SIDEBAR_BG   = 0xFF0D1120;
    private static final int SIDEBAR_SEP  = 0xFF151D30;
    private static final int CONTENT_BG   = 0xFF0B0D14;
    private static final int CARD_BG      = 0xFF111520;
    private static final int CARD_ON      = 0xFF131A2C;
    private static final int CARD_HOV     = 0xFF141828;
    private static final int CARD_BORDER  = 0xFF1A2240;
    private static final int CARD_BORDER_ON = 0xFF1E3860;
    private static final int FIELD_BG     = 0xFF0E1320;
    private static final int FIELD_HOV    = 0xFF161D30;
    private static final int FIELD_ACT    = 0xFF1A2844;
    private static final int BORDER       = 0xFF1A2240;
    private static final int ACCENT       = 0xFF4FC8FF;
    private static final int ACCENT_DIM   = 0xFF2A6890;
    private static final int ACCENT_SOFT  = 0xFF163050;
    private static final int GREEN        = 0xFF50E88A;
    private static final int GREEN_DIM    = 0xFF1A3828;
    private static final int YELLOW       = 0xFFF0C040;
    private static final int RED          = 0xFFF05E7A;
    private static final int TEXT_W       = 0xFFE8EEFF;
    private static final int TEXT_G       = 0xFF7A88B0;
    private static final int TEXT_D       = 0xFF414D6A;
    private static final int TEXT_DD      = 0xFF252E45;
    private static final int WHITE        = 0xFFFFFFFF;
    private static final int NAV_ACTIVE   = 0xFF121C35;
    private static final int NAV_HOV      = 0xFF101828;
    private static final int TOGGLE_OFF   = 0xFF2A3048;
    private static final int SCROLL_BG    = 0xFF0E1320;
    private static final int SCROLL_FG    = 0xFF2A3858;
    private static final int SCROLL_HOV   = 0xFF3A4C70;

    // ── State ───────────────────────────────────────────────────────────
    private String currentCategory = "crystal";
    private String expandedMacro = null;
    private double scrollY = 0;
    private int maxScroll = 0;

    private boolean capturing = false;
    private String captureId = null;

    private boolean editingDelay = false;
    private String editingDelayId = null;
    private String delayBuffer = "";

    private long openTime;
    private float pulsePhase = 0;

    private boolean draggingScroll = false;
    private int dragStartY = 0;
    private double dragStartScroll = 0;

    // Computed each frame
    private int sidebarX, contentX, contentW, contentTop, contentH;

    private static final String[] CATEGORIES = {"crystal", "sword", "mace", "cart", "uhc"};
    private static final String[] CAT_LABELS = {"Crystal", "Sword", "Mace", "Cart", "UHC"};
    private static final String[] CAT_ICONS  = {"\u25C6", "\u2694", "\u2692", "\u26CF", "\u2665"};
    private static final String[] CAT_SUBS   = {
        "Anchor & crystal macros", "Sword & shield macros",
        "Mace & elytra combos", "Minecart TNT macros", "Bucket & trap macros"
    };

    public MacroScreen() {
        super(Text.literal("CrystalSpK"));
    }

    @Override
    protected void init() { openTime = System.currentTimeMillis(); }

    @Override
    public boolean shouldPause() { return false; }

    // ═══════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        pulsePhase += delta * 0.06f;

        // Full background
        ctx.fill(0, 0, width, height, BG);

        // ── Sidebar ─────────────────────────────────────────────────────
        sidebarX = 0;
        ctx.fill(sidebarX, 0, SIDEBAR_W, height, SIDEBAR_BG);
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, height, SIDEBAR_SEP);

        // Logo
        int ly = 8;
        // Accent glow behind logo
        for (int i = 0; i < 60; i++) {
            int a = 12 - i * 12 / 60;
            if (a > 0) ctx.fill(8 + i, ly, 9 + i, ly + 16, (a << 24) | 0x4FC8FF);
        }
        ctx.drawTextWithShadow(textRenderer, "\u25C6 CrystalSpK", 10, ly + 3, ACCENT);
        ctx.drawTextWithShadow(textRenderer, "Macro v1.0", 10, ly + 14, TEXT_DD);
        ly += 30;

        // Status indicator
        int totalActive = countAllActive();
        if (totalActive > 0) {
            // Green dot
            ctx.fill(12, ly + 3, 16, ly + 7, GREEN);
            ctx.drawTextWithShadow(textRenderer, totalActive + " active", 20, ly + 1, GREEN);
        } else {
            ctx.fill(12, ly + 3, 16, ly + 7, TEXT_D);
            ctx.drawTextWithShadow(textRenderer, "No macros", 20, ly + 1, TEXT_D);
        }
        ly += 16;

        // Separator
        ctx.fill(8, ly, SIDEBAR_W - 8, ly + 1, SIDEBAR_SEP);
        ly += 8;

        // Section label
        ctx.drawTextWithShadow(textRenderer, "MACROS", 10, ly, TEXT_DD);
        ly += 12;

        // Nav items
        int catIdx = -1;
        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean active = CATEGORIES[i].equals(currentCategory);
            boolean hov = mouseX >= 4 && mouseX < SIDEBAR_W - 4 &&
                          mouseY >= ly && mouseY < ly + NAV_ITEM_H;
            if (active) catIdx = i;

            if (active) {
                ctx.fill(4, ly, SIDEBAR_W - 4, ly + NAV_ITEM_H, NAV_ACTIVE);
                // Left accent bar
                ctx.fill(4, ly + 3, 6, ly + NAV_ITEM_H - 3, ACCENT);
                // Subtle glow
                for (int j = 0; j < 30; j++) {
                    int a = 8 - j * 8 / 30;
                    if (a > 0) ctx.fill(6 + j, ly, 7 + j, ly + NAV_ITEM_H, (a << 24) | 0x4FC8FF);
                }
            } else if (hov) {
                ctx.fill(4, ly, SIDEBAR_W - 4, ly + NAV_ITEM_H, NAV_HOV);
            }

            int col = active ? TEXT_W : (hov ? TEXT_G : TEXT_D);
            ctx.drawTextWithShadow(textRenderer, CAT_ICONS[i] + " " + CAT_LABELS[i], 12, ly + 7, col);

            // Badge
            int cnt = countActiveMacros(CATEGORIES[i]);
            if (cnt > 0) {
                String b = String.valueOf(cnt);
                int bw = textRenderer.getWidth(b);
                ctx.fill(SIDEBAR_W - bw - 16, ly + 5, SIDEBAR_W - 10, ly + NAV_ITEM_H - 5, active ? ACCENT_SOFT : 0xFF1A2040);
                ctx.drawTextWithShadow(textRenderer, b, SIDEBAR_W - bw - 13, ly + 7, ACCENT);
            }
            ly += NAV_ITEM_H + 2;
        }

        // Footer
        ctx.fill(8, height - 24, SIDEBAR_W - 8, height - 23, SIDEBAR_SEP);
        ctx.drawTextWithShadow(textRenderer, "R-Shift: GUI", 10, height - 16, TEXT_DD);

        // ── Main Content ────────────────────────────────────────────────
        contentX = SIDEBAR_W + 1;
        contentW = width - contentX;

        // Page header
        int headerY = 8;
        String pageTitle = catIdx >= 0 ? CAT_LABELS[catIdx] + " Macros" : "Macros";
        String pageSub = catIdx >= 0 ? CAT_SUBS[catIdx] : "";

        // Subtle radial glow top-right
        for (int i = 0; i < 80; i++) {
            int a = 6 - i * 6 / 80;
            if (a > 0) {
                ctx.fill(width - 80 + i, 0, width - 79 + i, 40, (a << 24) | 0x4FC8FF);
            }
        }

        ctx.drawTextWithShadow(textRenderer, pageTitle, contentX + PAD, headerY + 2, TEXT_W);
        ctx.drawTextWithShadow(textRenderer, pageSub, contentX + PAD + textRenderer.getWidth(pageTitle) + 10, headerY + 2, TEXT_D);

        // Separator under header
        int sepY = headerY + 16;
        ctx.fill(contentX + PAD, sepY, width - PAD, sepY + 1, SIDEBAR_SEP);

        // Detection bar
        int detY = sepY + 5;
        ctx.fill(contentX + PAD, detY, width - PAD, detY + 14, 0xFF0E1320);
        drawBorder(ctx, contentX + PAD, detY, contentW - PAD * 2, 14, BORDER);
        // LED
        int ledCol = GREEN; // TODO: actual MC detection
        ctx.fill(contentX + PAD + 5, detY + 5, contentX + PAD + 9, detY + 9, ledCol);
        ctx.drawTextWithShadow(textRenderer, "Status:", contentX + PAD + 13, detY + 3, TEXT_G);
        ctx.drawTextWithShadow(textRenderer, "Ready", contentX + PAD + 52, detY + 3, TEXT_W);

        // Active chips
        int chipY = detY + 18;
        MacroConfig cfg = MacroConfig.get();
        int chipX = contentX + PAD;
        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null || !entry.active) continue;
            String chipText = def.name;
            int cw = textRenderer.getWidth(chipText) + 14;
            if (chipX + cw > width - PAD) { chipX = contentX + PAD; chipY += 13; }
            ctx.fill(chipX, chipY, chipX + cw, chipY + 12, 0xFF141828);
            drawBorder(ctx, chipX, chipY, cw, 12, BORDER);
            ctx.fill(chipX + 4, chipY + 4, chipX + 8, chipY + 8, ACCENT);
            ctx.drawTextWithShadow(textRenderer, chipText, chipX + 11, chipY + 2, TEXT_W);
            chipX += cw + 4;
        }

        // ── Scrollable Content ──────────────────────────────────────────
        contentTop = chipY + 16;
        contentH = height - contentTop;

        ctx.enableScissor(contentX, contentTop, width, height);

        int totalContentH = calcContentHeight();
        maxScroll = Math.max(0, totalContentH - contentH + PAD);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        int cy = contentTop + PAD / 2 - (int) scrollY;
        int cardAreaW = contentW - PAD * 2 - 6;

        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            boolean isActive = entry.active;
            int fieldCount = 3 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 4 + fieldCount * (FIELD_H + 4) + 4 : CARD_H;

            if (cy + cardH >= contentTop && cy <= contentTop + contentH) {
                renderCard(ctx, def, entry, expanded, isActive,
                           contentX + PAD, cy, cardAreaW, cardH, mouseX, mouseY);
            }
            cy += cardH + CARD_GAP;
        }

        ctx.disableScissor();

        // ── Scrollbar ───────────────────────────────────────────────────
        if (maxScroll > 0) {
            int sbX = width - 5;
            ctx.fill(sbX, contentTop, sbX + 4, contentTop + contentH, SCROLL_BG);
            double thumbRatio = (double) contentH / (totalContentH + PAD);
            int thumbH = Math.max(14, (int)(contentH * thumbRatio));
            int thumbY = contentTop + (int)((contentH - thumbH) * (scrollY / Math.max(1, maxScroll)));
            boolean sbHov = mouseX >= sbX && mouseX < sbX + 5 && mouseY >= thumbY && mouseY < thumbY + thumbH;
            ctx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, (sbHov || draggingScroll) ? SCROLL_HOV : SCROLL_FG);
        }

        // ── Capture Overlay ─────────────────────────────────────────────
        if (capturing) {
            ctx.fill(0, 0, width, height, 0xCC050810);
            int ow = 260, oh = 80;
            int ox = (width - ow) / 2, oy = (height - oh) / 2;
            ctx.fill(ox, oy, ox + ow, oy + oh, 0xFF0E1424);
            drawBorder(ctx, ox, oy, ow, oh, ACCENT);
            // Top accent line
            ctx.fill(ox + 20, oy + 1, ox + ow - 20, oy + 3, ACCENT_DIM);
            ctx.drawCenteredTextWithShadow(textRenderer, "Press any key or mouse button", width / 2, oy + 16, TEXT_W);
            ctx.drawCenteredTextWithShadow(textRenderer, "Binding: " + (captureId != null ? captureId.toUpperCase() : ""), width / 2, oy + 34, ACCENT);
            ctx.drawCenteredTextWithShadow(textRenderer, "ESC = cancel  |  DEL = unbind", width / 2, oy + 56, TEXT_DD);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CARD RENDERING
    // ═══════════════════════════════════════════════════════════════════

    private void renderCard(DrawContext ctx, MacroDef def, MacroConfig.MacroEntry entry,
                            boolean expanded, boolean isActive, int x, int y, int w, int h,
                            int mouseX, int mouseY) {
        boolean headerHov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CARD_H;
        int bg = isActive ? CARD_ON : (headerHov && !expanded ? CARD_HOV : CARD_BG);

        ctx.fill(x, y, x + w, y + h, bg);
        drawBorder(ctx, x, y, w, h, isActive ? CARD_BORDER_ON : CARD_BORDER);

        // Active glow — animated pulse on top edge
        if (isActive) {
            int pw = Math.min(w / 3, 100);
            int pulse = (int)(Math.sin(pulsePhase) * 20 + 45);
            for (int i = 0; i < pw; i++) {
                int a = Math.max(0, pulse - i * pulse / pw);
                ctx.fill(x + 6 + i, y + 1, x + 7 + i, y + 2, (a << 24) | 0x4FC8FF);
            }
        }

        // Badge
        String badge = def.id.toUpperCase();
        int badgeTextW = textRenderer.getWidth(badge);
        int badgeW = badgeTextW + 8;
        int badgeBg = isActive ? 0xFF142A50 : 0xFF1A2038;
        ctx.fill(x + 6, y + 7, x + 6 + badgeW, y + CARD_H - 7, badgeBg);
        drawBorder(ctx, x + 6, y + 7, badgeW, CARD_H - 14, isActive ? ACCENT_DIM : BORDER);
        ctx.drawTextWithShadow(textRenderer, badge, x + 10, y + 11, isActive ? ACCENT : TEXT_G);

        // Name
        ctx.drawTextWithShadow(textRenderer, def.name, x + 10 + badgeW + 4, y + 7, TEXT_W);

        // Collapsed: hotkey + mode tag
        if (!expanded) {
            String kbName = MacroEngine.getBindName(entry.keybind);
            boolean kbCapturing = capturing && def.id.equals(captureId);
            String kbText = kbCapturing ? "[...]" : "[" + kbName + "]";
            ctx.drawTextWithShadow(textRenderer, kbText, x + 10 + badgeW + 4, y + 19, TEXT_DD);

            // Mode tag
            String modeTag = MacroConfig.MODE_NAMES[entry.mode];
            int modeW = textRenderer.getWidth(modeTag) + 8;
            int modeX = x + 10 + badgeW + 4 + textRenderer.getWidth(kbText) + 6;
            if (entry.mode == MacroConfig.MODE_HOLD) {
                ctx.fill(modeX, y + 18, modeX + modeW, y + 28, GREEN_DIM);
                ctx.drawTextWithShadow(textRenderer, modeTag, modeX + 4, y + 19, GREEN);
            } else if (entry.mode == MacroConfig.MODE_LOOP) {
                ctx.fill(modeX, y + 18, modeX + modeW, y + 28, ACCENT_SOFT);
                ctx.drawTextWithShadow(textRenderer, modeTag, modeX + 4, y + 19, ACCENT);
            }
        }

        // Toggle
        int togX = x + w - 30;
        int togY = y + (CARD_H - 14) / 2;
        drawToggle(ctx, togX, togY, isActive, mouseX, mouseY);

        // ── Expanded ────────────────────────────────────────────────────
        if (expanded) {
            int fy = y + CARD_H + 2;
            ctx.fill(x + 8, fy - 1, x + w - 8, fy, SIDEBAR_SEP);

            // Keybind
            boolean kbCap = capturing && def.id.equals(captureId);
            String kbVal = kbCap ? "Press key..." : MacroEngine.getBindName(entry.keybind);
            drawField(ctx, x + 8, fy, w - 16, "Keybind", kbVal, mouseX, mouseY, kbCap);
            fy += FIELD_H + 4;

            // Delay
            boolean delEdit = editingDelay && def.id.equals(editingDelayId);
            String delVal = delEdit ? delayBuffer + "_" : entry.delay + "ms";
            drawField(ctx, x + 8, fy, w - 16, "Delay", delVal, mouseX, mouseY, delEdit);
            fy += FIELD_H + 4;

            // Mode
            String modeName = MacroConfig.MODE_NAMES[entry.mode];
            int modeCol = entry.mode == MacroConfig.MODE_HOLD ? GREEN :
                          entry.mode == MacroConfig.MODE_LOOP ? ACCENT : TEXT_W;
            drawFieldColored(ctx, x + 8, fy, w - 16, "Mode", modeName, mouseX, mouseY, modeCol);
            fy += FIELD_H + 4;

            // Slots
            for (String slot : def.slotNames) {
                String label = formatSlotLabel(slot);
                int slotVal = entry.slots.getOrDefault(slot, -1);
                String valStr = slotVal >= 0 ? "Slot " + (slotVal + 1) : "None";
                drawField(ctx, x + 8, fy, w - 16, label, valStr, mouseX, mouseY, false);
                fy += FIELD_H + 4;
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  DRAWING HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    private void drawToggle(DrawContext ctx, int x, int y, boolean on, int mx, int my) {
        int tw = 26, th = 14;
        boolean hov = mx >= x && mx < x + tw && my >= y && my < y + th;
        int bg = on ? ACCENT : TOGGLE_OFF;
        if (hov) bg = on ? 0xFF60D4FF : 0xFF343C58;

        ctx.fill(x, y, x + tw, y + th, bg);
        drawBorder(ctx, x, y, tw, th, on ? ACCENT_DIM : BORDER);

        int dotX = on ? x + tw - 10 : x + 2;
        ctx.fill(dotX, y + 2, dotX + 8, y + th - 2, on ? WHITE : TEXT_D);

        // Glow when on
        if (on) {
            ctx.fill(x - 1, y - 1, x + tw + 1, y, 0x184FC8FF);
            ctx.fill(x - 1, y + th, x + tw + 1, y + th + 1, 0x184FC8FF);
        }
    }

    private void drawField(DrawContext ctx, int fx, int fy, int fw, String label, String value,
                            int mx, int my, boolean active) {
        drawFieldColored(ctx, fx, fy, fw, label, value, mx, my, active ? ACCENT : TEXT_W);
    }

    private void drawFieldColored(DrawContext ctx, int fx, int fy, int fw, String label, String value,
                                   int mx, int my, int valueCol) {
        boolean active = (valueCol == ACCENT);
        int vx = fx + fw - FIELD_W;
        boolean hov = mx >= vx && mx < vx + FIELD_W && my >= fy && my < fy + FIELD_H;

        ctx.drawTextWithShadow(textRenderer, label, fx + 4, fy + 6, TEXT_G);

        int fbg = active ? FIELD_ACT : (hov ? FIELD_HOV : FIELD_BG);
        ctx.fill(vx, fy, vx + FIELD_W, fy + FIELD_H, fbg);
        drawBorder(ctx, vx, fy, FIELD_W, FIELD_H, active ? ACCENT : (hov ? ACCENT_DIM : BORDER));
        ctx.drawTextWithShadow(textRenderer, value, vx + 5, fy + 6, hov && !active ? ACCENT : valueCol);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONTENT HEIGHT
    // ═══════════════════════════════════════════════════════════════════

    private int calcContentHeight() {
        int total = 0;
        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            boolean expanded = def.id.equals(expandedMacro);
            int fieldCount = 3 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 4 + fieldCount * (FIELD_H + 4) + 4 : CARD_H;
            total += cardH + CARD_GAP;
        }
        return total;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOUSE SCROLL
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (capturing) return true;
        if (mouseX >= contentX && mouseY >= contentTop) {
            scrollY -= vAmount * 22;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MOUSE CLICKS
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Capture mode
        if (capturing) {
            if (button == 0) { capturing = false; captureId = null; return true; }
            MacroConfig cfg = MacroConfig.get();
            MacroConfig.MacroEntry entry = cfg.macros.get(captureId);
            if (entry != null) { entry.keybind = -(button + 1); cfg.save(); }
            capturing = false; captureId = null;
            return true;
        }

        if (editingDelay) finishDelayEdit();
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // ── Sidebar nav clicks ──────────────────────────────────────────
        int ly = 8 + 30 + 16 + 8 + 12; // logo + status + sep + section label
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (mouseX >= 4 && mouseX < SIDEBAR_W - 4 &&
                mouseY >= ly && mouseY < ly + NAV_ITEM_H) {
                if (!CATEGORIES[i].equals(currentCategory)) {
                    currentCategory = CATEGORIES[i];
                    expandedMacro = null;
                    scrollY = 0;
                }
                return true;
            }
            ly += NAV_ITEM_H + 2;
        }

        // ── Scrollbar drag ──────────────────────────────────────────────
        if (maxScroll > 0) {
            int sbX = width - 5;
            if (mouseX >= sbX && mouseX < sbX + 6 && mouseY >= contentTop) {
                draggingScroll = true;
                dragStartY = (int) mouseY;
                dragStartScroll = scrollY;
                return true;
            }
        }

        // ── Card clicks ─────────────────────────────────────────────────
        if (mouseX < contentX) return true; // clicked sidebar, handled above

        MacroConfig cfg = MacroConfig.get();
        int cx = contentX + PAD;
        int cy = contentTop + PAD / 2 - (int) scrollY;
        int cw = contentW - PAD * 2 - 6;

        for (MacroDef def : MacroDef.ALL) {
            if (!def.category.equals(currentCategory)) continue;
            MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
            if (entry == null) continue;

            boolean expanded = def.id.equals(expandedMacro);
            int fieldCount = 3 + def.slotNames.size();
            int cardH = expanded ? CARD_H + 4 + fieldCount * (FIELD_H + 4) + 4 : CARD_H;

            if (cy + cardH >= contentTop && cy <= contentTop + contentH &&
                mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + cardH) {

                // Toggle
                int togX = cx + cw - 30;
                int togY = cy + (CARD_H - 14) / 2;
                if (mouseX >= togX && mouseX < togX + 26 && mouseY >= togY && mouseY < togY + 14) {
                    entry.active = !entry.active;
                    cfg.save();
                    return true;
                }

                // Header click → expand/collapse
                if (mouseY >= cy && mouseY < cy + CARD_H && mouseX < togX) {
                    expandedMacro = expanded ? null : def.id;
                    return true;
                }

                // Expanded field clicks
                if (expanded) {
                    int fy = cy + CARD_H + 2;
                    int fx = cx + 8 + cw - 16 - FIELD_W;

                    // Keybind
                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        capturing = true; captureId = def.id; return true;
                    }
                    fy += FIELD_H + 4;

                    // Delay
                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        editingDelay = true; editingDelayId = def.id;
                        delayBuffer = String.valueOf(entry.delay);
                        return true;
                    }
                    fy += FIELD_H + 4;

                    // Mode
                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        entry.mode = (entry.mode + 1) % 3;
                        cfg.save(); return true;
                    }
                    fy += FIELD_H + 4;

                    // Slots
                    for (String slot : def.slotNames) {
                        if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                            int val = entry.slots.getOrDefault(slot, -1);
                            val = (val + 2) % 10 - 1;
                            entry.slots.put(slot, val);
                            cfg.save(); return true;
                        }
                        fy += FIELD_H + 4;
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
            double ratio = (double) maxScroll / (contentH - 14);
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

    // ═══════════════════════════════════════════════════════════════════
    //  KEYBOARD
    // ═══════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════════════════

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
