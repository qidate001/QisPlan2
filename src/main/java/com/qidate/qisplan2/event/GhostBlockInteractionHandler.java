package com.qidate.qisplan2.event;

import com.qidate.qisplan2.ghost.module.GhostModuleHost;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.BlockDestructionAttemptEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 方块破坏事件
 */
public class GhostBlockInteractionHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        BlockState state = event.getState();

        // 直接从方块实例获取鬼模块宿主，不再判断具体方块类型。
        if (!(state.getBlock() instanceof GhostModuleHost host)) {
            return;
        }

        BlockDestructionAttemptEvent destructionEvent =
                new BlockDestructionAttemptEvent(
                        player,
                        event.getPos(),
                        state
                );

        GhostModuleRuntime.dispatch(host, destructionEvent);

        if (destructionEvent.isCanceled()) {
            event.setCanceled(true);
        }
    }
}