package com.qidate.qisplan2.event;

import com.qidate.qisplan2.entity.AbstractDoorTriggerGhost;
import com.qidate.qisplan2.entity.ClosingGhost;
import com.qidate.qisplan2.entity.OpeningGhost;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

public final class DoorGhostTriggerHandler {

    private DoorGhostTriggerHandler() {
    }

    public static void onDoorChanged(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            LivingEntity source,
            boolean opening
    ) {

        /*
         * ========================================================
         * 找到门的下半部分
         * ========================================================
         */
        BlockPos doorPos =
                getLowerDoorPos(
                        level,
                        pos,
                        state
                );

        if (doorPos == null) {
            return;
        }

        BlockState doorState =
                level.getBlockState(
                        doorPos
                );

        if (!(doorState.getBlock()
                instanceof DoorBlock)) {

            return;
        }

        /*
         * ========================================================
         * 100 格触发范围
         * ========================================================
         */
        double radius =
                AbstractDoorTriggerGhost.TRIGGER_RADIUS;

        AABB area =
                new AABB(
                        doorPos
                ).inflate(
                        radius
                );

        if (opening) {

            /*
             * ====================================================
             * 开门鬼
             * ====================================================
             */
            for (OpeningGhost ghost :
                    level.getEntitiesOfClass(
                            OpeningGhost.class,
                            area,
                            entity ->
                                    entity.isAlive()
                                            && !entity.isRemoved()
                    )) {

                if (ghost.distanceToSqr(
                        doorPos.getX() + 0.5D,
                        doorPos.getY(),
                        doorPos.getZ() + 0.5D
                ) > radius * radius) {
                    continue;
                }

                ghost.triggerDoorEvent(
                        source,
                        doorPos,
                        doorState
                );
            }

        } else {

            /*
             * ====================================================
             * 关门鬼
             * ====================================================
             */
            for (ClosingGhost ghost :
                    level.getEntitiesOfClass(
                            ClosingGhost.class,
                            area,
                            entity ->
                                    entity.isAlive()
                                            && !entity.isRemoved()
                    )) {

                if (ghost.distanceToSqr(
                        doorPos.getX() + 0.5D,
                        doorPos.getY(),
                        doorPos.getZ() + 0.5D
                ) > radius * radius) {
                    continue;
                }

                ghost.triggerDoorEvent(
                        source,
                        doorPos,
                        doorState
                );
            }
        }
    }

    private static BlockPos getLowerDoorPos(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {

        if (!(state.getBlock()
                instanceof DoorBlock)) {

            return null;
        }

        if (!state.hasProperty(
                DoorBlock.HALF
        )) {
            return null;
        }

        if (state.getValue(
                DoorBlock.HALF
        ) == DoubleBlockHalf.UPPER) {

            return pos.below();
        }

        return pos;
    }
}