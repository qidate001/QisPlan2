package com.qidate.qisplan2.client.screen;

import com.qidate.qisplan2.ghost.module.ability.GhostAbilityCatalog;
import com.qidate.qisplan2.network.payload.GhostAbilityUsePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** 所有声明整数输入的技能共用的输入面板，不依赖任何具体模块 ID。 */
public final class GhostAbilityInputScreen extends Screen {
    private final GhostAbilityCatalog.Entry entry;
    private EditBox input;
    /** 按声明决定立即发送还是打开输入，不在客户端执行能力。 */
    public static void invoke(GhostAbilityCatalog.Entry entry) {
        if (entry.ability().integerInput() == null)
            PacketDistributor.sendToServer(new GhostAbilityUsePayload(entry.moduleId(), entry.ability().getId()));
        else Minecraft.getInstance().setScreen(new GhostAbilityInputScreen(entry));
    }
    /** 使用模块提供的能力名称作为标题。 */
    private GhostAbilityInputScreen(GhostAbilityCatalog.Entry entry) {
        super(Component.translatable(entry.ability().translationKey())); this.entry = entry;
    }
    /** 创建有界数字输入；服务器也会独立验证范围。 */
    protected void init() {
        input = new EditBox(font, width/2-80, height/2-12, 160, 20, title);
        input.setMaxLength(11); input.setFilter(value -> value.matches("-?[0-9]*"));
        input.setValue(Integer.toString(entry.ability().integerInput().initial()));
        addRenderableWidget(input); setInitialFocus(input);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> confirm())
                .bounds(width/2-40,height/2+20,80,20).build());
    }
    /** 确认合法整数后发送一次请求，取消窗口则没有效果。 */
    private void confirm() {
        try {
            int number = Integer.parseInt(input.getValue());
            if (!entry.ability().integerInput().accepts(number)) return;
            PacketDistributor.sendToServer(new GhostAbilityUsePayload(entry.moduleId(), entry.ability().getId(), number));
            onClose();
        } catch (NumberFormatException ignored) { }
    }
    /** 回车确认，其余按键交给原版输入控件。 */
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == 257 || key == 335) { confirm(); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    /** 绘制背景、标题和表单。 */
    public void render(GuiGraphics g, int x, int y, float partial) {
        renderBackground(g,x,y,partial); g.drawCenteredString(font,title,width/2,height/2-40,0xFFFFFF);
        super.render(g,x,y,partial);
    }
    /** 编辑时世界继续运行，提交时服务端会重新验证模块是否仍在。 */
    public boolean isPauseScreen() { return false; }
}
