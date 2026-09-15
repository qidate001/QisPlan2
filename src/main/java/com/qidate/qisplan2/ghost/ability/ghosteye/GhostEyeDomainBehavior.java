package com.qidate.qisplan2.ghost.ability.ghosteye;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import com.qidate.qisplan2.ghost.layer.GhostLayerHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class GhostEyeDomainBehavior
        implements GhostDomainBehavior {

    @Override
    public void onCreate(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    @Override
    public void tick(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    @Override
    public void onRemove(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    @Override
    public void onEntityEnter(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {

        GhostLayerHandler.setLayer(
                entity,
                domain.getLayer()
        );

        if (entity instanceof ServerPlayer player) {
            QisPlan2.LOGGER.info(
                    "[鬼眼] 玩家 {} 进入鬼域 {}，层数：{}",
                    player.getName().getString(),
                    domain.getId(),
                    domain.getLayer()
            );
        }
    }

    @Override
    public void onEntityLeave(
            ServerLevel level,
            GhostDomain domain,
            Entity entity
    ) {

        GhostLayerHandler.setLayer(
                entity,
                0
        );

        if (entity instanceof ServerPlayer player) {
            QisPlan2.LOGGER.info(
                    "[鬼眼] 玩家 {} 离开鬼域 {}，层数恢复为 0",
                    player.getName().getString(),
                    domain.getId()
            );
        }
    }

    @Override
    public void onEntitySwitch(
            ServerLevel level,
            GhostDomain oldDomain,
            GhostDomain newDomain,
            Entity entity
    ) {

        GhostLayerHandler.setLayer(
                entity,
                newDomain.getLayer()
        );

        if (entity instanceof ServerPlayer player) {
            QisPlan2.LOGGER.info(
                    "[鬼眼] 玩家 {} 切换鬼域：{} → {}，层数：{} → {}",
                    player.getName().getString(),
                    oldDomain.getId(),
                    newDomain.getId(),
                    oldDomain.getLayer(),
                    newDomain.getLayer()
            );
        }
    }
}