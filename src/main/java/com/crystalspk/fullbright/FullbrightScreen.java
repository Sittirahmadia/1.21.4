package com.crystalspk.fullbright;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Fullbright settings screen
 */
public class FullbrightScreen extends Screen {
    private static final int BG = 0xFF0A0E18;
    private static final int CARD_BG = 0xFF0F1428;
    private static final int CARD_BORDER = 0xFF1A2A48;
    private static final int ACCENT_BLUE = 0xFF3B82F6;
    private static final int TEXT_PRIMARY = 0xFFE5E7EB;
    private static final int TEXT_SECONDARY = 0xFF9CA3AF;

    private final Screen parent;
    private FullbrightModule fullbright = FullbrightModule.getInstance();
    private FullbrightConfig config = FullbrightConfig.getInstance();

    private int sliderX, sliderY, sliderW, sliderH;
    private boolean draggingSlider = false;

    public FullbrightScreen(Screen parent) {
        super(Text.literal("Fullbright Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        sliderX = width / 2 - 100;
        sliderY = height / 2 - 40;
        sliderW = 200;
        sliderH = 20;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        // Background
        ctx.fill(0, 0, width, height, BG);

        // Title
        ctx.drawCenteredTextWithShadow(textRenderer, "Fullbright Settings", width / 2, 20, TEXT_PRIMARY);

        // Enable/Disable button
        int btnW = 120, btnH = 20;
        int btnX = width / 2 - btnW / 2;
        int btnY = height / 2 - 80;
        boolean btnHov = mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH;

        int btnBg = fullbright.isEnabled() ? 0xFF10B981 : CARD_BG;
        ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnBg);
        drawBorder(ctx, btnX, btnY, btnW, btnH, CARD_BORDER);

        String btnText = fullbright.isEnabled() ? "Enabled" : "Disabled";
        int btnTextColor = fullbright.isEnabled() ? 0xFFFFFFFF : TEXT_SECONDARY;
        ctx.drawCenteredTextWithShadow(textRenderer, btnText, btnX + btnW / 2, btnY + 6, btnTextColor);

        // Gamma label
        ctx.drawTextWithShadow(textRenderer, "Gamma: " + String.format("%.0f", fullbright.getGamma()), 
                sliderX, sliderY - 20, TEXT_PRIMARY);

        // Slider background
        ctx.fill(sliderX, sliderY, sliderX + sliderW, sliderY + sliderH, CARD_BG);
        drawBorder(ctx, sliderX, sliderY, sliderW, sliderH, CARD_BORDER);

        // Slider thumb
        float gammaPercent = fullbright.getGamma() / 1000.0f;
        int thumbX = (int)(sliderX + gammaPercent * (sliderW - 10));
        ctx.fill(thumbX, sliderY + 2, thumbX + 10, sliderY + sliderH - 2, ACCENT_BLUE);

        // Gamma range info
        ctx.drawTextWithShadow(textRenderer, "0", sliderX - 15, sliderY + 5, TEXT_SECONDARY);
        ctx.drawTextWithShadow(textRenderer, "1000", sliderX + sliderW + 5, sliderY + 5, TEXT_SECONDARY);

        // Presets
        int presetY = sliderY + 50;
        ctx.drawTextWithShadow(textRenderer, "Presets:", sliderX, presetY, TEXT_PRIMARY);

        int presets[] = {100, 250, 500, 750, 1000};
        String presetLabels[] = {"Normal", "Low", "Mid", "High", "Max"};
        int presetX = sliderX;

        for (int i = 0; i < presets.length; i++) {
            int pX = presetX + (i * 40);
            int pY = presetY + 20;
            int pW = 35, pH = 16;
            boolean pHov = mouseX >= pX && mouseX < pX + pW && mouseY >= pY && mouseY < pY + pH;

            ctx.fill(pX, pY, pX + pW, pY + pH, pHov ? ACCENT_BLUE : CARD_BG);
            drawBorder(ctx, pX, pY, pW, pH, CARD_BORDER);
            ctx.drawCenteredTextWithShadow(textRenderer, presetLabels[i], pX + pW / 2, pY + 4, 
                    fullbright.getGamma() == presets[i] ? 0xFFFFFFFF : TEXT_SECONDARY);
        }

        // Close button
        int closeW = 100, closeH = 20;
        int closeX = width / 2 - closeW / 2;
        int closeY = height - 40;
        boolean closeHov = mouseX >= closeX && mouseX < closeX + closeW && mouseY >= closeY && mouseY < closeY + closeH;

        ctx.fill(closeX, closeY, closeX + closeW, closeY + closeH, closeHov ? 0xFF4B5563 : CARD_BG);
        drawBorder(ctx, closeX, closeY, closeW, closeH, CARD_BORDER);
        ctx.drawCenteredTextWithShadow(textRenderer, "Close", closeX + closeW / 2, closeY + 6, TEXT_PRIMARY);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        // Enable/Disable button
        int btnW = 120, btnH = 20;
        int btnX = width / 2 - btnW / 2;
        int btnY = height / 2 - 80;
        if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= btnY && mouseY < btnY + btnH) {
            fullbright.toggle();
            if (fullbright.isEnabled()) {
                fullbright.setGamma(config.getGamma());
            }
            return true;
        }

        // Slider
        if (mouseX >= sliderX && mouseX < sliderX + sliderW && mouseY >= sliderY && mouseY < sliderY + sliderH) {
            draggingSlider = true;
            updateGammaFromMouse(mouseX);
            return true;
        }

        // Presets
        int presetY = sliderY + 50;
        int presets[] = {100, 250, 500, 750, 1000};
        int presetX = sliderX;

        for (int i = 0; i < presets.length; i++) {
            int pX = presetX + (i * 40);
            int pY = presetY + 20;
            int pW = 35, pH = 16;
            if (mouseX >= pX && mouseX < pX + pW && mouseY >= pY && mouseY < pY + pH) {
                fullbright.setGamma(presets[i]);
                config.setGamma(presets[i]);
                return true;
            }
        }

        // Close button
        int closeW = 100, closeH = 20;
        int closeX = width / 2 - closeW / 2;
        int closeY = height - 40;
        if (mouseX >= closeX && mouseX < closeX + closeW && mouseY >= closeY && mouseY < closeY + closeH) {
            config.setGamma(fullbright.getGamma());
            config.save();
            this.close();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingSlider && button == 0) {
            updateGammaFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingSlider = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateGammaFromMouse(double mouseX) {
        float percent = (float) ((mouseX - sliderX) / (double) sliderW);
        percent = Math.max(0, Math.min(1, percent));
        fullbright.setGamma(percent * 1000);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void close() {
        this.client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
