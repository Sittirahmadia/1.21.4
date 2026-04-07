package com.crystalspk.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Simplified ImGUI-like helper for modern dark UI rendering
 * Compatible with Minecraft 1.21.4 and Android Zalith launcher
 */
public class ImGuiHelper {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    // Color Theme
    public static final int BG = 0xFF0A0E18;
    public static final int CARD_BG = 0xFF0F1428;
    public static final int PRIMARY = 0xFF3B82F6;
    public static final int SECONDARY = 0xFF06B6D4;
    public static final int ACCENT = 0xFF8B5CF6;
    public static final int SUCCESS = 0xFF10B981;
    public static final int WARNING = 0xFFF59E0B;
    public static final int DANGER = 0xFFEF4444;
    public static final int BORDER = 0xFF1A2A48;
    public static final int TEXT_PRIMARY = 0xFFE5E7EB;
    public static final int TEXT_SECONDARY = 0xFF9CA3AF;

    /**
     * Draw a modern button with 3D effect
     */
    public static boolean button(DrawContext ctx, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        boolean hov = isHover(x, y, w, h, mouseX, mouseY);
        int bgColor = hov ? 0xFF4F96FF : PRIMARY;

        drawShadow(ctx, x + 2, y + 2, w - 4, h - 4, 2);
        ctx.fill(x, y, x + w, y + h, bgColor);
        drawBorder(ctx, x, y, w, h, BORDER);

        ctx.drawCenteredTextWithShadow(MC.textRenderer, label, x + w / 2, y + h / 2 - 4, 0xFFFFFFFF);
        return hov;
    }

    /**
     * Draw a modern toggle/checkbox
     */
    public static void toggle(DrawContext ctx, int x, int y, int w, int h, String label, boolean value) {
        int bgColor = value ? SUCCESS : 0xFF2D3E5F;
        ctx.fill(x, y, x + w, y + h, bgColor);
        drawBorder(ctx, x, y, w, h, BORDER);

        int dotX = value ? x + w - 7 : x + 2;
        ctx.fill(dotX, y + 2, dotX + 5, y + h - 2, 0xFFFFFFFF);

        ctx.drawTextWithShadow(MC.textRenderer, label, x + w + 8, y + 1, TEXT_PRIMARY);
    }

    /**
     * Draw a modern slider
     */
    public static void slider(DrawContext ctx, int x, int y, int w, int h, String label, float value, float min, float max) {
        ctx.drawTextWithShadow(MC.textRenderer, label + ": " + String.format("%.0f", value), x, y - 10, TEXT_PRIMARY);

        ctx.fill(x, y, x + w, y + h, BORDER);
        drawBorder(ctx, x, y, w, h, BORDER);

        float percent = (value - min) / (max - min);
        int thumbX = (int)(x + percent * (w - 10));

        ctx.fill(thumbX, y - 2, thumbX + 10, y + h + 2, PRIMARY);
        drawBorder(ctx, thumbX, y - 2, 10, h + 4, PRIMARY);
    }

    /**
     * Draw a modern window/panel
     */
    public static void window(DrawContext ctx, int x, int y, int w, int h, String title) {
        drawShadow(ctx, x + 2, y + 2, w - 4, h - 4, 3);
        ctx.fill(x, y, x + w, y + h, CARD_BG);
        drawBorder(ctx, x, y, w, h, BORDER);

        // Title bar
        ctx.fill(x, y, x + w, y + 20, PRIMARY);
        ctx.drawTextWithShadow(MC.textRenderer, title, x + 8, y + 6, 0xFFFFFFFF);
    }

    /**
     * Draw text
     */
    public static void text(DrawContext ctx, int x, int y, String label) {
        ctx.drawTextWithShadow(MC.textRenderer, label, x, y, TEXT_PRIMARY);
    }

    /**
     * Draw a separator line
     */
    public static void separator(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, BORDER);
    }

    /**
     * Draw 3D border
     */
    public static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, (80 << 24) | color);
        ctx.fill(x, y + h - 1, x + w, y + h, (40 << 24) | color);
        ctx.fill(x, y, x + 1, y + h, (60 << 24) | color);
        ctx.fill(x + w - 1, y, x + w, y + h, (40 << 24) | color);
    }

    /**
     * Draw shadow effect
     */
    public static void drawShadow(DrawContext ctx, int x, int y, int w, int h, int depth) {
        for (int i = 0; i < depth; i++) {
            int alpha = (int)(30 * (1.0 - i / (float) depth));
            ctx.fill(x + i, y + i, x + w + i, y + h + i, (alpha << 24) | 0x050810);
        }
    }

    /**
     * Check if mouse is hovering
     */
    public static boolean isHover(int x, int y, int w, int h, int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    /**
     * Draw gradient background
     */
    public static void gradientBg(DrawContext ctx, int width, int height) {
        ctx.fill(0, 0, width, height, BG);
        for (int i = 0; i < 100; i++) {
            int a = (20 - i * 20 / 100);
            if (a > 0) {
                ctx.fill(width - 100 + i, 0, width - 99 + i, 60, (a << 24) | PRIMARY);
            }
        }
    }
}
