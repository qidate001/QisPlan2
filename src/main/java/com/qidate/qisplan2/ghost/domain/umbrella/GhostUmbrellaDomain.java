package com.qidate.qisplan2.ghost.domain.umbrella;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainUpdateMode;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GhostUmbrellaDomain {

    public static final double DOMAIN_RADIUS = 50.0D;

    public static final ResourceLocation TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_umbrella"
            );

    private GhostUmbrellaDomain() {
    }

    /**
     * 每 Tick 自动维护鬼雨伞鬼域。
     *
     * 撑着鬼雨伞：
     *  - 没有鬼域 -> 创建
     *  - 已有鬼域 -> 更新位置
     *
     * 没有撑着鬼雨伞：
     *  - 删除鬼雨伞鬼域
     */
    public static void tick(ServerPlayer player) {

        boolean umbrellaOpen =
                GhostUmbrellaItem.isOpen(player.getMainHandItem())
                        || GhostUmbrellaItem.isOpen(player.getOffhandItem());

        if (umbrellaOpen) {
            ensure(player);
            return;
        }

        GhostDomainManager manager =
                GhostDomainManager.get(player.serverLevel());

        if (manager.getBySourceAndType(
                player.getUUID(),
                TYPE
        ) != null) {
            remove(player);
        }
    }

    /**
     * 确保玩家拥有一个鬼雨伞鬼域。
     */
    public static void ensure(ServerPlayer player) {

        ServerLevel level = player.serverLevel();

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        UUID playerUUID = player.getUUID();

        GhostDomain domain =
                manager.getBySourceAndType(
                        playerUUID,
                        TYPE
                );

        if (domain == null) {

            domain = new GhostDomain(
                    UUID.randomUUID(),
                    playerUUID,
                    TYPE,
                    level.dimension(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    new CylinderDomainShape(DOMAIN_RADIUS),
                    GhostDomainUpdateMode.DISTANCE,
                    3.0D,
                    new GhostUmbrellaBehavior()
            );

            manager.add(domain);

            return;
        }

        // 鬼域跟随玩家移动
        manager.updatePosition(
                domain.getId(),
                player.getX(),
                player.getY(),
                player.getZ()
        );
    }

    /**
     * 删除玩家的鬼雨伞鬼域。
     */
    public static void remove(ServerPlayer player) {

        GhostDomainManager.get(player.serverLevel())
                .removeBySourceAndType(
                        player.getUUID(),
                        TYPE
                );
    }
}