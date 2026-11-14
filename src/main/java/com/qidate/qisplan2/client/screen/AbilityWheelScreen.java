package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import com.qidate.qisplan2.network.payload.GhostAbilityUsePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Objects;

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
        entries = minecraft.player == null ? List.of() : AbilityWheelPreferences.current().resolve(GhostAbilityCatalog.all(new PlayerGhostModuleHost(minecraft.player)));
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
        if (minecraft.player != null && minecraft.player.isAlive() && minecraft.isWindowActive() && selected >= 0 && entries.get(selected) != null) {
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
        AbilityWheelRenderer.draw(g, font, entries, inner, outer, selected, eased);
        Component label = selected < 0 || entries.get(selected) == null ? Component.translatable(entries.stream().allMatch(Objects::isNull)
                ? "hud.qisplan2.ability_wheel.empty" : "hud.qisplan2.ability_wheel.cancel")
                : Component.translatable(entries.get(selected).ability().translationKey());
        var lines = font.split(label, (int)(inner * 1.7));
        int y = -lines.size() * 5;
        for (var line : lines) { g.drawCenteredString(font, line, 0, y, 0xf0fffc); y += 10; }
        if (selected >= 0 && entries.get(selected) != null) {
            var id = entries.get(selected).moduleId();
            var origin = Component.translatable("module." + id.getNamespace() + "." + id.getPath());
            var originLines = font.split(origin, (int)(inner * 1.7));
            for (var line : originLines) { g.drawCenteredString(font, line, 0, y + 5, 0x8aa8b5); y += 10; }
        }
        g.pose().popPose();
        g.drawCenteredString(font, Component.translatable("hud.qisplan2.ability_wheel.hint"), cx,
                (int)(cy + outer + 12), 0xb3c8d4);
    }
}
