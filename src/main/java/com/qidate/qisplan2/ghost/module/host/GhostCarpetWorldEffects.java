package com.qidate.qisplan2.ghost.module.host;

import com.qidate.qisplan2.core.ModBlocks;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 地毯能力的 Minecraft 世界实现；不持有接触计时，不决定规则何时发动。 */
public final class GhostCarpetWorldEffects {
    /** 世界效果只通过静态入口执行。 */
    private GhostCarpetWorldEffects() {}

    /** 在袭击目标处播放原有灵魂粒子与音效，仅供已获准的能力调用。 */
    public static void feedback(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        level.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 0.5, target.getZ(),
                20, 0.4, 0.5, 0.4, 0.05);
        level.playSound(null, target.blockPosition(), SoundEvents.SOUL_ESCAPE.value(),
                SoundSource.BLOCKS, 1, 0.8F);
    }

    /** 击杀成功后寻找附近群落边缘，沿用原有随机蔓延 2 至 3 格的行为。 */
    public static void spread(
            LivingEntity entity
    ) {

        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos origin =
                entity.blockPosition();

        /*
         * ========================================
         * 找到死亡点附近的鬼地毯
         * ========================================
         */

        BlockPos carpetStart =
                findNearbyGhostCarpet(
                        serverLevel,
                        origin
                );

        if (carpetStart == null) {
            return;
        }


        /*
         * ========================================
         * 找到整个鬼地毯群落
         * ========================================
         */

        Set<BlockPos> carpetGroup =
                findGhostCarpetGroup(
                        serverLevel,
                        carpetStart
                );

        if (carpetGroup.isEmpty()) {
            return;
        }


        /*
         * ========================================
         * 找到群落边缘
         * ========================================
         */

        List<BlockPos> edges =
                findCarpetEdges(
                        serverLevel,
                        carpetGroup
                );

        if (edges.isEmpty()) {
            return;
        }


        /*
         * ========================================
         * 随机决定本次蔓延 2～3 格
         * ========================================
         */

        int spreadCount =
                2 + serverLevel.random.nextInt(2);


        /*
         * 随机选择一个边缘作为起点
         */

        BlockPos current =
                edges.get(
                        serverLevel.random.nextInt(
                                edges.size()
                        )
                );


        /*
         * ========================================
         * 开始随机感染
         * ========================================
         */

        for (int i = 0; i < spreadCount; i++) {

            BlockPos next =
                    findRandomSpreadPosition(
                            serverLevel,
                            current,
                            carpetGroup
                    );

            /*
             * 当前点附近完全没有可以蔓延的位置。
             */
            if (next == null) {
                return;
            }


            /*
             * 放置鬼地毯
             */
            serverLevel.setBlock(
                    next,
                    ModBlocks.GHOST_CARPET
                            .get()
                            .defaultBlockState(),
                    3
            );


            /*
             * 加入当前群落。
             */
            carpetGroup.add(next);


            /*
             * 下一次从刚刚感染的位置继续蔓延。
             */
            current = next;
        }
    }

    /**
     * 在死亡位置附近寻找一块鬼地毯。
     *
     * 这里不要求死亡位置本身就是鬼地毯，
     * 因为实体死亡后可能正好站在地毯边缘。
     */
    private static BlockPos findNearbyGhostCarpet(
            ServerLevel level,
            BlockPos origin
    ) {

        /*
         * 优先检查自己脚下以及周围 2 格。
         */

        for (int radius = 0; radius <= 2; radius++) {

            for (int dx = -radius; dx <= radius; dx++) {

                for (int dz = -radius; dz <= radius; dz++) {

                    BlockPos pos =
                            origin.offset(
                                    dx,
                                    0,
                                    dz
                            );

                    if (isGhostCarpet(
                            level,
                            pos
                    )) {
                        return pos;
                    }
                }
            }
        }

        return null;
    }

    /** 检查当前已查询位置是否属于鬼地毯群落。 */
    private static boolean isGhostCarpet(
            ServerLevel level,
            BlockPos pos
    ) {

        return level.getBlockState(pos)
                .is(ModBlocks.GHOST_CARPET.get());
    }

    /**
     * 找到与起点相连的整个鬼地毯群落。
     *
     * 只有上下左右相邻的鬼地毯才属于同一个群落。
     */
    private static Set<BlockPos> findGhostCarpetGroup(
            ServerLevel level,
            BlockPos start
    ) {

        Set<BlockPos> group =
                new HashSet<>();

        ArrayDeque<BlockPos> queue =
                new ArrayDeque<>();

        group.add(start);
        queue.add(start);


        while (!queue.isEmpty()) {

            BlockPos current =
                    queue.poll();


            /*
             * 鬼地毯只按照水平四方向连接。
             */
            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {

                BlockPos next =
                        current.relative(direction);


                if (group.contains(next)) {
                    continue;
                }


                if (!isGhostCarpet(
                        level,
                        next
                )) {
                    continue;
                }


                group.add(next);
                queue.add(next);
            }
        }

        return group;
    }

    /**
     * 找出鬼地毯群落所有可以向外扩散的边缘。
     */
    private static List<BlockPos> findCarpetEdges(
            ServerLevel level,
            Set<BlockPos> group
    ) {

        List<BlockPos> edges =
                new ArrayList<>();


        for (BlockPos carpet : group) {

            for (Direction direction :
                    Direction.Plane.HORIZONTAL) {

                BlockPos target =
                        carpet.relative(direction);


                /*
                 * 已经是鬼地毯，不是边缘。
                 */
                if (group.contains(target)) {
                    continue;
                }


                /*
                 * 判断这个位置是否真的可以放地毯。
                 */
                if (canPlaceGhostCarpet(
                        level,
                        target
                )) {

                    edges.add(carpet);

                    break;
                }
            }
        }

        return edges;
    }

    /**
     * 从当前鬼地毯随机向四周寻找可以感染的位置。
     */
    private static BlockPos findRandomSpreadPosition(
            ServerLevel level,
            BlockPos current,
            Set<BlockPos> group
    ) {
        Direction[] directions = {
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST
        };

        // 随机交换数组中的元素
        for (int i = directions.length - 1; i > 0; i--) {
            int j = level.random.nextInt(i + 1);

            Direction temp = directions[i];
            directions[i] = directions[j];
            directions[j] = temp;
        }

        // 按随机顺序尝试
        for (Direction direction : directions) {

            BlockPos target = current.relative(direction);

            // 已经是群落的一部分
            if (group.contains(target)) {
                continue;
            }

            // 不能放置
            if (!canPlaceGhostCarpet(level, target)) {
                continue;
            }

            return target;
        }

        return null;
    }

    /**
     * 判断指定位置是否适合生成鬼地毯。
     */
    private static boolean canPlaceGhostCarpet(
            ServerLevel level,
            BlockPos pos
    ) {

        /*
         * 目标位置必须为空气。
         */
        if (!level.getBlockState(pos).isAir()) {
            return false;
        }


        /*
         * 下方必须存在可以承载地毯的方块。
         */
        BlockPos below =
                pos.below();

        BlockState belowState =
                level.getBlockState(below);


        return belowState.isFaceSturdy(
                level,
                below,
                Direction.UP
        );
    }

    /**
     * 尝试在指定位置生成一块鬼地毯。
     */
    private static void tryPlaceGhostCarpet(
            ServerLevel level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);

        /*
         * 目标位置必须是空气。
         */
        if (!state.isAir()) {
            return;
        }

        /*
         * 鬼地毯需要一个支撑面。
         */
        BlockPos below =
                pos.below();

        BlockState belowState =
                level.getBlockState(below);

        /*
         * 必须能在下面这个方块上放置地毯。
         */
        if (!belowState.isFaceSturdy(
                level,
                below,
                Direction.UP
        )) {
            return;
        }

        /*
         * 放置鬼地毯。
         */
        level.setBlock(
                pos,
                ModBlocks.GHOST_CARPET
                        .get()
                        .defaultBlockState(),
                3
        );
    }


}
