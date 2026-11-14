package com.qidate.qisplan2.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import java.util.List;

/** 预览与实战共用的十槽圆环，几何坐标始终以轮盘中心为原点。 */
public final class AbilityWheelRenderer {
    /** 绘制工具不实例化。 */
    private AbilityWheelRenderer() {}

    /** 画背景、图标和名称，空槽不绑定任何能力。 */
    public static void draw(GuiGraphics g, Font font, List<GhostAbilityCatalog.Entry> entries,
            float inner, float outer, int selected, float opacity) {
        g.flush();
        for (int i = 0; i < entries.size(); i++) {
            double from = AbilityWheelLayout.start(i, entries.size()) + 0.012;
            double to = AbilityWheelLayout.start(i + 1, entries.size()) - 0.012;
            ring(g, from, to, inner, outer, ((int)(210 * opacity) << 24) | (i == selected ? 0x24656c : 0x101924));
            ring(g, from, to, outer - (i == selected ? 3 : 1), outer,
                    ((int)(240 * opacity) << 24) | (i == selected ? 0x80fff0 : 0x5b7584));
            ring(g, from, to, inner, inner + 1, ((int)(200 * opacity) << 24) | 0x5b7584);
        }
        for (int i = 0; i < entries.size(); i++) {
            double angle = -Math.PI / 2 + i * Math.PI * 2 / entries.size();
            float radius = (inner + outer) / 2;
            int x = (int)(Math.cos(angle) * radius), y = (int)(Math.sin(angle) * radius);
            var entry = entries.get(i);
            if (entry != null) g.blit(entry.ability().iconTexture(), x - 10, y - 13, 0, 0, 20, 20, 20, 20);
            g.pose().pushPose();
            g.pose().translate(x, y + (entry == null ? -3 : 10), 0);
            g.pose().scale(0.7f, 0.7f, 1);
            int textWidth = Math.max(35, (int)(2 * radius * Math.sin(Math.PI / entries.size()) / 0.7) - 8);
            var lines = font.split(Component.translatable(entry == null ? "screen.qisplan2.possession.empty_slot"
                    : entry.ability().translationKey()), textWidth);
            for (int line = 0; line < Math.min(2, lines.size()); line++)
                g.drawCenteredString(font, lines.get(line), 0, line * 10, i == selected ? 0x9affee : 0xc1d1dd);
            g.pose().popPose();
        }
    }

    /** 用小四边形拼接圆环扇区，避免额外纹理并支持任意 GUI 缩放和槽位数。 */
    private static void ring(GuiGraphics g, double from, double to, float inside, float outside, int color) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        var matrix = g.pose().last().pose();
        int steps = Math.max(2, (int)Math.ceil((to - from) * 28));
        for (int step = 0; step < steps; step++) {
            double a = from + (to - from) * step / steps, b = from + (to - from) * (step + 1) / steps;
            buffer.addVertex(matrix, (float)Math.cos(a) * inside, (float)Math.sin(a) * inside, 0).setColor(color);
            buffer.addVertex(matrix, (float)Math.cos(a) * outside, (float)Math.sin(a) * outside, 0).setColor(color);
            buffer.addVertex(matrix, (float)Math.cos(b) * outside, (float)Math.sin(b) * outside, 0).setColor(color);
            buffer.addVertex(matrix, (float)Math.cos(b) * inside, (float)Math.sin(b) * inside, 0).setColor(color);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
