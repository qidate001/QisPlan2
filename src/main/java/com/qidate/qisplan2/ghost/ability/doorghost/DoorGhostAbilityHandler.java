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
        QisPlan2.LOGGER.info(
                "[QisPlan2] DoorGhostAbilityHandler：触发者={}，门={}",
                source.getName().getString(),
                doorPos
        );

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

            QisPlan2.LOGGER.info(
                    "[QisPlan2] 玩家 {}：开门鬼={}，关门鬼={}",
                    player.getName().getString(),
                    PossessionHandler.hasGhost(
                            player,
                            OpeningGhostAbility.ID
                    ),
                    PossessionHandler.hasGhost(
                            player,
                            ClosingGhostAbility.ID
                    )
            );

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