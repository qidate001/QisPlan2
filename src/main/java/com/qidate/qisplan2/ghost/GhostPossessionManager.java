package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.network.possession.GhostPossessionNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class GhostPossessionManager {

    private static final Map<
            UUID,
            GhostPossessionSession
            > SESSIONS = new HashMap<>();

    private GhostPossessionManager() {
    }


    /**
     * 是否正在驾驭。
     */
    public static boolean isPossessing(
            ServerPlayer player
    ) {
        return SESSIONS.containsKey(
                player.getUUID()
        );
    }


    /**
     * 开始驾驭。
     */
    public static boolean start(
            ServerPlayer player,
            GhostPossessionTarget target
    ) {

        if (isPossessing(player)) {
            return false;
        }

        GhostPossessionSession session =
                new GhostPossessionSession(
                        player,
                        target,
                        player.serverLevel()
                                .getRandom()
                                .nextLong()
                );

        SESSIONS.put(
                player.getUUID(),
                session
        );

        GhostPossessionNetwork.sendStart(
                player,
                session
        );

        return true;
    }


    /**
     * 每 tick。
     */
    public static void tick(
            MinecraftServer server
    ) {

        if (SESSIONS.isEmpty()) {
            return;
        }

        var iterator =
                SESSIONS.entrySet()
                        .iterator();

        while (iterator.hasNext()) {

            var entry =
                    iterator.next();

            UUID playerUUID =
                    entry.getKey();

            GhostPossessionSession session =
                    entry.getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    playerUUID
                            );

            if (player == null) {

                iterator.remove();

                continue;
            }

            /*
             * 推进小游戏。
             */
            session.tick();

            /*
             * 时间结束。
             */
            if (session.remainingTicks() <= 0) {

                finish(
                        player,
                        session
                );

                iterator.remove();

                continue;
            }

            GhostPossessionNetwork.sendUpdate(
                    player,
                    session
            );
        }
    }


    /**
     * 游戏结束。
     */
    private static void finish(
            ServerPlayer player,
            GhostPossessionSession session
    ) {

        double success =
                session.success();

        boolean won =
                player.serverLevel()
                        .getRandom()
                        .nextDouble()
                        * 100.0D
                        < success;

        /*
         * ========================================================
         * 目标处理
         * ========================================================
         */

        if (won) {

            boolean completed =
                    session.target()
                            .onSuccess(
                                    player
                            );

            /*
             * Target 自己也可能因为某些原因
             * 无法完成真正的驾驭。
             */
            if (!completed) {

                won = false;

                session.target()
                        .onFailure(
                                player
                        );
            }

        } else {

            session.target()
                    .onFailure(
                            player
                    );
        }

        /*
         * ========================================================
         * 关闭客户端小游戏
         * ========================================================
         */

        GhostPossessionNetwork.sendEnd(
                player,
                won,
                success
        );
    }

    public static GhostPossessionSession get(
            ServerPlayer player
    ) {
        return SESSIONS.get(
                player.getUUID()
        );
    }
}