package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * 必死诅咒骷髅条
 *
 * 左上角：
 * 玩家自己的诅咒
 *
 * 右上角：
 * 上一次攻击目标的诅咒
 */
public class DeathCurseHudOverlay {

    private static final int MAX_CURSE = 10;
    private static final int SLOT_SIZE = 12;
    private static final int SLOT_SPACING = 12;

    // 空骷髅的亮度系数
    private static final float EMPTY_SLOT_BRIGHTNESS = 0.4F;

    /**
     * 注册 HUD 图层
     */
    public static void registerDeathCurseLayer(
            RegisterGuiLayersEvent event
    ) {

        event.registerAboveAll(
                ResourceLocation.fromNamespaceAndPath(
                        QisPlan2.MODID,
                        "death_curse_bar"
                ),
                DeathCurseHudOverlay::renderDeathCurseBar
        );

        QisPlan2.LOGGER.info(
                "[QisPlan2] Death curse HUD layer registered"
        );
    }

    /**
     * 渲染死亡诅咒 HUD
     */
    private static void renderDeathCurseBar(
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker
    ) {

        Minecraft minecraft =
                Minecraft.getInstance();

        Player player =
                minecraft.player;

        if (player == null || minecraft.level == null) {
            return;
        }


        /*
         * ========================================
         * 左上角：自己的诅咒
         * ========================================
         */

        int playerCurse =
                player.getData(
                        QisPlan2.DEATH_CURSE_COUNT.get()
                );

        if (playerCurse > 0) {

            playerCurse =
                    Math.min(
                            playerCurse,
                            MAX_CURSE
                    );

            renderCurseBar(
                    guiGraphics,
                    playerCurse,
                    8,
                    8
            );
        }


        /*
         * ========================================
         * 右上角：最后攻击目标
         * ========================================
         */

        LivingEntity target =
                DeathCurseClientHandler.getLastAttackTarget();

        if (target == null) {
            return;
        }

        /*
         * 目标已经被移除
         */
        if (target.isRemoved()) {

            DeathCurseClientHandler.clearLastAttackTarget();
            return;
        }

        /*
         * 目标不在当前世界
         */
        if (target.level() != minecraft.level) {

            DeathCurseClientHandler.clearLastAttackTarget();
            return;
        }

        /*
         * 目标已经死亡
         */
        if (target.isDeadOrDying()) {

            DeathCurseClientHandler.clearLastAttackTarget();
            return;
        }


        int targetCurse =
                target.getData(
                        QisPlan2.DEATH_CURSE_COUNT.get()
                );

        /*
         * 目标没有诅咒时不显示
         */
        if (targetCurse <= 0) {
            return;
        }

        targetCurse =
                Math.min(
                        targetCurse,
                        MAX_CURSE
                );


        /*
         * ========================================
         * 计算右上角位置
         * ========================================
         */

        int barWidth =
                (MAX_CURSE - 1) * SLOT_SPACING
                        + SLOT_SIZE;

        int screenWidth =
                guiGraphics.guiWidth();

        int startX =
                screenWidth
                        - 8
                        - barWidth;

        int startY = 8;

        renderCurseBar(
                guiGraphics,
                targetCurse,
                startX,
                startY
        );
    }

    /**
     * 渲染一条诅咒骷髅
     */
    private static void renderCurseBar(
            GuiGraphics guiGraphics,
            int curse,
            int startX,
            int startY
    ) {

        ItemStack skull =
                new ItemStack(
                        Items.SKELETON_SKULL
                );

        for (int i = 0; i < MAX_CURSE; i++) {

            int x =
                    startX
                            + i * SLOT_SPACING;

            int y =
                    startY;

            boolean empty =
                    i >= curse;

            /*
             * 空骷髅变暗
             */
            if (empty) {

                guiGraphics.setColor(
                        EMPTY_SLOT_BRIGHTNESS,
                        EMPTY_SLOT_BRIGHTNESS,
                        EMPTY_SLOT_BRIGHTNESS,
                        1.0F
                );
            }

            guiGraphics.renderItem(
                    skull,
                    x,
                    y
            );

            /*
             * 恢复颜色
             */
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