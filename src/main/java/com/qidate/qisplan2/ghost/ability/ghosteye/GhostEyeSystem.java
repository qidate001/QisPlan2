package com.qidate.qisplan2.ghost.ability.ghosteye;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainUpdateMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class GhostEyeSystem {

    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_eye"
            );

    private static final double DOMAIN_RADIUS = 50.0D;

    private GhostEyeSystem() {
    }

    /**
     * 鬼眼每 tick 调用。
     */
    public static void tick(
            GhostAbilityContext context
    ) {

        ServerPlayer player =
                context.player();

        if (!(player.level()
                instanceof ServerLevel level)) {

            return;
        }

        /*
         * ========================================================
         * 当前鬼眼是否仍然存在
         * ========================================================
         */

        if (!PossessionHandler.hasGhost(
                player,
                GhostEyeAbility.ID
        )) {

            removeDomain(player);
            return;
        }

        /*
         * ========================================================
         * 获取当前鬼眼鬼域
         * ========================================================
         */

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        GhostDomain domain =
                manager.getBySourceAndType(
                        player.getUUID(),
                        DOMAIN_TYPE
                );

        /*
         * ========================================================
         * 第一次开启
         * ========================================================
         */

        if (domain == null) {

            createDomain(
                    level,
                    player
            );

            return;
        }

        /*
         * ========================================================
         * 更新鬼域位置
         * ========================================================
         */

        manager.updatePosition(
                domain.getId(),
                player.getX(),
                player.getY(),
                player.getZ()
        );
    }

    /**
     * 创建第 1 层鬼眼鬼域。
     */
    private static void createDomain(
            ServerLevel level,
            ServerPlayer player
    ) {

        double strength =
                PossessionHandler.getEffectiveStrength(
                        player,
                        GhostEyeAbility.ID
                );

        GhostDomain domain =
                new GhostDomain(
                        UUID.randomUUID(),
                        player.getUUID(),
                        DOMAIN_TYPE,
                        strength,
                        1,
                        level.dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        new CylinderDomainShape(
                                DOMAIN_RADIUS
                        ),
                        GhostDomainUpdateMode.DISTANCE,
                        3.0D,
                        new GhostEyeDomainBehavior()
                );

        GhostDomainManager.get(level)
                .add(domain);

        QisPlan2.LOGGER.info(
                "[鬼眼] 玩家 {} 开启了第1层鬼域，强度={}，半径={}",
                player.getGameProfile().getName(),
                strength,
                DOMAIN_RADIUS
        );
    }

    /**
     * 删除鬼眼鬼域。
     */
    public static void removeDomain(
            ServerPlayer player
    ) {

        if (!(player.level()
                instanceof ServerLevel level)) {

            return;
        }

        GhostDomainManager.get(level)
                .removeBySourceAndType(
                        player.getUUID(),
                        DOMAIN_TYPE
                );
    }
}