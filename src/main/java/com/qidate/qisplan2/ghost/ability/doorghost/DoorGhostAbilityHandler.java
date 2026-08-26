package com.qidate.qisplan2.ghost.ability.doorghost;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.PossessionHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public final class DoorGhostAbilityHandler {

    private DoorGhostAbilityHandler() {
    }

    public static void onDoorChanged(
            ServerLevel level,
            BlockPos doorPos,
            LivingEntity source
    ) {

        double centerX =
                doorPos.getX() + 0.5D;

        double centerY =
                doorPos.getY() + 0.5D;

        double centerZ =
                doorPos.getZ() + 0.5D;

        double radius =
                100.0D;

        AABB box =
                new AABB(
                        centerX,
                        centerY,
                        centerZ,
                        centerX,
                        centerY,
                        centerZ
                ).inflate(
                        radius
                );

        double radiusSqr =
                radius * radius;

        /*
         * 只检查附近的在线玩家。
         */
        for (ServerPlayer player :
                level.getEntitiesOfClass(
                        ServerPlayer.class,
                        box
                )) {
            /*
             * ========================================================
             * 驾驭者自己触发门事件：
             *
             * 不标记自己。
             *
             * 否则第一人称下会把自己的整个屏幕
             * 都套上 Outline。
             * ========================================================
             */
            if (player == source) {
                continue;
            }

            if (player.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
            ) > radiusSqr) {

                continue;
            }

            /*
             * ====================================================
             * 开门鬼驾驭者
             * ====================================================
             */

            if (PossessionHandler.hasGhost(
                    player,
                    OpeningGhostAbility.ID
            )) {

                DoorGhostMarkManager.mark(
                        player,
                        source.getUUID()
                );
            }

            /*
             * ====================================================
             * 关门鬼驾驭者
             * ====================================================
             */

            if (PossessionHandler.hasGhost(
                    player,
                    ClosingGhostAbility.ID
            )) {

                DoorGhostMarkManager.mark(
                        player,
                        source.getUUID()
                );
            }
        }
    }
}