
package com.qidate.qisplan2.ghost.module.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * 灵异语义事件：方块摧毁尝试。
 */
public final class BlockDestructionAttemptEvent
        extends DestructionAttemptEvent {

    private final Player player;
    private final BlockPos pos;
    private final BlockState state;

    public BlockDestructionAttemptEvent(
            Player player,
            BlockPos pos,
            BlockState state
    ) {
        this.player = Objects.requireNonNull(player, "player");
        this.pos = Objects.requireNonNull(pos, "pos").immutable();
        this.state = Objects.requireNonNull(state, "state");
    }

    /**
     * 发起方块摧毁尝试的玩家。
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * 被尝试摧毁的方块坐标。
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * 被尝试摧毁时的方块状态。
     */
    public BlockState getState() {
        return state;
    }
}
