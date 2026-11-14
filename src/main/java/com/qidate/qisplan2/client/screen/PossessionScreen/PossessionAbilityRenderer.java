package com.qidate.qisplan2.client.screen.PossessionScreen;

import com.qidate.qisplan2.client.screen.AbilityWheelLayout;
import com.qidate.qisplan2.client.screen.AbilityWheelRenderer;
import com.qidate.qisplan2.client.screen.AbilityWheelPreferences;
import com.qidate.qisplan2.client.screen.AbilityWheelSlots;
import com.qidate.qisplan2.ghost.module.GhostModuleRegistry;
import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** 总面板：左侧按完整厉鬼分组，右侧编辑真实轮盘使用的十槽布局。 */
public final class PossessionAbilityRenderer {
    private static final int LEFT = 12, TOP = 48, WIDTH = 274, HEIGHT = 224;
    private static final int CX = 442, CY = 159;
    private static final float OUTER = 111, INNER = 47;
    private final PossessionScrollbar scrollbar = new PossessionScrollbar(289, TOP, HEIGHT);
    private AbilityWheelSlots layout = AbilityWheelPreferences.current();
    private List<GhostAbilityCatalog.Entry> available = List.of();
    private final List<Chip> chips = new ArrayList<>();
    private AbilityWheelSlots.Key dragged;
    private int sourceSlot = -1;
    private int contentHeight;
    private String message = "screen.qisplan2.possession.layout_hint";

    /** 技能卡的命中框使用面板局部坐标，与缩放无关。 */
    private record Chip(int x, int y, GhostAbilityCatalog.Entry entry) {}

    /** 获取翻译组件，避免把显示名称保存到配置中。 */
    private static Component text(String key) { return Component.translatable("screen.qisplan2.possession." + key); }

    /** 更新实时能力目录；已移除的能力只能显示空槽，不能发出释放请求。 */
    private void refresh() {
        var player = Minecraft.getInstance().player;
        available = player == null ? List.of() : GhostAbilityCatalog.all(new PlayerGhostModuleHost(player));
    }

    /** 绘制列表、共用轮盘预览和浮动拖拽卡片。 */
    public void render(GuiGraphics g, int panelX, int panelY, float scale, double mx, double my) {
        refresh();
        var mc = Minecraft.getInstance();
        var font = mc.font;
        g.fill(8, 30, 298, 279, 0x70222c37);
        g.drawString(font, text("abilities"), 14, 34, 0xcde5e8);
        g.drawCenteredString(font, text("preview"), CX, 34, 0xcde5e8);
        chips.clear();
        g.enableScissor((int)(panelX + LEFT * scale), (int)(panelY + TOP * scale),
                (int)(panelX + (LEFT + WIDTH) * scale), (int)(panelY + (TOP + HEIGHT) * scale));
        int offset = 0;
        if (mc.player != null) for (var instance : new PlayerGhostModuleHost(mc.player).getModules()) {
            var module = GhostModuleRegistry.get(instance.id());
            if (module == null) continue;
            var abilities = available.stream().filter(e -> e.moduleId().equals(instance.id())).toList();
            int y = TOP + offset - (int)scrollbar.getScroll();
            ResourceLocation icon = module.iconTexture();
            if (icon == null) icon = ResourceLocation.parse("minecraft:textures/item/nether_star.png");
            g.blit(icon, LEFT + 3, y + 2, 0, 0, 18, 18, 18, 18);
            g.drawString(font, font.substrByWidth(module.displayName(), WIDTH - 32).getString(), LEFT + 27, y + 7, 0xf1f6f8);
            offset += 25;
            if (abilities.isEmpty()) {
                g.drawString(font, text("no_abilities"), LEFT + 27, TOP + offset - (int)scrollbar.getScroll(), 0x82939e);
                offset += 18;
            }
            for (int i = 0; i < abilities.size(); i++) {
                int x = LEFT + (i % 2) * 136;
                int chipY = TOP + offset + (i / 2) * 35 - (int)scrollbar.getScroll();
                var entry = abilities.get(i);
                boolean assigned = layout.snapshot().contains(AbilityWheelSlots.Key.of(entry));
                boolean hover = inside(mx, my, x, chipY, 131, 31) && inside(mx, my, LEFT, TOP, WIDTH, HEIGHT);
                g.fill(x, chipY, x + 131, chipY + 31, hover ? 0xff315560 : assigned ? 0xff263e46 : 0xff1c2833);
                g.blit(entry.ability().iconTexture(), x + 5, chipY + 6, 0, 0, 18, 18, 18, 18);
                var lines = font.split(Component.translatable(entry.ability().translationKey()), 101);
                for (int line = 0; line < Math.min(2, lines.size()); line++)
                    g.drawString(font, lines.get(line), x + 27, chipY + 5 + line * 10, 0xe1eeef);
                chips.add(new Chip(x, chipY, entry));
            }
            offset += ((abilities.size() + 1) / 2) * 35 + 8;
        }
        if (offset == 0) g.drawString(font, text("no_ghosts"), LEFT + 5, TOP + 10, 0xa9bcc8);
        contentHeight = offset;
        g.disableScissor();
        scrollbar.setScroll(scrollbar.getScroll(), contentHeight);
        scrollbar.render(g, contentHeight);
        int selected = slot(mx, my);
        var resolved = layout.resolve(available);
        g.pose().pushPose();
        g.pose().translate(CX, CY, 0);
        AbilityWheelRenderer.draw(g, font, resolved, INNER, OUTER, selected, 1);
        Component center = text(dragged != null ? (sourceSlot >= 0 ? "drop_remove" : "drag_to_slot") : "drag_to_slot");
        if (dragged == null && selected >= 0 && resolved.get(selected) != null)
            center = Component.translatable(resolved.get(selected).ability().translationKey());
        var lines = font.split(center, 79);
        for (int i = 0; i < Math.min(4, lines.size()); i++)
            g.drawCenteredString(font, lines.get(i), 0, -lines.size() * 5 + i * 10, 0xcde5e8);
        g.pose().popPose();
        button(g, 12, "reset");
        button(g, 112, "save");
        var hints = font.split(Component.translatable(message), 380);
        for (int i = 0; i < Math.min(2, hints.size()); i++) g.drawString(font, hints.get(i), 210, 289 + i * 10, 0x9bb6c2);
        if (dragged != null) {
            var key = dragged;
            var entry = available.stream().filter(e -> AbilityWheelSlots.Key.of(e).equals(key)).findFirst().orElse(null);
            if (entry != null) {
                g.pose().pushPose(); g.pose().translate(0, 0, 300);
                g.fill((int)mx + 7, (int)my + 5, (int)mx + 151, (int)my + 32, 0xf0355963);
                g.blit(entry.ability().iconTexture(), (int)mx + 11, (int)my + 9, 0, 0, 18, 18, 18, 18);
                g.drawString(font, font.substrByWidth(Component.translatable(entry.ability().translationKey()), 113).getString(),
                        (int)mx + 33, (int)my + 14, 0xffffff);
                g.pose().popPose();
            }
        }
    }
    /** 底部小按钮使用与现有面板一致的矩形样式。 */
    private void button(GuiGraphics g, int x, String key) {
        g.fill(x, 288, x + 92, 309, 0xff303f4b);
        g.drawCenteredString(Minecraft.getInstance().font, text(key), x + 46, 295, 0xe2f2f4);
    }
    /** 鼠标与缩放后的面板统一使用局部坐标命中。 */
    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
    /** 与实战轮盘使用相同的十扇区方向。 */
    private int slot(double x, double y) { return AbilityWheelLayout.index(x - CX, y - CY, INNER, OUTER, AbilityWheelSlots.SIZE); }
    /** 选择拖动源；右键直接清空，左键在松开时才提交变更。 */
    public boolean mouseClicked(double x, double y, int button) {
        refresh();
        int index = slot(x, y);
        if (button == 1 && index >= 0) { layout.clear(index); save(); return true; }
        if (button != 0) return false;
        if (inside(x, y, 12, 288, 92, 21)) {
            layout = new AbilityWheelSlots(available.stream().limit(10).map(AbilityWheelSlots.Key::of).toList());
            save(); return true;
        }
        if (inside(x, y, 112, 288, 92, 21)) { save(); return true; }
        if (scrollbar.mouseClicked(x, y, contentHeight)) return true;
        if (index >= 0) {
            dragged = layout.snapshot().get(index); sourceSlot = index; return true;
        }
        if (inside(x, y, LEFT, TOP, WIDTH, HEIGHT)) for (var chip : chips) {
            if (inside(x, y, chip.x(), chip.y(), 131, 31)) {
                dragged = AbilityWheelSlots.Key.of(chip.entry()); sourceSlot = -1; return true;
            }
        }
        return false;
    }
    /** 拖拽滚动条或保留技能拖动状态，绘制时使用当前鼠标位置。 */
    public boolean mouseDragged(double x, double y) { return scrollbar.mouseDragged(x, y, contentHeight) || dragged != null; }
    /** 有效扇区放置或交换；拖到中心移除原槽，其余位置取消。 */
    public boolean mouseReleased(double x, double y) {
        boolean scrolled = scrollbar.mouseReleased();
        if (dragged == null) return scrolled;
        refresh();
        int index = slot(x, y);
        var key = dragged;
        if (index >= 0 && available.stream().anyMatch(e -> AbilityWheelSlots.Key.of(e).equals(key))) {
            layout.assign(index, dragged); save();
        } else if (sourceSlot >= 0 && Math.hypot(x - CX, y - CY) < INNER) {
            layout.clear(sourceSlot); save();
        }
        cancelDrag(); return true;
    }
    /** 滚轮仅影响左侧技能列表。 */
    public boolean mouseScrolled(double x, double y, double amount) {
        return inside(x, y, LEFT, TOP, WIDTH + 10, HEIGHT) && scrollbar.scroll(amount, contentHeight);
    }
    /** 切页或关闭时丢弃未完成的拖动，不误改配置。 */
    public void cancelDrag() { dragged = null; sourceSlot = -1; scrollbar.mouseReleased(); }
    /** 每次完成编辑即保存，并显示实际保存结果。 */
    private void save() { message = AbilityWheelPreferences.save(layout)
            ? "screen.qisplan2.possession.saved" : "screen.qisplan2.possession.save_failed"; }
}
