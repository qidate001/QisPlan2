package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 心形条上方的必死诅咒骷髅条
 *
 * 使用 NeoForge HUD Layer 系统进行渲染。
 */
public class DeathCurseHudOverlay {

    private static final int MAX_CURSE = 10;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;

    // 空格骷髅的亮度系数
    private static final float EMPTY_SLOT_BRIGHTNESS = 0.4F;

    /**
     * 注册 HUD 图层
     *
     * 由 QisPlan2 主类通过 Mod Event Bus 调用。
     */
    public static void registerDeathCurseLayer(RegisterGuiLayersEvent event) {

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "death_curse_bar"
                ),
                DeathCurseHudOverlay::renderDeathCurseBar
        );

        QisPlan2.LOGGER.info("[QisPlan2] Death curse HUD layer registered");
    }

    /**
     * 渲染必死诅咒骷髅条
     */
    private static void renderDeathCurseBar(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker
    ) {

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        // 玩家不存在时不渲染
        if (player == null) {
            return;
        }

        int curse = player.getData(QisPlan2.DEATH_CURSE_COUNT.get());

        // 没有诅咒时不显示
        if (curse <= 0) {
            return;
        }

        // 防止异常数据超过最大值
        curse = Math.min(curse, MAX_CURSE);

        // 心形条 y
        int heartTop = guiGraphics.guiHeight() - 39;

        // 骷髅条位于心形条正上方
        int y = heartTop - SLOT_SIZE - 1;

        // 10 格水平居中
        int totalWidth = (MAX_CURSE - 1) * SLOT_SPACING + SLOT_SIZE;
        int startX = guiGraphics.guiWidth() / 2 - totalWidth / 2;

        ItemStack skull = new ItemStack(Items.SKELETON_SKULL);

        for (int i = 0; i < MAX_CURSE; i++) {

            int x = startX + i * SLOT_SPACING;

            // 当前诅咒数量以外的格子显示为较暗状态
            boolean empty = i >= curse;

            if (empty) {
                guiGraphics.setColor(
                        EMPTY_SLOT_BRIGHTNESS,
                        EMPTY_SLOT_BRIGHTNESS,
                        EMPTY_SLOT_BRIGHTNESS,
                        1.0F
                );
            }

            guiGraphics.renderItem(skull, x, y);

            // 恢复默认颜色
            if (empty) {
                guiGraphics.setColor(
                        1.0F,
                        1.0F,
                        1.0F,
                        1.0F
                );
            }
        }
    }
}