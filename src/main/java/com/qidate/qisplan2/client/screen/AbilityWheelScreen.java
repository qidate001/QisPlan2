package com.qidate.qisplan2.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.network.payload.GhostAbilityUsePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import java.util.List;

/** 以准星为中心的透明轮盘：不暂停世界，鼠标由原版 Screen 生命周期接管。 */
public final class AbilityWheelScreen extends Screen {
    private List<GhostAbilityCatalog.Entry> entries = List.of();
    private long opened;
    private boolean finished;
    private int selected = -1;
    private float outer;
    private float inner;

    /** 创建时不依赖世界；玩家数据在 init 中读取。 */
    public AbilityWheelScreen() { super(Component.translatable("hud.qisplan2.ability_wheel.title")); }

    /** 将光标放到取消区，避免打开瞬间误选上一次鼠标位置。 */
    @Override protected void init() {
        entries = minecraft.player == null ? List.of() : GhostAbilityCatalog.wheel(new PlayerGhostModuleHost(minecraft.player));
        outer = Math.min(132, Math.min(width, height) * 0.36f);
        inner = outer * 0.44f;
        opened = System.nanoTime();
        GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), minecraft.getWindow().getScreenWidth() / 2.0,
                minecraft.getWindow().getScreenHeight() / 2.0);
    }
    /** 轮盘始终不会暂停单人游戏。 */
    @Override public boolean isPauseScreen() { return false; }
    /** 保持世界可见，不应用 Screen 默认模糊或菜单底图。 */
    @Override public void renderBackground(GuiGraphics g, int mx, int my, float partial) {}
    /** 根据实际光标位置重新计算，确保松键时不依赖上一帧的选中缓存。 */
    private int currentSelection() {
        double x = minecraft.mouseHandler.xpos() * width / minecraft.getWindow().getScreenWidth();
        double y = minecraft.mouseHandler.ypos() * height / minecraft.getWindow().getScreenHeight();
        return AbilityWheelLayout.index(x - width / 2.0, y - height / 2.0, inner, outer, entries.size());
    }
    /** 一次打开最多发出一个请求；中心、外部和空列表只关闭轮盘。 */
    public void confirm() {
        if (finished) return;
        selected = currentSelection();
        finished = true;
        if (minecraft.player != null && minecraft.player.isAlive() && minecraft.isWindowActive() && selected >= 0) {
            var entry = entries.get(selected);
            PacketDistributor.sendToServer(new GhostAbilityUsePayload(entry.moduleId(), entry.ability().getId()));
        }
        minecraft.setScreen(null);
    }
    /** 取消不发送技能包，供 Esc、失焦和死亡调用。 */
    public void cancel() { if (!finished) { finished = true; minecraft.setScreen(null); } }
    /** Esc 走取消路径，不会被当成释放。 */
    @Override public void onClose() { cancel(); }
    /** 左键确认并吞掉点击，避免同一次输入继续攻击世界中的实体。 */
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (button == 0) { confirm(); return true; }
        if (button == 1) { cancel(); return true; }
        return true;
    }
    /** 120 毫秒展开，扇区背景和图标一起缩放；高亮立即响应鼠标。 */
    @Override public void render(GuiGraphics g, int mx, int my, float partial) {
        float progress = Math.min(1, (System.nanoTime() - opened) / 120_000_000f);
        float eased = 1 - (float)Math.pow(1 - progress, 3);
        float scale = 0.82f + 0.18f * eased;
        selected = currentSelection();
        int cx = width / 2, cy = height / 2;
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1);
        g.flush();
        int count = Math.max(1, entries.size());
        for (int i = 0; i < count; i++) {
            double start = AbilityWheelLayout.start(i, count);
            double end = start + Math.PI * 2 / count;
            boolean hover = i == selected;
            ring(g, start + 0.012, end - 0.012, inner, outer,
                    ((int)((hover ? 230 : 200) * eased) << 24) | (hover ? 0x24656c : 0x101924));
            ring(g, start + 0.012, end - 0.012, outer - (hover ? 3 : 1), outer,
                    ((int)(240 * eased) << 24) | (hover ? 0x80fff0 : 0x5b7584));
            ring(g, start + 0.012, end - 0.012, inner, inner + 1,
                    ((int)(200 * eased) << 24) | 0x5b7584);
        }
        for (int i = 0; i < entries.size(); i++) {
            double angle = -Math.PI / 2 + i * Math.PI * 2 / entries.size();
            float radius = (inner + outer) / 2;
            int x = (int)(Math.cos(angle) * radius), y = (int)(Math.sin(angle) * radius);
            g.blit(entries.get(i).ability().iconTexture(), x - 12, y - 12, 0, 0, 24, 24, 24, 24);
            g.drawCenteredString(font, Integer.toString(i + 1), x, y + 14, i == selected ? 0x9affee : 0x9baebd);
        }
        Component label = selected < 0 ? Component.translatable(entries.isEmpty()
                ? "hud.qisplan2.ability_wheel.empty" : "hud.qisplan2.ability_wheel.cancel")
                : Component.translatable(entries.get(selected).ability().translationKey());
        var lines = font.split(label, (int)(inner * 1.7));
        int y = -lines.size() * 5;
        for (var line : lines) { g.drawCenteredString(font, line, 0, y, 0xf0fffc); y += 10; }
        if (selected >= 0) {
            var id = entries.get(selected).moduleId();
            var origin = Component.translatable("module." + id.getNamespace() + "." + id.getPath());
            var originLines = font.split(origin, (int)(inner * 1.7));
            for (var line : originLines) { g.drawCenteredString(font, line, 0, y + 5, 0x8aa8b5); y += 10; }
        }
        g.pose().popPose();
        g.drawCenteredString(font, Component.translatable("hud.qisplan2.ability_wheel.hint"), cx,
                (int)(cy + outer + 12), 0xb3c8d4);
    }
    /** 用小四边形拼接圆环扇区，避免额外纹理并支持任意 GUI 缩放和槽位数。 */
    private void ring(GuiGraphics g, double from, double to, float inside, float outside, int color) {
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
