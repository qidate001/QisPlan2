package com.qidate.qisplan2.ghost.ability.knockingghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.block.GhostDoorBlock;
import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

import java.util.*;

/**
 * 敲门鬼公共规则系统。
 *
 * 本体 KnockingGhost 与驾驭后的 KnockingGhostAbility
 * 都通过这里执行“敲门”。
 */
public final class KnockingGhostDoorSystem {

    /*
     * ============================================================
     * 基础规则
     * ============================================================
     */

    /**
     * 听到敲门声的半径。
     */
    public static final double KNOCK_HEARING_RADIUS = 16.0D;

    /**
     * 敲门灵异攻击强度。
     */
    public static final double KNOCK_ATTACK_STRENGTH = 5.0D;

    /**
     * 敲门声与灵异攻击之间的延迟。
     *
     * 10 tick = 0.5 秒。
     */
    private static final int KNOCK_ATTACK_DELAY = 10;


    /*
     * ============================================================
     * 鬼门共鸣
     * ============================================================
     */

    /**
     * 鬼门共鸣半径。
     */
    public static final int GHOST_DOOR_ECHO_RADIUS = 50;

    /**
     * 鬼门共鸣垂直范围。
     */
    public static final int GHOST_DOOR_ECHO_VERTICAL_RADIUS = 20;

    /**
     * 共鸣最短延迟。
     */
    private static final int GHOST_DOOR_ECHO_MIN_DELAY = 5;

    /**
     * 共鸣最长延迟。
     */
    private static final int GHOST_DOOR_ECHO_MAX_DELAY = 40;

    /**
     * 最大递归层数。
     */
    public static final int MAX_GHOST_DOOR_ECHO_DEPTH = 50;


    /*
     * ============================================================
     * 最近敲门记忆
     * ============================================================
     */

    private static final int RECENT_DOOR_MEMORY_TIME =
            20 * 30;


    /*
     * ============================================================
     * 延迟攻击
     * ============================================================
     */

    private static final List<PendingKnockAttack>
            PENDING_ATTACKS =
            new ArrayList<>();

    private record PendingKnockAttack(
            ServerLevel level,
            LivingEntity source,
            UUID target,
            int remainingTicks
    ) {
    }


    /*
     * ============================================================
     * 延迟鬼门共鸣
     * ============================================================
     */

    private static final List<PendingDoorKnock>
            PENDING_DOOR_KNOCKS =
            new ArrayList<>();

    private record PendingDoorKnock(
            ServerLevel level,
            LivingEntity source,
            BlockPos doorPos,
            int echoDepth,
            int remainingTicks,
            Set<BlockPos> visitedDoors
    ) {
    }


    /*
     * ============================================================
     * 最近敲过的门
     *
     * 每个敲门来源都有自己独立的一份。
     * ============================================================
     */

    private static final Map<
            UUID,
            Map<BlockPos, Integer>
            > RECENTLY_KNOCKED =
            new HashMap<>();


    private KnockingGhostDoorSystem() {
    }


    /*
     * ============================================================
     * 公共入口
     * ============================================================
     */

    /**
     * 执行一次完整敲门。
     *
     * @param level     世界
     * @param source    敲门者
     * @param doorPos   门的下半部分
     */
    public static KnockResult knock(
            ServerLevel level,
            LivingEntity source,
            BlockPos doorPos
    ) {

        Set<BlockPos> visitedDoors =
                new HashSet<>();

        return knock(
                level,
                source,
                doorPos,
                0,
                visitedDoors
        );
    }


    /**
     * 实际执行敲门。
     */
    private static KnockResult knock(
            ServerLevel level,
            LivingEntity source,
            BlockPos doorPos,
            int echoDepth,
            Set<BlockPos> visitedDoors
    ) {

        doorPos =
                doorPos.immutable();


        /*
         * ========================================================
         * 门不存在
         * ========================================================
         */

        if (!isDoor(
                level,
                doorPos
        )) {

            return KnockResult.failed();
        }


        /*
         * ========================================================
         * 本轮递归去重
         * ========================================================
         */

        if (!visitedDoors.add(
                doorPos
        )) {

            return KnockResult.failed();
        }


        /*
         * ========================================================
         * 最近门记忆
         * ========================================================
         */

        markRecentlyKnocked(
                source,
                doorPos
        );


        /*
         * ========================================================
         * 播放声音
         * ========================================================
         */

        level.playSound(
                null,
                doorPos,
                QisPlan2.GHOST_KNOCK.get(),
                SoundSource.HOSTILE,
                1.0F,
                1.0F
        );


        /*
         * ========================================================
         * 查找听见敲门声的生物
         * ========================================================
         */

        int hearingTargetCount =
                queueKnockAttacks(
                        level,
                        source,
                        doorPos
                );


        /*
         * ========================================================
         * 普通门
         *
         * 到这里结束。
         * ========================================================
         */

        if (!isGhostDoor(
                level,
                doorPos
        )) {

            return new KnockResult(
                    true,
                    hearingTargetCount,
                    false
            );
        }


        /*
         * ========================================================
         * 50 层上限
         * ========================================================
         */

        if (echoDepth
                >= MAX_GHOST_DOOR_ECHO_DEPTH) {

            return new KnockResult(
                    true,
                    hearingTargetCount,
                    true
            );
        }


        /*
         * ========================================================
         * 鬼门继续共鸣
         * ========================================================
         */

        scheduleGhostDoorEchoes(
                level,
                source,
                doorPos,
                echoDepth + 1,
                visitedDoors
        );


        return new KnockResult(
                true,
                hearingTargetCount,
                true
        );
    }


    /*
     * ============================================================
     * 敲门结果
     * ============================================================
     */

    public record KnockResult(
            boolean success,
            int hearingTargetCount,
            boolean ghostDoor
    ) {

        private static KnockResult failed() {

            return new KnockResult(
                    false,
                    0,
                    false
            );
        }
    }


    /*
     * ============================================================
     * 查找听者
     * ============================================================
     */

    private static int queueKnockAttacks(
            ServerLevel level,
            LivingEntity source,
            BlockPos doorPos
    ) {

        AABB hearingBox =
                new AABB(
                        doorPos
                ).inflate(
                        KNOCK_HEARING_RADIUS
                );


        int count = 0;


        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        hearingBox,
                        LivingEntity::isAlive
                )) {

            /*
             * 敲门者自己不受攻击。
             */
            if (entity == source) {
                continue;
            }


            /*
             * ====================================================
             * 真正球形距离
             * ====================================================
             */

            double dx =
                    entity.getX()
                            - (doorPos.getX() + 0.5D);

            double dy =
                    entity.getY()
                            - (doorPos.getY() + 0.5D);

            double dz =
                    entity.getZ()
                            - (doorPos.getZ() + 0.5D);


            if (dx * dx
                    + dy * dy
                    + dz * dz
                    > KNOCK_HEARING_RADIUS
                    * KNOCK_HEARING_RADIUS) {

                continue;
            }


            count++;


            /*
             * ====================================================
             * 延迟攻击
             * ====================================================
             */

            PENDING_ATTACKS.add(
                    new PendingKnockAttack(
                            level,
                            source,
                            entity.getUUID(),
                            KNOCK_ATTACK_DELAY
                    )
            );
        }


        return count;
    }


    /*
     * ============================================================
     * 鬼门共鸣
     * ============================================================
     */

    private static void scheduleGhostDoorEchoes(
            ServerLevel level,
            LivingEntity source,
            BlockPos sourceDoor,
            int echoDepth,
            Set<BlockPos> visitedDoors
    ) {

        int radius =
                GHOST_DOOR_ECHO_RADIUS;

        int vertical =
                GHOST_DOOR_ECHO_VERTICAL_RADIUS;


        Set<BlockPos> nearbyDoors =
                new HashSet<>();


        BlockPos.MutableBlockPos mutable =
                new BlockPos.MutableBlockPos();


        /*
         * ========================================================
         * 扫描 50 格
         * ========================================================
         */

        for (int x = -radius;
             x <= radius;
             x++) {

            for (int y = -vertical;
                 y <= vertical;
                 y++) {

                for (int z = -radius;
                     z <= radius;
                     z++) {

                    /*
                     * 圆形范围。
                     */
                    if (x * x + z * z
                            > radius * radius) {

                        continue;
                    }


                    mutable.set(
                            sourceDoor.getX() + x,
                            sourceDoor.getY() + y,
                            sourceDoor.getZ() + z
                    );


                    /*
                     * 不强制加载新区块。
                     */
                    if (!level.hasChunkAt(
                            mutable
                    )) {

                        continue;
                    }


                    BlockPos candidate =
                            mutable.immutable();


                    /*
                     * 自己跳过。
                     */
                    if (candidate.equals(
                            sourceDoor
                    )) {

                        continue;
                    }


                    /*
                     * 本轮已经访问过。
                     */
                    if (visitedDoors.contains(
                            candidate
                    )) {

                        continue;
                    }


                    /*
                     * 必须是真门。
                     */
                    if (!isDoor(
                            level,
                            candidate
                    )) {

                        continue;
                    }


                    nearbyDoors.add(
                            candidate
                    );
                }
            }
        }


        /*
         * ========================================================
         * 加入延迟队列
         *
         * 注意：
         *
         * 这里不要提前 visitedDoors.add()
         *
         * 必须等它真正响起的时候再标记。
         * ========================================================
         */

        for (BlockPos door :
                nearbyDoors) {

            double dx =
                    door.getX()
                            - sourceDoor.getX();

            double dz =
                    door.getZ()
                            - sourceDoor.getZ();


            double distance =
                    Math.sqrt(
                            dx * dx
                                    + dz * dz
                    );


            int delay =
                    GHOST_DOOR_ECHO_MIN_DELAY
                            + (int) Math.min(
                            GHOST_DOOR_ECHO_MAX_DELAY
                                    - GHOST_DOOR_ECHO_MIN_DELAY,
                            distance / 8.0D
                    );


            PENDING_DOOR_KNOCKS.add(
                    new PendingDoorKnock(
                            level,
                            source,
                            door,
                            echoDepth,
                            delay,
                            visitedDoors
                    )
            );
        }
    }


    /*
     * ============================================================
     * 每服务器 Tick 调用
     * ============================================================
     */

    public static void tick() {

        tickPendingAttacks();

        tickPendingDoorKnocks();

        tickRecentDoors();
    }


    /*
     * ============================================================
     * 延迟攻击
     * ============================================================
     */

    private static void tickPendingAttacks() {

        for (int i =
             PENDING_ATTACKS.size() - 1;
             i >= 0;
             i--) {

            PendingKnockAttack pending =
                    PENDING_ATTACKS.get(i);


            int remaining =
                    pending.remainingTicks()
                            - 1;


            if (remaining > 0) {

                PENDING_ATTACKS.set(
                        i,
                        new PendingKnockAttack(
                                pending.level(),
                                pending.source(),
                                pending.target(),
                                remaining
                        )
                );

                continue;
            }


            ServerLevel level =
                    pending.level();


            Entity entity =
                    level.getEntity(
                            pending.target()
                    );


            /*
             * 目标还在。
             */
            if (entity instanceof LivingEntity living
                    && living.isAlive()) {

                /*
                 * 敲门者还存在：
                 *
                 * 才能构造真正的敲门伤害来源。
                 */
                if (pending.source()
                        .isAlive()) {

                    SupernaturalDeathHandler.tryKill(
                            living,
                            ModDamageTypes.knockingGhost(
                                    pending.source()
                            ),
                            KNOCK_ATTACK_STRENGTH
                    );
                }
            }


            PENDING_ATTACKS.remove(i);
        }
    }


    /*
     * ============================================================
     * 延迟鬼门
     * ============================================================
     */

    private static void tickPendingDoorKnocks() {

        for (int i =
             PENDING_DOOR_KNOCKS.size() - 1;
             i >= 0;
             i--) {

            PendingDoorKnock pending =
                    PENDING_DOOR_KNOCKS.get(i);


            int remaining =
                    pending.remainingTicks()
                            - 1;


            if (remaining > 0) {

                PENDING_DOOR_KNOCKS.set(
                        i,
                        new PendingDoorKnock(
                                pending.level(),
                                pending.source(),
                                pending.doorPos(),
                                pending.echoDepth(),
                                remaining,
                                pending.visitedDoors()
                        )
                );

                continue;
            }


            ServerLevel level =
                    pending.level();


            BlockPos doorPos =
                    pending.doorPos();


            /*
             * 门还存在才响。
             */
            if (isDoor(
                    level,
                    doorPos
            )) {

                knock(
                        level,
                        pending.source(),
                        doorPos,
                        pending.echoDepth(),
                        pending.visitedDoors()
                );
            }


            PENDING_DOOR_KNOCKS.remove(i);
        }
    }


    /*
     * ============================================================
     * 最近门清理
     * ============================================================
     */

    private static void tickRecentDoors() {

        for (Iterator<
                Map.Entry<
                        UUID,
                        Map<BlockPos, Integer>
                        >
                > iterator =
             RECENTLY_KNOCKED.entrySet().iterator();
             iterator.hasNext();) {

            Map.Entry<
                    UUID,
                    Map<BlockPos, Integer>
                    > entry =
                    iterator.next();


            Map<BlockPos, Integer> doors =
                    entry.getValue();


            for (Iterator<
                    Map.Entry<BlockPos, Integer>
                    > doorIterator =
                 doors.entrySet().iterator();
                 doorIterator.hasNext();) {

                Map.Entry<BlockPos, Integer>
                        doorEntry =
                        doorIterator.next();


                int remaining =
                        doorEntry.getValue() - 1;


                if (remaining <= 0) {

                    doorIterator.remove();

                } else {

                    doorEntry.setValue(
                            remaining
                    );
                }
            }


            if (doors.isEmpty()) {

                iterator.remove();
            }
        }
    }


    /*
     * ============================================================
     * 最近门
     * ============================================================
     */

    public static boolean wasRecentlyKnocked(
            LivingEntity source,
            BlockPos pos
    ) {

        Map<BlockPos, Integer> doors =
                RECENTLY_KNOCKED.get(
                        source.getUUID()
                );

        return doors != null
                && doors.containsKey(
                pos
        );
    }


    private static void markRecentlyKnocked(
            LivingEntity source,
            BlockPos pos
    ) {

        RECENTLY_KNOCKED
                .computeIfAbsent(
                        source.getUUID(),
                        ignored ->
                                new HashMap<>()
                )
                .put(
                        pos.immutable(),
                        RECENT_DOOR_MEMORY_TIME
                );
    }


    /*
     * ============================================================
     * 门判断
     * ============================================================
     */

    public static boolean isDoor(
            Level level,
            BlockPos pos
    ) {

        BlockState state =
                level.getBlockState(pos);


        /*
         * 原版 / 标准 DoorBlock
         *
         * 只认下半部分。
         */
        if (state.getBlock()
                instanceof DoorBlock) {

            return state.getValue(
                    DoorBlock.HALF
            ) == DoubleBlockHalf.LOWER;
        }


        /*
         * 其他模组：
         *
         * 加入 DOORS 标签。
         */
        if (state.is(BlockTags.DOORS)) {

            if (state.hasProperty(
                    BlockStateProperties.DOUBLE_BLOCK_HALF
            )) {

                return state.getValue(
                        BlockStateProperties.DOUBLE_BLOCK_HALF
                ) == DoubleBlockHalf.LOWER;
            }

            /*
             * 单方块门。
             */
            return true;
        }


        return false;
    }

    public static int countHearingTargets(
            ServerLevel level,
            LivingEntity source,
            BlockPos doorPos
    ) {

        AABB hearingBox =
                new AABB(
                        doorPos
                ).inflate(
                        KNOCK_HEARING_RADIUS
                );

        int count = 0;

        for (LivingEntity entity :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        hearingBox,
                        LivingEntity::isAlive
                )) {

            /*
             * 敲门者自己不算。
             */
            if (entity == source) {
                continue;
            }

            double dx =
                    entity.getX()
                            - (doorPos.getX() + 0.5D);

            double dy =
                    entity.getY()
                            - (doorPos.getY() + 0.5D);

            double dz =
                    entity.getZ()
                            - (doorPos.getZ() + 0.5D);

            if (dx * dx
                    + dy * dy
                    + dz * dz
                    > KNOCK_HEARING_RADIUS
                    * KNOCK_HEARING_RADIUS) {

                continue;
            }

            count++;
        }

        return count;
    }


    public static boolean isGhostDoor(
            Level level,
            BlockPos pos
    ) {

        return level.getBlockState(pos)
                .getBlock()
                instanceof GhostDoorBlock;
    }
}