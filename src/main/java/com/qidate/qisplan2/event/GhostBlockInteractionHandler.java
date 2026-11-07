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
        if (event.isCanceled()) return;
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        BlockState state = event.getState();

        // 优先使用方块实体持有的可变模块实例；无方块实体宿主时使用方块定义。
        var blockEntity = event.getLevel().getBlockEntity(event.getPos());
        GhostModuleHost host = blockEntity instanceof GhostModuleHost modules ? modules
                : state.getBlock() instanceof GhostModuleHost modules ? modules : null;
        if (host == null) return;

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