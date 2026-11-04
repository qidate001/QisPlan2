package com.qidate.qisplan2.event;

import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.possession.ability.ghostdoor.GhostDoorAbilityHandler;
import com.qidate.qisplan2.ghost.module.GhostModuleRuntime;
import com.qidate.qisplan2.ghost.module.event.DoorChangedEvent;
import com.qidate.qisplan2.ghost.module.implementation.ClosingGhostModule;
import com.qidate.qisplan2.ghost.module.implementation.OpeningGhostModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class DoorGhostTriggerHandler {

    private DoorGhostTriggerHandler() {
    }

    /**
     * 门鬼的触发范围。
     *
     * 100 格球形范围。
     */
    private static final double TRIGGER_RADIUS =
            100.0D;

    /**
     * 处理一次门的开关事件。
     *
     * @param level      服务端世界
     * @param pos        被操作的门方块位置
     * @param state      被操作位置的门状态
     * @param source     开门/关门的生物
     * @param opening    true = 开门，false = 关门
     */
    public static void onDoorChanged(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            LivingEntity source,
            boolean opening
    ) {

        if (source == null
                || !source.isAlive()) {
            return;
        }

        /*
         * ========================================================
         * 找到门的下半部分。
         *
         * 玩家有可能点击上半部分。
         * AI / setOpen 也可能传入另一部分的位置。
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

        GhostDoorAbilityHandler.onDoorChanged(
                level,
                doorPos,
                source,
                opening
        );

        /*
         * 重新读取门的真正下半部分状态。
         */
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
         * 100 格球形范围
         * ========================================================
         */
        double centerX =
                doorPos.getX() + 0.5D;

        double centerY =
                doorPos.getY() + 0.5D;

        double centerZ =
                doorPos.getZ() + 0.5D;

        AABB searchBox =
                new AABB(
                        centerX,
                        centerY,
                        centerZ,
                        centerX,
                        centerY,
                        centerZ
                ).inflate(
                        TRIGGER_RADIUS
                );

        double maxDistanceSqr =
                TRIGGER_RADIUS
                        * TRIGGER_RADIUS;

        List<ModularGhostEntity> ghosts = level.getEntitiesOfClass(
                ModularGhostEntity.class,
                searchBox,
                ghost -> ghost.isAlive() && !ghost.isRemoved()
                        && (ghost.hasGhostModule(OpeningGhostModule.ID)
                        || ghost.hasGhostModule(ClosingGhostModule.ID))
                        && ghost.distanceToSqr(centerX, centerY, centerZ) <= maxDistanceSqr
        );
        GhostModuleRuntime.dispatch(
                ghosts,
                new DoorChangedEvent(source, doorPos, doorState, opening)
        );
    }

    /**
     * 获取门的下半部分。
     */
    private static BlockPos getLowerDoorPos(
            ServerLevel level,
            BlockPos pos,
            BlockState state
    ) {

        /*
         * 必须是 DoorBlock。
         */
        if (!(state.getBlock()
                instanceof DoorBlock)) {
            return null;
        }

        /*
         * DoorBlock 正常都有 HALF。
         */
        if (!state.hasProperty(
                DoorBlock.HALF
        )) {
            return null;
        }

        /*
         * 如果点击的是上半部分，
         * 转到下面。
         */
        if (state.getValue(
                DoorBlock.HALF
        ) == DoubleBlockHalf.UPPER) {

            BlockPos lower =
                    pos.below();

            BlockState lowerState =
                    level.getBlockState(
                            lower
                    );

            if (lowerState.getBlock()
                    instanceof DoorBlock) {

                return lower;
            }

            return null;
        }

        /*
         * 已经是下半部分。
         */
        return pos;
    }
}
