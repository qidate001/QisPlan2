package com.qidate.qisplan2.block.entity;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.ghost.isolation.GhostIsolationSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.qidate.qisplan2.core.ModGameRules.GHOST_ISOLATION_MARKER_ENABLED;

public class GhostIsolationMarkerBlockEntity
        extends BlockEntity {

    /**
     * 两次检测之间的等待时间。
     *
     * <p>
     * 5 秒 = 100 tick。
     */
    private static final int RETRY_INTERVAL_TICKS = 100;

    /**
     * 最大检测次数。
     */
    private static final int MAX_ATTEMPTS = 3;

    /**
     * 已经进行的检测次数。
     */
    private int attempts;

    /**
     * 下一次检测还需要等待的 tick。
     */
    private int retryTicks;

    public GhostIsolationMarkerBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        super(
                ModBlocks.GHOST_ISOLATION_MARKER_BLOCK_ENTITY.get(),
                pos,
                state
        );
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            GhostIsolationMarkerBlockEntity blockEntity
    ) {

        if (level.isClientSide()) {
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        /*
         * ========================================================
         * 游戏规则
         * ========================================================
         *
         * 关闭时：
         *
         *     Marker 完全停止工作。
         *
         * 这样可以将 Marker 保留在建筑中，
         * 用于结构保存、编辑以及后续再次启用检测。
         */
        if (!serverLevel.getGameRules().getBoolean(
                GHOST_ISOLATION_MARKER_ENABLED
        )) {
            return;
        }

        /*
         * ========================================================
         * 等待下一次检测
         * ========================================================
         */
        if (blockEntity.retryTicks > 0) {

            blockEntity.retryTicks--;

            return;
        }

        /*
         * ========================================================
         * 检测次数已经用尽
         * ========================================================
         *
         * 理论上第三次失败时会立即删除，
         * 这里作为额外保险。
         */
        if (blockEntity.attempts >= MAX_ATTEMPTS) {

            serverLevel.removeBlock(
                    pos,
                    false
            );

            return;
        }

        /*
         * ========================================================
         * 发起一次灵异隔绝检测
         * ========================================================
         */

        blockEntity.attempts++;

        blockEntity.setChanged();

        QisPlan2.LOGGER.info(
                "[灵异隔绝检测] Marker 开始第 {}/{} 次检测：{}",
                blockEntity.attempts,
                MAX_ATTEMPTS,
                pos
        );

        boolean isolated =
                GhostIsolationSystem.isIsolated(
                        serverLevel,
                        pos
                );

        /*
         * ========================================================
         * 检测成功
         * ========================================================
         *
         * GhostIsolationSystem 已经负责建立 Region。
         *
         * Marker 的任务完成，立即自毁。
         */
        if (isolated) {

            QisPlan2.LOGGER.info(
                    "[灵异隔绝检测] Marker 第 {}/{} 次检测成功，建立灵异隔绝区域：{}",
                    blockEntity.attempts,
                    MAX_ATTEMPTS,
                    pos
            );

            serverLevel.removeBlock(
                    pos,
                    false
            );

            return;
        }

        /*
         * ========================================================
         * 检测失败
         * ========================================================
         */

        if (blockEntity.attempts >= MAX_ATTEMPTS) {

            QisPlan2.LOGGER.info(
                    "[灵异隔绝检测] Marker 连续 {} 次检测失败，放弃并自毁：{}",
                    MAX_ATTEMPTS,
                    pos
            );

            serverLevel.removeBlock(
                    pos,
                    false
            );

            return;
        }

        /*
         * 还有机会。
         *
         * 等待 5 秒后再次检测。
         */
        blockEntity.retryTicks =
                RETRY_INTERVAL_TICKS;

        blockEntity.setChanged();

        QisPlan2.LOGGER.info(
                "[灵异隔绝检测] Marker 第 {}/{} 次检测失败，{} 秒后再次检测：{}",
                blockEntity.attempts,
                MAX_ATTEMPTS,
                RETRY_INTERVAL_TICKS / 20,
                pos
        );
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries
    ) {
        super.saveAdditional(
                tag,
                registries
        );

        tag.putInt(
                "Attempts",
                attempts
        );

        tag.putInt(
                "RetryTicks",
                retryTicks
        );
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            net.minecraft.core.HolderLookup.Provider registries
    ) {
        super.loadAdditional(
                tag,
                registries
        );

        attempts =
                tag.getInt(
                        "Attempts"
                );

        retryTicks =
                tag.getInt(
                        "RetryTicks"
                );
    }
}