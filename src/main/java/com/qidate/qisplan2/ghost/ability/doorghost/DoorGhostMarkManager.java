package com.qidate.qisplan2.ghost.ability.doorghost;

import com.qidate.qisplan2.network.DoorGhostMarkNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/**
 * 开门鬼 / 关门鬼的驾驭标记管理器。
 *
 * 负责：
 *
 * 1. 记录某个驾驭者正在标记哪些实体。
 * 2. 记录标记剩余时间。
 * 3. 到期后通知客户端取消标记。
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
     * 玩家 UUID
     * ->
     * 被标记实体 UUID -> 剩余 tick
     */
    private static final Map<
            UUID,
            Map<UUID, Integer>
            > MARKS =
            new HashMap<>();

    private DoorGhostMarkManager() {
    }

    /**
     * 标记一个实体。
     */
    public static void mark(
            ServerPlayer viewer,
            UUID target
    ) {

        Map<UUID, Integer> playerMarks =
                MARKS.computeIfAbsent(
                        viewer.getUUID(),
                        ignored -> new HashMap<>()
                );

        playerMarks.put(
                target,
                MARK_DURATION
        );

        /*
         * 发送给这个玩家自己的客户端。
         */
        DoorGhostMarkNetwork.sendMark(
                viewer,
                target,
                true
        );
    }

    /**
     * 取消一个标记。
     */
    public static void unmark(
            ServerPlayer viewer,
            UUID target
    ) {

        Map<UUID, Integer> playerMarks =
                MARKS.get(
                        viewer.getUUID()
                );

        if (playerMarks == null) {
            return;
        }

        if (playerMarks.remove(
                target
        ) != null) {

            DoorGhostMarkNetwork.sendMark(
                    viewer,
                    target,
                    false
            );
        }

        if (playerMarks.isEmpty()) {
            MARKS.remove(
                    viewer.getUUID()
            );
        }
    }

    /**
     * 检查某个驾驭者是否已经标记了目标。
     */
    public static boolean isMarked(
            ServerPlayer viewer,
            UUID target
    ) {

        Map<UUID, Integer> playerMarks =
                MARKS.get(
                        viewer.getUUID()
                );

        return playerMarks != null
                && playerMarks.containsKey(
                target
        );
    }

    /**
     * 每服务器 Tick。
     */
    public static void tick(
            net.minecraft.server.MinecraftServer server
    ) {

        Iterator<
                Map.Entry<
                        UUID,
                        Map<UUID, Integer>
                        >
                > playerIterator =
                MARKS.entrySet().iterator();

        while (playerIterator.hasNext()) {

            Map.Entry<
                    UUID,
                    Map<UUID, Integer>
                    > playerEntry =
                    playerIterator.next();

            Map<UUID, Integer> playerMarks =
                    playerEntry.getValue();

            ServerPlayer viewer =
                    server.getPlayerList().getPlayer(
                            playerEntry.getKey()
                    );

            Iterator<
                    Map.Entry<
                            UUID,
                            Integer
                            >
                    > markIterator =
                    playerMarks.entrySet().iterator();

            while (markIterator.hasNext()) {

                Map.Entry<
                        UUID,
                        Integer
                        > mark =
                        markIterator.next();

                int remaining =
                        mark.getValue() - 1;

                if (remaining <= 0) {

                    if (viewer != null) {

                        DoorGhostMarkNetwork.sendMark(
                                viewer,
                                mark.getKey(),
                                false
                        );
                    }

                    markIterator.remove();

                } else {

                    mark.setValue(
                            remaining
                    );
                }
            }

            if (playerMarks.isEmpty()) {
                playerIterator.remove();
            }
        }
    }

    public static LivingEntity findNearestMarkedTarget(
            ServerPlayer viewer,
            ServerLevel level
    ) {

        Map<UUID, Integer> marks =
                MARKS.get(
                        viewer.getUUID()
                );

        if (marks == null
                || marks.isEmpty()) {

            return null;
        }

        LivingEntity best =
                null;

        double bestDistance =
                Double.MAX_VALUE;

        Iterator<UUID> iterator =
                marks.keySet().iterator();

        while (iterator.hasNext()) {

            UUID targetUUID =
                    iterator.next();

            Entity entity =
                    level.getEntity(
                            targetUUID
                    );

            if (!(entity instanceof LivingEntity living)
                    || !living.isAlive()) {

                iterator.remove();

                continue;
            }

            if (living == viewer) {

                iterator.remove();

                continue;
            }

            double distance =
                    viewer.distanceToSqr(
                            living
                    );

            if (distance < bestDistance) {

                bestDistance =
                        distance;

                best =
                        living;
            }
        }

        if (marks.isEmpty()) {

            MARKS.remove(
                    viewer.getUUID()
            );
        }

        return best;
    }

    /**
     * 找到当前在线玩家。
     *
     * 这里暂时通过服务器列表查找。
     */
    private static ServerPlayer findPlayer(
            UUID uuid
    ) {

        /*
         * 这个方法需要服务器实例，
         * 所以后续 Tick 中如果你希望更严谨，
         * 可以把 ServerPlayer 映射直接存进去。
         *
         * 这里先留给下一版完善。
         */
        return null;
    }



    /**
     * 玩家退出服务器时清理。
     */
    public static void clear(
            ServerPlayer player
    ) {

        MARKS.remove(
                player.getUUID()
        );
    }
}