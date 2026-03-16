package com.raven.ravenz.utils.render.font.util;

import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;

public final class BufferUtils {
    private BufferUtils() {
    }

    public static void draw(BufferBuilder builder) {
        try (BuiltBuffer builtBuffer = builder.end()) {
            try {
                selectDefaultLayer(builtBuffer.getDrawParameters().mode()).draw(builtBuffer);
            } catch (Throwable ignored) {
                try {
                    Class<?> bufferRenderer = Class.forName("net.minecraft.client.render.BufferRenderer");
                    bufferRenderer.getMethod("drawWithGlobalProgram", BuiltBuffer.class).invoke(null, builtBuffer);
                } catch (Throwable ignored2) {
                }
            }
        }
    }

    public static void draw(BufferBuilder builder, RenderLayer layer) {
        try (BuiltBuffer builtBuffer = builder.end()) {
            layer.draw(builtBuffer);
        }
    }

    private static RenderLayer selectDefaultLayer(VertexFormat.DrawMode mode) {
        return switch (mode) {
            case DEBUG_LINES, DEBUG_LINE_STRIP -> RenderLayer.getLines();
            default -> RenderLayer.getGui();
        };
    }
}
