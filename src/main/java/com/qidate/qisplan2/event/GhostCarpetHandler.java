package com.qidate.qisplan2.event;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModBlocks;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = QisPlan2.MODID)
public class GhostCarpetHandler {
    /**
     * 每个实体已经累计踩了多少 tick。
     */
    private static final Map<UUID, Long> CARPET_TICKS =
            new HashMap<>();

    /**
     * 本次是否已经触发。
     */
    private static final Map<UUID, Boolean> TRIGGERED =
            new HashMap<>();

    /**
     * 灵异强度
     */
    private static final double SUPERNATURAL_ATTACK_STRENGTH = 2.0D;


    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {

        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }

        if (entity.level().isClientSide()) {
            return;
        }

        UUID uuid = entity.getUUID();

        /*
         * 死亡 → 清空数据
         */
        if (!entity.isAlive()) {
            reset(entity);
            return;
        }

        /*
         * 检查鬼地毯
         */
        BlockPos entityPos =
                entity.blockPosition();

        BlockState currentState =
                entity.level().getBlockState(entityPos);

        BlockState belowState =
                entity.level().getBlockState(entityPos.below());

        boolean onGhostCarpet =
                currentState.is(ModBlocks.GHOST_CARPET.get())
                        || belowState.is(ModBlocks.GHOST_CARPET.get());

        /*
         * 没踩鬼地毯 → 清空累计时间
         */
        if (!onGhostCarpet) {
            reset(entity);
            return;
        }

        /*
         * 正在踩鬼地毯 → 累计时间
         */
        long ticks =
                CARPET_TICKS.getOrDefault(uuid, 0L);

        ticks++;

        CARPET_TICKS.put(uuid, ticks);

        /*
         * 获取触发时间
         */
        int triggerTicks =
                entity.level()
                        .getGameRules()
                        .getInt(QisPlan2.GHOST_CARPET_KILL_TIME);

        /*
         * 达到触发时间
         */
        if (ticks >= triggerTicks) {

            applyGhostCarpetCurse(entity);

            /*
             * 攻击结束，重新开始下一轮 15 秒。
             */
            CARPET_TICKS.put(uuid, 0L);
        }
    }


    /**
     * 增加一层鬼地毯诅咒。
     */
    private static void applyGhostCarpetCurse(
            LivingEntity entity
    ) {
        /*
         * ========================================
         * 粒子
         * ========================================
         */

        if (entity.level() instanceof ServerLevel serverLevel) {

            serverLevel.sendParticles(
                    ParticleTypes.SOUL,
                    entity.getX(),
                    entity.getY() + 0.5,
                    entity.getZ(),
                    20,
                    0.4,
                    0.5,
                    0.4,
                    0.05
            );

            serverLevel.playSound(
                    null,
                    entity.blockPosition(),
                    SoundEvents.SOUL_ESCAPE.value(),
                    SoundSource.BLOCKS,
                    1.0F,
                    0.8F
            );
        }


        /*
         * ========================================
         * 请求灵异死亡
         * ========================================
         */

        boolean killed =
                SupernaturalDeathHandler.tryKill(
                        entity,
                        ModDamageTypes.ghostCarpet(entity),
                        SUPERNATURAL_ATTACK_STRENGTH
                );

        if (killed) {
            spreadGhostCarpet(entity);
        }
    }

    /*
     * 鬼地毯成功杀死生命后，尝试向外蔓延最多 2 格。
     *
     *
     * 鬼地毯成功杀死生命后进行感染式蔓延。
     *
     * 规则：
     * 1. 找到死亡点附近的鬼地毯群落。
     * 2. 找到群落的边缘。
     * 3. 随机选择一个边缘作为感染起点。
     * 4. 随机向四个方向尝试。
     * 5. 每成功生成一块，就从新生成的地毯继续蔓延。
     * 6. 一次随机成功蔓延 2～3 格。
     */
    private static void spreadGhostCarpet(
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

        java.util.Set<BlockPos> carpetGroup =
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

        java.util.List<BlockPos> edges =
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
    private static java.util.Set<BlockPos> findGhostCarpetGroup(
            ServerLevel level,
            BlockPos start
    ) {

        java.util.Set<BlockPos> group =
                new java.util.HashSet<>();

        java.util.ArrayDeque<BlockPos> queue =
                new java.util.ArrayDeque<>();

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
    private static java.util.List<BlockPos> findCarpetEdges(
            ServerLevel level,
            java.util.Set<BlockPos> group
    ) {

        java.util.List<BlockPos> edges =
                new java.util.ArrayList<>();


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
            java.util.Set<BlockPos> group
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
                net.minecraft.core.Direction.UP
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


    /**
     * 离开鬼地毯：
     *
     * 计时清零
     * 触发状态清零
     * 日志计时清零
     */
    private static void reset(
            LivingEntity entity
    ) {

        UUID uuid =
                entity.getUUID();

        CARPET_TICKS.remove(uuid);
        TRIGGERED.remove(uuid);
//        LAST_LOG_TIME.remove(uuid);
    }
}