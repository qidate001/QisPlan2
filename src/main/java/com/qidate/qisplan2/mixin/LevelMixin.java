package com.qidate.qisplan2.mixin;

import com.qidate.qisplan2.ghost.isolation.GhostIsolationBlockRegistry;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Level Mixin。
 *
 * <p>
 * 监听世界中的实际方块状态变化，
 * 用于通知灵异隔绝系统使相关缓存失效。
 * </p>
 */
@Mixin(Level.class)
public abstract class LevelMixin {

    /**
     * 监听方块状态变更通知。
     *
     * <p>
     * Level#setBlock() 在成功修改方块状态后，
     * 会进入 markAndNotifyBlock()。
     *
     * <p>
     * 因此这里可以作为统一的方块状态变化入口。
     */
    @Inject(
            method = "markAndNotifyBlock",
            at = @At("TAIL")
    )
    private void qisplan2$onBlockStateChanged(
            BlockPos pos,
            LevelChunk levelChunk,
            BlockState oldState,
            BlockState newState,
            int flags,
            int recursionLeft,
            CallbackInfo ci
    ) {

        /*
         * 客户端不参与灵异隔绝缓存。
         */
        Level level =
                (Level) (Object) this;

        if (level.isClientSide()) {
            return;
        }

        /*
         * 只有旧状态或新状态属于
         * 灵异隔绝方块时，
         * 才可能影响灵异隔绝。
         */
        boolean oldBlocks =
                GhostIsolationBlockRegistry
                        .canBlockSupernatural(
                                oldState
                        );

        boolean newBlocks =
                GhostIsolationBlockRegistry
                        .canBlockSupernatural(
                                newState
                        );

        /*
         * 普通方块之间的变化，
         * 与灵异隔绝完全无关。
         */
        if (!oldBlocks && !newBlocks) {
            return;
        }

        /*
         * ServerLevel 才应该进入系统。
         */
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {

            GhostIsolationSystem.onBlockChanged(
                    serverLevel,
                    pos
            );
        }
    }
}