
package com.qidate.qisplan2.ghost.module.sound;

import com.qidate.qisplan2.core.ModBlocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 鬼声音源的遮挡规则。
 */
public final class GhostSoundBlockers {

    private GhostSoundBlockers() {
    }

    /**
     * 鬼石砖和关闭的鬼门会阻挡鬼音乐。
     */
    public static boolean isGhostPianoBlocker(BlockState state) {
        if (state.is(ModBlocks.GHOST_STONE_BRICKS.get())) {
            return true;
        }

        if (state.is(ModBlocks.GHOST_DOOR.get())) {
            return !state.getValue(DoorBlock.OPEN);
        }

        return false;
    }
}