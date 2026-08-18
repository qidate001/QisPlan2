package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 心形条上方的必死诅咒骷髅条
 * <p>
 * NeoForge 1.21.1 的 HUD 采用分层渲染系统，
 * 必须通过 {@link RegisterGuiLayersEvent} 在 mod 事件总线上注册一个 {@link net.minecraft.client.gui.LayeredDraw.Layer}，
 * 不能再使用已不触发的 RenderGuiEvent / RenderGuiLayerEvent。
 */
@EventBusSubscriber(modid = QisPlan2.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DeathCurseHudOverlay {

    private static final int MAX_CURSE = 10;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 18;

    // 空格骷髅的亮度系数（深 30% = 70% 亮度）
    private static final float EMPTY_SLOT_BRIGHTNESS = 0.7F;

    // 调试：只在诅咒值变化时打印一次日志
    private static int lastLoggedCurse = -1;

    /**
     * 注册 HUD 图层，渲染在所有图层之上
     */
    @SubscribeEvent
    public static void registerDeathCurseLayer(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(QisPlan2.MODID, "death_curse_bar"),
                DeathCurseHudOverlay::renderDeathCurseBar
        );
        QisPlan2.LOGGER.info("[QisPlan2] Death curse HUD layer registered");
    }

    private static void renderDeathCurseBar(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {

        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        int curse = player.getData(QisPlan2.DEATH_CURSE_COUNT.get());

        if (curse != lastLoggedCurse) {
            lastLoggedCurse = curse;
            QisPlan2.LOGGER.info("[QisPlan2] DeathCurseHudOverlay: client curse = {}", curse);
        }

        // 没有诅咒时不显示
        if (curse <= 0) {
            return;
        }

        // 心形条 y（原版固定为 guiHeight - 39），骷髅条画在其正上方
        int heartTop = guiGraphics.guiHeight() - 39;
        int y = heartTop - SLOT_SIZE - 1;

        // 10 格水平居中于屏幕
        int totalWidth = (MAX_CURSE - 1) * SLOT_SPACING + SLOT_SIZE;
        int startX = guiGraphics.guiWidth() / 2 - totalWidth / 2;

        ItemStack skull = new ItemStack(Items.SKELETON_SKULL);

        for (int i = 0; i < MAX_CURSE; i++) {
            int x = startX + i * SLOT_SPACING;

            // 未积累的格子：渲染成深 30% 的颜色（70% 亮度）
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

            if (empty) {
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}
