package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostStoneBricksHandler {

    @SubscribeEvent
    public static void onBlockBreak(
            BlockEvent.BreakEvent event
    ) {

        Player player = event.getPlayer();

        // 只处理玩家
        if (player == null) {
            return;
        }

        BlockState state = event.getState();

        // 只处理鬼石砖
        if (!isGhostStoneBricks(state)) {
            return;
        }

        /*
         * 玩家尝试破坏鬼石砖
         *
         * 成功杀死玩家
         * ↓
         * 取消这次方块破坏
         * ↓
         * 鬼石砖保持原样
         */

        boolean killed =
                SupernaturalDeathHandler.tryKill(
                        player,
                        ModDamageTypes.ghostStoneBricks(player),
                        4.0
                );

        if (killed) {

            // 阻止 Minecraft 继续执行破坏方块
            event.setCanceled(true);
        }
    }


    private static boolean isGhostStoneBricks(
            BlockState state
    ) {

        return state.getBlock()
                == ModBlocks.GHOST_STONE_BRICKS.get();
    }
}