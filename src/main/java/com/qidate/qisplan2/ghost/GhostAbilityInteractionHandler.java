package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.ability.knockingghost.KnockingGhostAbility;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

public final class GhostAbilityInteractionHandler {

    private GhostAbilityInteractionHandler() {
    }

    /**
     * 注册事件。
     */
    public static void register() {

        NeoForge.EVENT_BUS.register(
                GhostAbilityInteractionHandler.class
        );
    }

    @SubscribeEvent
    public static void onRightClickBlock(
            RightClickBlock event
    ) {

        /*
         * ========================================================
         * 只处理服务器。
         * ========================================================
         */
        if (!(event.getEntity()
                instanceof ServerPlayer player)) {

            return;
        }

        /*
         * ========================================================
         * 必须 Shift。
         * ========================================================
         */
        if (!player.isShiftKeyDown()) {
            return;
        }

        /*
         * ========================================================
         * 必须空手。
         *
         * 这样不会抢普通物品的右键逻辑。
         * ========================================================
         */
        ItemStack itemStack =
                event.getItemStack();

        if (!itemStack.isEmpty()) {
            return;
        }

        /*
         * ========================================================
         * 当前驾驭的是不是敲门鬼？
         * ========================================================
         */
        if (!PossessionHandler.hasGhost(
                player,
                KnockingGhostAbility.ID
        )) {
            return;
        }

        /*
         * ========================================================
         * 尝试使用敲门鬼能力。
         * ========================================================
         */
        boolean success =
                PossessionHandler.useAbilityOnBlock(
                        player,
                        KnockingGhostAbility.ID,
                        event.getPos()
                );

        if (!success) {
            return;
        }

        /*
         * ========================================================
         * 成功以后阻止普通门交互。
         *
         * 防止玩家一边“敲门”，
         * 一边又正常打开门。
         * ========================================================
         */
        event.setCanceled(
                true
        );
    }
}