package com.qidate.qisplan2.block;

import com.qidate.qisplan2.structure.GhostManorGenerationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class GhostManorMarkerBlock extends Block {

    public GhostManorMarkerBlock(
            Properties properties
    ) {
        super(properties);
    }

    @Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {


        super.onPlace(
                state,
                level,
                pos,
                oldState,
                movedByPiston
        );

        /*
         * 只处理真正的服务器世界。
         */
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * 防止同一种方块重新放置时重复触发。
         */
        if (oldState.is(this)) {
            return;
        }

        /*
         * ========================================
         * 不要在结构生成过程中立即启动庄园生成。
         *
         * 延迟到下一次服务器任务执行，
         * 避免在 Chunk 生成流程里面再次请求 Chunk。
         * ========================================
         */
        serverLevel.getServer().execute(() -> {

            /*
             * Marker 可能已经不存在了。
             */
            if (!serverLevel
                    .getBlockState(pos)
                    .is(this)) {
                return;
            }

            /*
             * 启动鬼庄园大型结构生成。
             */
            boolean started =
                    GhostManorGenerationManager.start(
                            serverLevel,
                            pos
                    );

            /*
             * Marker 自己删除。
             */
            serverLevel.removeBlock(
                    pos,
                    false
            );
        });
    }
}