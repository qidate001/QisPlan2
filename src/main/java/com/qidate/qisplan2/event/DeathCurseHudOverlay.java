package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModAttachments;
import com.qidate.qisplan2.item.DeathCurseSword;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
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
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_SPACING = 12;

    // 空骷髅的亮度系数
    private static final float EMPTY_SLOT_BRIGHTNESS = 0.4F;

    private static final int HUD_HIDE_DELAY_TICKS = 100;
    private static long lastSwordHeldTime = -1;

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

        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.level == null) {
            return;
        }

        /*
         * ========================================
         * 判断是否拿着死亡诅咒之剑
         * ========================================
         */

        boolean holdingSword =
                player.getMainHandItem().getItem()
                        instanceof DeathCurseSword;

        long gameTime = minecraft.level.getGameTime();

        if (holdingSword) {

            lastSwordHeldTime = gameTime;

        } else {

            if (lastSwordHeldTime < 0) {
                return;
            }

            if (gameTime - lastSwordHeldTime >= HUD_HIDE_DELAY_TICKS) {
                return;
            }
        }


        /*
         * ========================================
         * 左上角：自己的诅咒
         * ========================================
         */

        int playerCurse =
                player.getData(
                        ModAttachments.DEATH_CURSE_COUNT.get()
                );

        if (playerCurse > 0) {

            playerCurse =
                    Math.min(
                            playerCurse,
                            MAX_CURSE
                    );

            renderPlayerCurseBar(
                    guiGraphics,
                    playerCurse,
                    8,
                    8,
                    player
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
                        ModAttachments.DEATH_CURSE_COUNT.get()
                );

        /*
         * 目标没有诅咒时不显示右侧
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

        renderTargetCurseBar(
                guiGraphics,
                targetCurse,
                startX,
                startY
        );
    }

    /**
     * 动态创建玩家头
     *
     * @param player 玩家
     * @return 玩家头
     */
    private static ItemStack createPlayerHead(Player player) {

        ItemStack head =
                new ItemStack(Items.PLAYER_HEAD);

        head.set(
                DataComponents.PROFILE,
                new ResolvableProfile(
                        player.getGameProfile()
                )
        );

        return head;
    }

    /**
     * 渲染自己的诅咒条
     *
     * 10 个位置全部使用玩家头颅。
     */
    private static void renderPlayerCurseBar(
            GuiGraphics guiGraphics,
            int curse,
            int startX,
            int startY,
            Player player
    ) {

        ItemStack head =
                createPlayerHead(player);

        for (int i = 0; i < MAX_CURSE; i++) {

            int x =
                    startX
                            + i * SLOT_SPACING;

            int y =
                    startY;

            boolean empty =
                    i >= curse;

            /*
             * 当前诅咒数量以外的头像变暗
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
                    head,
                    x,
                    y
            );

            /*
             * 恢复默认颜色
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

    /**
     * 渲染目标诅咒条
     *
     * 10 个位置全部使用骷髅头。
     */
    private static void renderTargetCurseBar(
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
             * 恢复默认颜色
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