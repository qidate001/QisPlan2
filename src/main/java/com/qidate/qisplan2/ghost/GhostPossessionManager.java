package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.core.ModDataComponents;
import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.ghost.ability.divinationslip.GhostDivinationSlipAbility;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

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

        QisNetwork.sendPossessionStart(
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

            QisNetwork.sendPossessionUpdate(
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

        QisNetwork.sendPossessionEnd(
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