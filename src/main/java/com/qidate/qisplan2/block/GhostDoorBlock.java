package com.qidate.qisplan2.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;

public class GhostDoorBlock extends DoorBlock {

    public GhostDoorBlock(
            BlockSetType blockSetType,
            Properties properties
    ) {
        super(blockSetType, properties);
    }

    /**
     * 鬼门只能从正面打开。
     */
    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {

        /*
         * =========================
         * 检查玩家是否位于正面
         * =========================
         */

        if (!isPlayerOnFront(state, pos, player)) {

            // 背面：完全没有反应
            return InteractionResult.PASS;
        }


        /*
         * =========================
         * 正面：使用原版门逻辑
         * =========================
         */

        return super.useWithoutItem(
                state,
                level,
                pos,
                player,
                hit
        );
    }


    /**
     * 判断玩家是否位于鬼门正面。
     */
    private static boolean isPlayerOnFront(
            BlockState state,
            BlockPos pos,
            Player player
    ) {

        /*
         * 门的朝向。
         */
        var facing =
                state.getValue(FACING);


        /*
         * 门中心。
         */
        double centerX =
                pos.getX() + 0.5;

        double centerZ =
                pos.getZ() + 0.5;


        /*
         * 玩家相对于门中心的位置。
         */
        double dx =
                player.getX() - centerX;

        double dz =
                player.getZ() - centerZ;


        /*
         * 点积判断正反面。
         */
        double dot =
                dx * facing.getStepX()
                        + dz * facing.getStepZ();


        return dot < 0;
    }
}