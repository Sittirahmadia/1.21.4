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
 * Modern 3D dark GUI with advanced visual effects.
 * Compact sidebar + animated 3D cards with gradient effects and depth shadows.
 */
public class MacroScreen extends Screen {

    // ── Layout ──────────────────────────────────────────────────────────
    private static final int SIDEBAR_W  = 120;
    private static final int NAV_ITEM_H = 28;
    private static final int CARD_H     = 36;
    private static final int FIELD_H    = 18;
    private static final int FIELD_W    = 80;
    private static final int PAD        = 8;
    private static final int CARD_GAP   = 4;

    // ── Modern 3D Dark Theme ─────────────────────────────────────────────
    private static final int BG             = 0xFF0A0E18;
    private static final int BG_SECONDARY   = 0xFF0D1127;
    private static final int SIDEBAR_BG     = 0xFF0C1220;
    private static final int SIDEBAR_SEP    = 0xFF151E35;
    private static final int CARD_BG        = 0xFF0F1428;
    private static final int CARD_BG_HOVER  = 0xFF141C32;
    private static final int CARD_BG_ACTIVE = 0xFF0F2745;
    private static final int CARD_SHADOW    = 0xFF05080D;
    private static final int BORDER_LIGHT   = 0xFF1A2A48;
    private static final int BORDER_ACTIVE  = 0xFF2563EB;
    private static final int FIELD_BG       = 0xFF0D1520;
    private static final int FIELD_BORDER   = 0xFF1A2540;
    private static final int ACCENT_BLUE    = 0xFF3B82F6;
    private static final int ACCENT_CYAN    = 0xFF06B6D4;
    private static final int ACCENT_PURPLE  = 0xFF8B5CF6;
    private static final int SUCCESS_GREEN  = 0xFF10B981;
    private static final int WARNING_YELLOW = 0xFFF59E0B;
    private static final int DANGER_RED     = 0xFFEF4444;
    private static final int TEXT_PRIMARY   = 0xFFE5E7EB;
    private static final int TEXT_SECONDARY = 0xFF9CA3AF;
    private static final int TEXT_TERTIARY  = 0xFF6B7280;
    private static final int GLOW_BLUE      = 0xFF3B82F6;
    private static final int SCROLL_TRACK   = 0xFF0D1520;
    private static final int SCROLL_THUMB   = 0xFF2D3E5F;

    // ── Animation State ─────────────────────────────────────────────────
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
    private float hoverPhase = 0;

    private boolean draggingScroll = false;
    private int dragStartY = 0;
    private double dragStartScroll = 0;

    // Layout computed each frame
    private int sidebarX, contentX, contentW, contentTop, contentH;

    private static final String[] CATEGORIES = {"crystal", "sword", "mace", "cart", "uhc", "optimizer"};
    private static final String[] CAT_LABELS = {"Crystal", "Sword", "Mace", "Cart", "UHC", "Optimizer"};
    private static final String[] CAT_ICONS  = {"◆", "⚔", "⚒", "⛏", "❤", "⚡"};
    private static final String[] CAT_SUBS   = {
        "Anchor & crystal", "Sword & shield", "Mace & elytra",
        "Minecart TNT", "Bucket & trap", "Optimizations"
    };

    public MacroScreen() {
        super(Text.literal("CrystalSpK Macros"));
    }

    @Override
    protected void init() { 
        openTime = System.currentTimeMillis(); 
    }

    @Override
    public boolean shouldPause() { 
        return false; 
    }

    // ═══════════════════════════════════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        pulsePhase += delta * 0.05f;
        hoverPhase += delta * 0.04f;

        // Full gradient background
        drawGradientBackground(ctx);

        // ── Sidebar ─────────────────────────────────────────────────────
        sidebarX = 0;
        drawSidebarBackground(ctx);
        drawSidebarHeader(ctx);
        drawSidebarNavigation(ctx, mouseX, mouseY);
        drawSidebarFooter(ctx);

        // ── Main Content ────────────────────────────────────────────────
        contentX = SIDEBAR_W + 1;
        contentW = width - contentX;
        drawMainContent(ctx, mouseX, mouseY, delta);

        // ── Capture Overlay ─────────────────────────────────────────────
        if (capturing) {
            drawCaptureOverlay(ctx);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawGradientBackground(DrawContext ctx) {
        ctx.fill(0, 0, width, height, BG);
        // Subtle corner gradient accents
        for (int i = 0; i < 100; i++) {
            int a = (20 - i * 20 / 100);
            if (a > 0) {
                ctx.fill(width - 100 + i, 0, width - 99 + i, 60, (a << 24) | GLOW_BLUE);
            }
        }
    }

    private void drawSidebarBackground(DrawContext ctx) {
        ctx.fill(sidebarX, 0, SIDEBAR_W, height, SIDEBAR_BG);
        ctx.fill(SIDEBAR_W, 0, SIDEBAR_W + 1, height, SIDEBAR_SEP);
    }

    private void drawSidebarHeader(DrawContext ctx) {
        int ly = 10;
        
        // 3D effect: shadow and glow
        drawShadow(ctx, 8, ly + 2, 110, 20, 2);
        ctx.fill(8, ly, 110, ly + 16, CARD_BG_ACTIVE);
        drawBorder3D(ctx, 8, ly, 110, 16, ACCENT_BLUE, true);
        
        ctx.drawTextWithShadow(textRenderer, "◆ CrystalSpK", 12, ly + 4, ACCENT_CYAN);
        ctx.drawTextWithShadow(textRenderer, "v1.0", 12, ly + 12, TEXT_TERTIARY);
        
        ly += 22;

        // Status indicator with 3D effect
        int totalActive = countAllActive();
        int statusColor = totalActive > 0 ? SUCCESS_GREEN : TEXT_TERTIARY;
        drawShadow(ctx, 10, ly + 1, 100, 14, 1);
        ctx.fill(10, ly, 110, ly + 14, CARD_BG);
        
        if (totalActive > 0) {
            ctx.fill(14, ly + 4, 18, ly + 10, SUCCESS_GREEN);
        } else {
            ctx.fill(14, ly + 4, 18, ly + 10, TEXT_TERTIARY);
        }
        
        String statusText = totalActive > 0 ? totalActive + " active" : "Idle";
        ctx.drawTextWithShadow(textRenderer, statusText, 22, ly + 2, statusColor);
        
        ly += 18;
        ctx.fill(10, ly, 110, ly + 1, SIDEBAR_SEP);
        ly += 8;

        ctx.drawTextWithShadow(textRenderer, "MACROS", 12, ly, TEXT_TERTIARY);
    }

    private void drawSidebarNavigation(DrawContext ctx, int mouseX, int mouseY) {
        int ly = 10 + 22 + 18 + 8 + 12;

        for (int i = 0; i < CATEGORIES.length; i++) {
            boolean active = CATEGORIES[i].equals(currentCategory);
            boolean hov = mouseX >= 4 && mouseX < SIDEBAR_W - 4 &&
                          mouseY >= ly && mouseY < ly + NAV_ITEM_H;

            int bgColor = active ? CARD_BG_ACTIVE : (hov ? CARD_BG_HOVER : CARD_BG);
            int borderColor = active ? ACCENT_BLUE : BORDER_LIGHT;

            if (hov || active) {
                drawShadow(ctx, 4, ly + 1, SIDEBAR_W - 8, NAV_ITEM_H - 2, 2);
            }

            ctx.fill(4, ly, SIDEBAR_W - 4, ly + NAV_ITEM_H, bgColor);
            drawBorder3D(ctx, 4, ly, SIDEBAR_W - 8, NAV_ITEM_H, borderColor, active);

            int textColor = active ? ACCENT_CYAN : (hov ? TEXT_PRIMARY : TEXT_SECONDARY);
            ctx.drawTextWithShadow(textRenderer, CAT_ICONS[i] + " " + CAT_LABELS[i], 10, ly + 7, textColor);

            // Badge
            int cnt = countActiveMacros(CATEGORIES[i]);
            if (cnt > 0) {
                String badge = String.valueOf(cnt);
                int badgeW = textRenderer.getWidth(badge) + 6;
                ctx.fill(SIDEBAR_W - badgeW - 6, ly + 6, SIDEBAR_W - 6, ly + NAV_ITEM_H - 6, 
                        active ? ACCENT_PURPLE : ACCENT_BLUE);
                ctx.drawTextWithShadow(textRenderer, badge, SIDEBAR_W - badgeW - 3, ly + 7, 0xFFFFFFFF);
            }

            ly += NAV_ITEM_H + 3;
        }
    }

    private void drawSidebarFooter(DrawContext ctx) {
        ctx.fill(8, height - 22, SIDEBAR_W - 8, height - 21, SIDEBAR_SEP);
        ctx.drawTextWithShadow(textRenderer, "R-Shift: Close", 10, height - 15, TEXT_TERTIARY);
    }

    private void drawMainContent(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Page header
        int headerY = 10;
        int catIdx = -1;
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(currentCategory)) {
                catIdx = i;
                break;
            }
        }

        String pageTitle = catIdx >= 0 ? CAT_LABELS[catIdx] : "Macros";
        String pageSub = catIdx >= 0 ? CAT_SUBS[catIdx] : "";

        ctx.drawTextWithShadow(textRenderer, pageTitle, contentX + PAD, headerY, TEXT_PRIMARY);
        ctx.drawTextWithShadow(textRenderer, pageSub, contentX + PAD + 120, headerY, TEXT_TERTIARY);

        // Separator
        int sepY = headerY + 16;
        ctx.fill(contentX + PAD, sepY, width - PAD, sepY + 1, SIDEBAR_SEP);

        // Status bar with 3D effect
        int statusY = sepY + 8;
        drawShadow(ctx, contentX + PAD, statusY + 1, contentW - PAD * 2, 16, 2);
        ctx.fill(contentX + PAD, statusY, contentX + PAD + contentW - PAD * 2, statusY + 16, CARD_BG);
        drawBorder3D(ctx, contentX + PAD, statusY, contentW - PAD * 2, 16, BORDER_LIGHT, false);

        ctx.fill(contentX + PAD + 6, statusY + 5, contentX + PAD + 10, statusY + 11, SUCCESS_GREEN);
        ctx.drawTextWithShadow(textRenderer, "Status: Ready", contentX + PAD + 16, statusY + 4, TEXT_PRIMARY);

        // ── Scrollable Content ──────────────────────────────────────────
        contentTop = statusY + 20;
        contentH = height - contentTop - 10;

        ctx.enableScissor(contentX, contentTop, width, height - 10);

        int totalContentH = calcContentHeight();
        maxScroll = Math.max(0, totalContentH - contentH + PAD);
        scrollY = Math.max(0, Math.min(scrollY, maxScroll));

        int cy = contentTop - (int) scrollY;
        int cardAreaW = contentW - PAD * 2 - 6;

        MacroConfig cfg = MacroConfig.get();

        if ("optimizer".equals(currentCategory)) {
            drawOptimizerContent(ctx, cy, cardAreaW, mouseX, mouseY);
        } else {
            // Macro cards
            for (MacroDef def : MacroDef.ALL) {
                if (!def.category.equals(currentCategory)) continue;
                MacroConfig.MacroEntry entry = cfg.macros.get(def.id);
                if (entry == null) continue;

                boolean expanded = def.id.equals(expandedMacro);
                int fieldCount = 3 + def.slotNames.size();
                int cardH = expanded ? CARD_H + 4 + fieldCount * (FIELD_H + 4) + 4 : CARD_H;

                if (cy + cardH >= contentTop && cy <= contentTop + contentH) {
                    renderCard3D(ctx, def, entry, expanded, contentX + PAD, cy, cardAreaW, cardH, mouseX, mouseY);
                }
                cy += cardH + CARD_GAP;
            }
        }

        ctx.disableScissor();

        // Scrollbar
        drawScrollbar(ctx, maxScroll, mouseX, mouseY);
    }

    private void drawOptimizerContent(DrawContext ctx, int cy, int cardAreaW, int mouseX, int mouseY) {
        Optimizer opt = Optimizer.get();

        // Buttons with 3D effect
        int btnW = 90, btnH = 18;
        int btnX = contentX + PAD;

        boolean applyHov = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= cy && mouseY < cy + btnH;
        drawShadow(ctx, btnX, cy + 1, btnW, btnH, 2);
        ctx.fill(btnX, cy, btnX + btnW, cy + btnH, applyHov ? CARD_BG_HOVER : CARD_BG);
        drawBorder3D(ctx, btnX, cy, btnW, btnH, applyHov ? ACCENT_BLUE : BORDER_LIGHT, false);
        ctx.drawTextWithShadow(textRenderer, "Apply All", btnX + 15, cy + 4, ACCENT_BLUE);

        int revX = btnX + btnW + 8;
        boolean revHov = mouseX >= revX && mouseX < revX + btnW && mouseY >= cy && mouseY < cy + btnH;
        drawShadow(ctx, revX, cy + 1, btnW, btnH, 2);
        ctx.fill(revX, cy, revX + btnW, cy + btnH, revHov ? CARD_BG_HOVER : CARD_BG);
        drawBorder3D(ctx, revX, cy, btnW, btnH, revHov ? DANGER_RED : BORDER_LIGHT, false);
        ctx.drawTextWithShadow(textRenderer, "Revert All", revX + 12, cy + 4, DANGER_RED);

        int optCount = opt.countEnabled();
        ctx.drawTextWithShadow(textRenderer, optCount + "/" + Optimizer.ALL_OPTS.length + " applied",
                revX + btnW + 12, cy + 4, optCount > 0 ? SUCCESS_GREEN : TEXT_TERTIARY);

        cy += btnH + 10;

        // Optimizer cards
        for (Optimizer.OptDef od : Optimizer.ALL_OPTS) {
            boolean enabled = opt.isEnabled(od.id);
            int oh = 32;

            if (cy + oh >= contentTop && cy <= contentTop + contentH) {
                boolean rowHov = mouseX >= contentX + PAD && mouseX < contentX + PAD + cardAreaW &&
                                 mouseY >= cy && mouseY < cy + oh;

                renderOptimizerCard3D(ctx, od, enabled, rowHov, contentX + PAD, cy, cardAreaW, oh);
            }
            cy += oh + CARD_GAP;
        }
    }

    private void renderCard3D(DrawContext ctx, MacroDef def, MacroConfig.MacroEntry entry,
                              boolean expanded, int x, int y, int w, int h,
                              int mouseX, int mouseY) {
        boolean hov = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + CARD_H;
        int bgColor = entry.active ? CARD_BG_ACTIVE : (hov ? CARD_BG_HOVER : CARD_BG);

        // 3D shadow
        drawShadow(ctx, x + 2, y + 2, w - 4, h - 2, 3);

        ctx.fill(x, y, x + w, y + h, bgColor);
        drawBorder3D(ctx, x, y, w, h, entry.active ? ACCENT_BLUE : BORDER_LIGHT, entry.active);

        // Animated glow for active macros
        if (entry.active) {
            int glowWidth = (int)(Math.sin(pulsePhase) * 30 + 50);
            for (int i = 0; i < glowWidth && i < w / 3; i++) {
                int a = (int)(30 - i * 30 / glowWidth);
                if (a > 0) {
                    ctx.fill(x + 4 + i, y + 1, x + 5 + i, y + 2, (a << 24) | ACCENT_CYAN);
                }
            }
        }

        // Badge
        String badge = def.id.toUpperCase().substring(0, Math.min(3, def.id.length()));
        int badgeW = 28;
        ctx.fill(x + 6, y + 6, x + 6 + badgeW, y + CARD_H - 6, CARD_BG);
        drawBorder3D(ctx, x + 6, y + 6, badgeW, CARD_H - 12, entry.active ? ACCENT_CYAN : ACCENT_BLUE, false);
        ctx.drawTextWithShadow(textRenderer, badge, x + 10, y + 10, entry.active ? ACCENT_CYAN : ACCENT_BLUE);

        // Name and keybind
        ctx.drawTextWithShadow(textRenderer, def.name, x + 40, y + 7, TEXT_PRIMARY);
        String kbName = capturing && def.id.equals(captureId) ? "[...]" : "[" + MacroEngine.getBindName(entry.keybind) + "]";
        ctx.drawTextWithShadow(textRenderer, kbName, x + 40, y + 18, TEXT_TERTIARY);

        // Mode tag
        String modeTag = MacroConfig.MODE_NAMES[entry.mode];
        int modeColor = entry.mode == MacroConfig.MODE_HOLD ? SUCCESS_GREEN : ACCENT_CYAN;
        ctx.drawTextWithShadow(textRenderer, modeTag, x + w - 60, y + 10, modeColor);

        // Toggle
        int togX = x + w - 28;
        int togY = y + (CARD_H - 14) / 2;
        drawToggle3D(ctx, togX, togY, entry.active, mouseX, mouseY);

        // ── Expanded content ────────────────────────────────────────────
        if (expanded) {
            int fy = y + CARD_H + 4;
            drawField3D(ctx, x + 8, fy, w - 16, "Keybind", MacroEngine.getBindName(entry.keybind), mouseX, mouseY);
            fy += FIELD_H + 4;

            String delVal = editingDelay && def.id.equals(editingDelayId) ? delayBuffer + "|" : entry.delay + "ms";
            drawField3D(ctx, x + 8, fy, w - 16, "Delay", delVal, mouseX, mouseY);
            fy += FIELD_H + 4;

            String modeName = MacroConfig.MODE_NAMES[entry.mode];
            drawField3D(ctx, x + 8, fy, w - 16, "Mode", modeName, mouseX, mouseY);
            fy += FIELD_H + 4;

            for (String slot : def.slotNames) {
                int slotVal = entry.slots.getOrDefault(slot, -1);
                String valStr = slotVal >= 0 ? "Slot " + (slotVal + 1) : "None";
                drawField3D(ctx, x + 8, fy, w - 16, formatSlotLabel(slot), valStr, mouseX, mouseY);
                fy += FIELD_H + 4;
            }
        }
    }

    private void renderOptimizerCard3D(DrawContext ctx, Optimizer.OptDef od, boolean enabled, boolean hov,
                                        int x, int y, int w, int h) {
        int bgColor = enabled ? CARD_BG_ACTIVE : (hov ? CARD_BG_HOVER : CARD_BG);

        drawShadow(ctx, x + 2, y + 2, w - 4, h - 2, 2);
        ctx.fill(x, y, x + w, y + h, bgColor);
        drawBorder3D(ctx, x, y, w, h, enabled ? ACCENT_BLUE : BORDER_LIGHT, enabled);

        // Toggle
        drawToggle3D(ctx, x + 8, y + 8, enabled, 0, 0);

        // Title and description
        ctx.drawTextWithShadow(textRenderer, od.name, x + 36, y + 6, TEXT_PRIMARY);
        ctx.drawTextWithShadow(textRenderer, od.desc, x + 36, y + 16, TEXT_TERTIARY);

        // Impact badge
        int impactCol = "high".equals(od.impact) ? DANGER_RED : ("med".equals(od.impact) ? WARNING_YELLOW : TEXT_TERTIARY);
        String impactText = od.impact.toUpperCase();
        int impW = textRenderer.getWidth(impactText) + 8;
        ctx.fill(x + w - impW - 6, y + 8, x + w - 6, y + 24, CARD_BG);
        ctx.drawTextWithShadow(textRenderer, impactText, x + w - impW - 3, y + 12, impactCol);
    }

    private void drawField3D(DrawContext ctx, int fx, int fy, int fw, String label, String value,
                              int mx, int my) {
        int vx = fx + fw - FIELD_W;
        boolean hov = mx >= vx && mx < vx + FIELD_W && my >= fy && my < fy + FIELD_H;

        ctx.drawTextWithShadow(textRenderer, label, fx, fy + 5, TEXT_SECONDARY);

        int fbg = hov ? FIELD_BG : 0xFF0A0E18;
        drawShadow(ctx, vx + 1, fy + 1, FIELD_W - 2, FIELD_H - 2, 1);
        ctx.fill(vx, fy, vx + FIELD_W, fy + FIELD_H, fbg);
        drawBorder3D(ctx, vx, fy, FIELD_W, FIELD_H, hov ? ACCENT_BLUE : FIELD_BORDER, false);
        ctx.drawTextWithShadow(textRenderer, value, vx + 4, fy + 5, hov ? ACCENT_BLUE : TEXT_SECONDARY);
    }

    private void drawScrollbar(DrawContext ctx, int maxScroll, int mouseX, int mouseY) {
        if (maxScroll > 0) {
            int sbX = width - 6;
            ctx.fill(sbX, contentTop, sbX + 4, contentTop + contentH, SCROLL_TRACK);

            double thumbRatio = (double) contentH / (maxScroll + contentH + PAD);
            int thumbH = Math.max(12, (int)(contentH * thumbRatio));
            int thumbY = contentTop + (int)((contentH - thumbH) * (scrollY / Math.max(1, maxScroll)));

            boolean sbHov = mouseX >= sbX && mouseX < sbX + 5 && mouseY >= thumbY && mouseY < thumbY + thumbH;
            int thumbColor = (sbHov || draggingScroll) ? ACCENT_BLUE : SCROLL_THUMB;

            ctx.fill(sbX, thumbY, sbX + 4, thumbY + thumbH, thumbColor);
        }
    }

    private void drawCaptureOverlay(DrawContext ctx) {
        ctx.fill(0, 0, width, height, 0xAA050810);
        int ow = 280, oh = 100;
        int ox = (width - ow) / 2, oy = (height - oh) / 2;

        drawShadow(ctx, ox + 2, oy + 2, ow - 4, oh - 4, 4);
        ctx.fill(ox, oy, ox + ow, oy + oh, CARD_BG_ACTIVE);
        drawBorder3D(ctx, ox, oy, ow, oh, ACCENT_BLUE, true);

        ctx.drawCenteredTextWithShadow(textRenderer, "Press any key...", width / 2, oy + 20, TEXT_PRIMARY);
        ctx.drawCenteredTextWithShadow(textRenderer, "Binding: " + (captureId != null ? captureId.toUpperCase() : ""), 
                width / 2, oy + 40, ACCENT_CYAN);
        ctx.drawCenteredTextWithShadow(textRenderer, "ESC = cancel  |  DEL = unbind", width / 2, oy + 60, TEXT_TERTIARY);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  3D DRAWING HELPERS
    // ═══════════════════════════════════════════════════════════════════

    private void drawShadow(DrawContext ctx, int x, int y, int w, int h, int depth) {
        for (int i = 0; i < depth; i++) {
            int alpha = (int)(30 * (1.0 - i / (float) depth));
            ctx.fill(x + i, y + i, x + w + i, y + h + i, (alpha << 24) | CARD_SHADOW);
        }
    }

    private void drawBorder3D(DrawContext ctx, int x, int y, int w, int h, int color, boolean isActive) {
        // Top border (lighter)
        ctx.fill(x, y, x + w, y + 1, (80 << 24) | color);
        // Bottom border (darker)
        ctx.fill(x, y + h - 1, x + w, y + h, (40 << 24) | color);
        // Left border
        ctx.fill(x, y, x + 1, y + h, (60 << 24) | color);
        // Right border
        ctx.fill(x + w - 1, y, x + w, y + h, (40 << 24) | color);

        if (isActive) {
            // Inner glow
            for (int i = 0; i < 2; i++) {
                int a = 15 - i * 8;
                ctx.fill(x + i + 1, y + i + 1, x + w - i - 1, y + i + 2, (a << 24) | color);
            }
        }
    }

    private void drawToggle3D(DrawContext ctx, int x, int y, boolean on, int mx, int my) {
        int tw = 24, th = 12;
        boolean hov = mx >= x && mx < x + tw && my >= y && my < y + th;

        drawShadow(ctx, x + 1, y + 1, tw - 2, th - 2, 1);
        int bgColor = on ? ACCENT_BLUE : TOGGLE_OFF;
        if (hov) bgColor = on ? ACCENT_CYAN : 0xFF3A4F6A;

        ctx.fill(x, y, x + tw, y + th, bgColor);
        drawBorder3D(ctx, x, y, tw, th, on ? ACCENT_CYAN : BORDER_LIGHT, on);

        int dotX = on ? x + tw - 9 : x + 2;
        ctx.fill(dotX, y + 2, dotX + 7, y + th - 2, on ? TEXT_PRIMARY : TEXT_TERTIARY);
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONTENT HEIGHT
    // ═══════════════════════════════════════════════════════════════════

    private int calcContentHeight() {
        if ("optimizer".equals(currentCategory)) {
            return 28 + 10 + Optimizer.ALL_OPTS.length * (32 + CARD_GAP) + PAD;
        }
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
    //  INPUT HANDLING
    // ═══════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double hAmount, double vAmount) {
        if (capturing) return true;
        if (mouseX >= contentX && mouseY >= contentTop) {
            scrollY -= vAmount * 18;
            scrollY = Math.max(0, Math.min(scrollY, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, hAmount, vAmount);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

        // Sidebar navigation
        int ly = 10 + 22 + 18 + 8 + 12;
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
            ly += NAV_ITEM_H + 3;
        }

        // Scrollbar
        if (maxScroll > 0) {
            int sbX = width - 6;
            if (mouseX >= sbX && mouseX < sbX + 6 && mouseY >= contentTop) {
                draggingScroll = true;
                dragStartY = (int) mouseY;
                dragStartScroll = scrollY;
                return true;
            }
        }

        if (mouseX < contentX) return true;

        MacroConfig cfg = MacroConfig.get();
        int cx = contentX + PAD;
        int cy = contentTop - (int) scrollY;
        int cw = contentW - PAD * 2 - 6;

        if ("optimizer".equals(currentCategory)) {
            Optimizer opt = Optimizer.get();
            int btnW = 90, btnH = 18;
            if (mouseX >= cx && mouseX < cx + btnW && mouseY >= cy && mouseY < cy + btnH) {
                opt.applyAll();
                return true;
            }
            int revX = cx + btnW + 8;
            if (mouseX >= revX && mouseX < revX + btnW && mouseY >= cy && mouseY < cy + btnH) {
                opt.revertAll();
                return true;
            }
            cy += btnH + 10;

            for (Optimizer.OptDef od : Optimizer.ALL_OPTS) {
                int oh = 32;
                if (mouseX >= cx && mouseX < cx + cw && mouseY >= cy && mouseY < cy + oh) {
                    opt.toggle(od.id);
                    return true;
                }
                cy += oh + CARD_GAP;
            }
            return true;
        }

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
                int togX = cx + cw - 28;
                int togY = cy + (CARD_H - 14) / 2;
                if (mouseX >= togX && mouseX < togX + 24 && mouseY >= togY && mouseY < togY + 12) {
                    entry.active = !entry.active;
                    cfg.save();
                    return true;
                }

                // Header click
                if (mouseY >= cy && mouseY < cy + CARD_H && mouseX < togX) {
                    expandedMacro = expanded ? null : def.id;
                    return true;
                }

                // Expanded fields
                if (expanded) {
                    int fy = cy + CARD_H + 4;
                    int fx = cx + 8 + cw - 16 - FIELD_W;

                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        capturing = true;
                        captureId = def.id;
                        return true;
                    }
                    fy += FIELD_H + 4;

                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        editingDelay = true;
                        editingDelayId = def.id;
                        delayBuffer = String.valueOf(entry.delay);
                        return true;
                    }
                    fy += FIELD_H + 4;

                    if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                        entry.mode = (entry.mode + 1) % 3;
                        cfg.save();
                        return true;
                    }
                    fy += FIELD_H + 4;

                    for (String slot : def.slotNames) {
                        if (mouseX >= fx && mouseX < fx + FIELD_W && mouseY >= fy && mouseY < fy + FIELD_H) {
                            int val = entry.slots.getOrDefault(slot, -1);
                            val = (val + 2) % 10 - 1;
                            entry.slots.put(slot, val);
                            cfg.save();
                            return true;
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (capturing) {
            MacroConfig cfg = MacroConfig.get();
            MacroConfig.MacroEntry entry = cfg.macros.get(captureId);
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturing = false;
                captureId = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (entry != null) {
                    entry.keybind = 0;
                    cfg.save();
                }
                capturing = false;
                captureId = null;
                return true;
            }
            if (entry != null) {
                entry.keybind = keyCode;
                cfg.save();
            }
            capturing = false;
            captureId = null;
            return true;
        }

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
            return true;
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
                int val = Math.max(1, Math.min(99999, Integer.parseInt(delayBuffer)));
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

    // ═══════════════════════════════════════════════════════════════════
    //  UTILITY
    // ═══════════════════════════════════════════════════════════════════

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
        if (!label.isEmpty()) {
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
        }
        StringBuilder sb = new StringBuilder();
        for (char c : label.toCharArray()) {
            if (Character.isUpperCase(c) && !sb.isEmpty()) sb.append(' ');
            sb.append(c);
        }
        return sb.toString();
    }

    private static final int TOGGLE_OFF = 0xFF2D3E5F;
}
