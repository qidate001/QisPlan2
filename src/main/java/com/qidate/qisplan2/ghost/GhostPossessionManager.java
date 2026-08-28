package com.qidate.qisplan2.ghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.core.ModItems;
import com.qidate.qisplan2.death.SupernaturalEntity;
import com.qidate.qisplan2.entity.AbstractGhostEntity;
import com.qidate.qisplan2.network.QisNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            Entity ghost
    ) {

        if (!(ghost instanceof SupernaturalEntity supernatural)) {
            return false;
        }

        /*
         * 已经在驾驭其他鬼。
         */
        if (isPossessing(player)) {
            return false;
        }

        /*
         * 必须是普通死机。
         */
        if (!supernatural.isSupernaturallyStunned()) {
            return false;
        }

        if (supernatural
                .isPermanentlySupernaturallyStunned()) {
            return false;
        }

        GhostPossessionSession session =
                new GhostPossessionSession(
                        player,
                        ghost.getUUID(),
                        ghost.getId(),
                        player.serverLevel()
                                .getRandom()
                                .nextLong()
                );

        SESSIONS.put(
                player.getUUID(),
                session
        );

        /*
         * 打开驾驭小游戏。
         */
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

            ServerLevel level =
                    player.serverLevel();

            Entity ghost =
                    level.getEntity(
                            session.ghostEntityId()
                    );

            /*
             * 鬼不存在了。
             */
            if (ghost == null
                    || ghost.getUUID().compareTo(
                    session.ghostUUID()
            ) != 0) {

                QisNetwork.sendPossessionEnd(
                        player,
                        false,
                        session.success()
                );

                iterator.remove();

                continue;
            }

            /*
             * 鬼已经不是普通死机。
             */
            if (!(ghost instanceof SupernaturalEntity supernatural)
                    || !supernatural.isSupernaturallyStunned()
                    || supernatural.isPermanentlySupernaturallyStunned()) {

                /*
                 * 鬼已经提前结束普通死机。
                 *
                 * 驾驭小游戏必须同步结束，
                 * 否则客户端 GUI 会永远停在那里。
                 */
                QisNetwork.sendPossessionEnd(
                        player,
                        false,
                        session.success()
                );

                iterator.remove();

                continue;
            }

            /*
             * 推进小游戏。
             */
            session.tick();

            /*
             * 防止最后的 tick 会先发送一个 remainingTicks = 0 的 Update，然后马上发送 End
             */
            if (session.remainingTicks() <= 0) {

                finish(
                        player,
                        ghost,
                        session
                );

                iterator.remove();

                continue;
            }

            QisNetwork.sendPossessionUpdate(
                    player,
                    session
            );

            /*
             * 时间结束。
             */
            if (session.remainingTicks()
                    <= 0) {

                finish(
                        player,
                        ghost,
                        session
                );

                iterator.remove();
            }
        }
    }


    /**
     * 游戏结束。
     */
    private static void finish(
            ServerPlayer player,
            Entity ghost,
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
         * ========================================
         * 成功
         * ========================================
         */
        if (won) {

            /*
             * 获取实体类型对应的 ResourceLocation。
             */
            var typeKey =
                    net.minecraft.core.registries.BuiltInRegistries
                            .ENTITY_TYPE
                            .getKey(
                                    ghost.getType()
                            );

            if (typeKey != null) {

                boolean possessed =
                        PossessionHandler.possess(
                                player,
                                typeKey
                        );

                /*
                 * ====================================================
                 * 驾驭成功：
                 *
                 * 如果鬼原本被棺材钉钉住，
                 * 视为棺材钉被拔出。
                 *
                 * 棺材钉返还给玩家。
                 * ====================================================
                 */

                if (possessed
                        && ghost instanceof AbstractGhostEntity abstractGhost
                        && abstractGhost.isCoffinNailed()) {

                    abstractGhost.setCoffinNailed(
                            false
                    );

                    ItemStack nail =
                            new ItemStack(
                                    ModItems.COFFIN_NAIL.get()
                            );

                    if (!player.isCreative()) {

                        if (!player.getInventory().add(
                                nail
                        )) {

                            player.drop(
                                    nail,
                                    false
                            );
                        }
                    }
                }
            }

            /*
             * 鬼消失。
             */
            ghost.discard();
        }

        /*
         * ========================================
         * 失败
         * ========================================
         */
        else {

            if (ghost instanceof SupernaturalEntity supernatural) {

                /*
                 * 普通死机清零。
                 */
                supernatural.clearSupernaturalStun();
            }
        }

        /*
         * 最终关闭客户端小游戏。
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