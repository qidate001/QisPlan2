package com.qidate.qisplan2.ghost.ability.doorghost;

import com.qidate.qisplan2.network.DoorGhostMarkNetwork;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 开门鬼 / 关门鬼的驾驭标记管理器。
 *
 * 每个驾驭者：
 *
 *     目标实体
 *         ├── 开门鬼标记
 *         └── 关门鬼标记
 *
 * 两种标记可以同时存在。
 */
public final class DoorGhostMarkManager {

    /**
     * 标记持续时间：
     *
     * 10 秒。
     */
    public static final int MARK_DURATION =
            20 * 10;


    /**
     * 当前玩家的所有标记。
     *
     * 玩家 UUID
     *     ↓
     * 目标 UUID
     *     ↓
     * MarkState
     */
    private static final Map<
            UUID,
            Map<UUID, MarkState>
            > MARKS =
            new HashMap<>();


    /**
     * 一个目标当前的门鬼标记状态。
     */
    private static final class MarkState {

        private int openingTicks;

        private int closingTicks;

        private MarkState(
                int openingTicks,
                int closingTicks
        ) {
            this.openingTicks =
                    openingTicks;

            this.closingTicks =
                    closingTicks;
        }

        private boolean hasOpening() {
            return openingTicks > 0;
        }

        private boolean hasClosing() {
            return closingTicks > 0;
        }

        private boolean isEmpty() {
            return openingTicks <= 0
                    && closingTicks <= 0;
        }
    }


    private DoorGhostMarkManager() {
    }


    /*
     * ============================================================
     * 标记
     * ============================================================
     */

    /**
     * 添加开门鬼标记。
     */
    public static void markOpening(
            ServerPlayer viewer,
            UUID target
    ) {

        MarkState state =
                getOrCreate(
                        viewer,
                        target
                );

        state.openingTicks =
                MARK_DURATION;

        sendMarkState(
                viewer,
                target,
                state
        );
    }


    /**
     * 添加关门鬼标记。
     */
    public static void markClosing(
            ServerPlayer viewer,
            UUID target
    ) {

        MarkState state =
                getOrCreate(
                        viewer,
                        target
                );

        state.closingTicks =
                MARK_DURATION;

        sendMarkState(
                viewer,
                target,
                state
        );
    }


    private static MarkState getOrCreate(
            ServerPlayer viewer,
            UUID target
    ) {

        Map<UUID, MarkState> playerMarks =
                MARKS.computeIfAbsent(
                        viewer.getUUID(),
                        ignored ->
                                new HashMap<>()
                );

        return playerMarks.computeIfAbsent(
                target,
                ignored ->
                        new MarkState(
                                0,
                                0
                        )
        );
    }


    /*
     * ============================================================
     * 标记查询
     * ============================================================
     */

    /**
     * 是否存在开门鬼标记。
     */
    public static boolean hasOpeningMark(
            ServerPlayer viewer,
            UUID target
    ) {

        MarkState state =
                getState(
                        viewer,
                        target
                );

        return state != null
                && state.hasOpening();
    }


    /**
     * 是否存在关门鬼标记。
     */
    public static boolean hasClosingMark(
            ServerPlayer viewer,
            UUID target
    ) {

        MarkState state =
                getState(
                        viewer,
                        target
                );

        return state != null
                && state.hasClosing();
    }


    /**
     * 是否同时存在两种标记。
     */
    public static boolean hasBothMarks(
            ServerPlayer viewer,
            UUID target
    ) {

        MarkState state =
                getState(
                        viewer,
                        target
                );

        return state != null
                && state.hasOpening()
                && state.hasClosing();
    }


    private static MarkState getState(
            ServerPlayer viewer,
            UUID target
    ) {

        Map<UUID, MarkState> playerMarks =
                MARKS.get(
                        viewer.getUUID()
                );

        if (playerMarks == null) {
            return null;
        }

        return playerMarks.get(
                target
        );
    }


    /*
     * ============================================================
     * 寻找最近目标
     * ============================================================
     */

    public static LivingEntity findNearestMarkedTarget(
            ServerPlayer viewer,
            ServerLevel level
    ) {

        Map<UUID, MarkState> playerMarks =
                MARKS.get(
                        viewer.getUUID()
                );

        if (playerMarks == null
                || playerMarks.isEmpty()) {

            return null;
        }


        LivingEntity best =
                null;

        double bestDistance =
                Double.MAX_VALUE;


        Iterator<
                Map.Entry<
                        UUID,
                        MarkState
                        >
                > iterator =
                playerMarks.entrySet().iterator();


        while (iterator.hasNext()) {

            Map.Entry<
                    UUID,
                    MarkState
                    > entry =
                    iterator.next();

            UUID targetUUID =
                    entry.getKey();

            MarkState state =
                    entry.getValue();


            /*
             * 没有任何有效标记。
             */
            if (state.isEmpty()) {

                iterator.remove();

                continue;
            }


            Entity entity =
                    level.getEntity(
                            targetUUID
                    );


            /*
             * 实体不存在 / 已死亡。
             */
            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()) {

                iterator.remove();

                continue;
            }


            /*
             * 驾驭者自己不应该成为目标。
             */
            if (living == viewer) {

                iterator.remove();

                continue;
            }


            double distance =
                    viewer.distanceToSqr(
                            living
                    );


            if (distance
                    < bestDistance) {

                bestDistance =
                        distance;

                best =
                        living;
            }
        }


        if (playerMarks.isEmpty()) {

            MARKS.remove(
                    viewer.getUUID()
            );
        }


        return best;
    }

    /**
     * 清除当前玩家身上某个目标的全部门鬼标记。
     *
     * 一次主动袭击成功后调用。
     */
    public static void clearMarks(
            ServerPlayer viewer,
            UUID target
    ) {

        Map<UUID, MarkState> playerMarks =
                MARKS.get(
                        viewer.getUUID()
                );

        if (playerMarks == null) {
            return;
        }

        MarkState state =
                playerMarks.remove(
                        target
                );

        if (state == null) {
            return;
        }

        /*
         * ========================================================
         * 告诉当前玩家客户端：
         *
         * 这个实体不再显示门鬼轮廓。
         * ========================================================
         */
        DoorGhostMarkNetwork.sendMark(
                viewer,
                target,
                false
        );

        if (playerMarks.isEmpty()) {

            MARKS.remove(
                    viewer.getUUID()
            );
        }
    }


    /*
     * ============================================================
     * Tick
     * ============================================================
     */

    public static void tick(
            MinecraftServer server
    ) {

        Iterator<
                Map.Entry<
                        UUID,
                        Map<UUID, MarkState>
                        >
                > playerIterator =
                MARKS.entrySet().iterator();


        while (playerIterator.hasNext()) {

            Map.Entry<
                    UUID,
                    Map<UUID, MarkState>
                    > playerEntry =
                    playerIterator.next();


            UUID playerUUID =
                    playerEntry.getKey();


            Map<UUID, MarkState> playerMarks =
                    playerEntry.getValue();


            ServerPlayer viewer =
                    server.getPlayerList()
                            .getPlayer(
                                    playerUUID
                            );


            Iterator<
                    Map.Entry<
                            UUID,
                            MarkState
                            >
                    > markIterator =
                    playerMarks.entrySet()
                            .iterator();


            while (markIterator.hasNext()) {

                Map.Entry<
                        UUID,
                        MarkState
                        > entry =
                        markIterator.next();


                UUID targetUUID =
                        entry.getKey();


                MarkState state =
                        entry.getValue();


                /*
                 * 开门鬼标记倒计时。
                 */
                if (state.openingTicks > 0) {

                    state.openingTicks--;
                }


                /*
                 * 关门鬼标记倒计时。
                 */
                if (state.closingTicks > 0) {

                    state.closingTicks--;
                }


                /*
                 * 如果某一种标记刚刚结束，
                 * 更新客户端轮廓状态。
                 */
                if (viewer != null) {

                    sendMarkState(
                            viewer,
                            targetUUID,
                            state
                    );
                }


                /*
                 * 两种标记都结束。
                 */
                if (state.isEmpty()) {

                    markIterator.remove();
                }
            }


            if (playerMarks.isEmpty()) {

                playerIterator.remove();
            }
        }
    }


    /*
     * ============================================================
     * 网络
     * ============================================================
     */

    private static void sendMarkState(
            ServerPlayer viewer,
            UUID target,
            MarkState state
    ) {

        Entity entity =
                viewer.serverLevel()
                        .getEntity(
                                target
                        );

        if (entity == null) {
            return;
        }


        /*
         * 只要还有一种标记，
         * 客户端就继续显示轮廓。
         */
        boolean marked =
                state.hasOpening()
                        || state.hasClosing();


        DoorGhostMarkNetwork.sendMark(
                viewer,
                target,
                marked
        );
    }


    /*
     * ============================================================
     * 清理
     * ============================================================
     */

    public static void clear(
            ServerPlayer player
    ) {

        MARKS.remove(
                player.getUUID()
        );
    }
}