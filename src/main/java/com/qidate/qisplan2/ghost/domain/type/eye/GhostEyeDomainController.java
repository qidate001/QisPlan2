package com.qidate.qisplan2.ghost.domain.type.eye;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.GhostAbilityContext;
import com.qidate.qisplan2.ghost.PossessionHandler;
import com.qidate.qisplan2.ghost.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.CylinderDomainShape;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainUpdateMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 鬼眼鬼域控制器。
 *
 * 负责：
 *  - 创建鬼域
 *  - 删除鬼域
 *  - 维护鬼域位置
 *  - 开关鬼眼鬼域
 */
public final class GhostEyeDomainController {

    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_eye"
            );

    private static final double DOMAIN_RADIUS = 50.0D;

    /**
     * 当前主动开启鬼眼的玩家。
     */
    private static final Set<UUID> OPEN_EYES =
            new HashSet<>();

    private GhostEyeDomainController() {
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
         * 已经失去鬼眼
         * ========================================================
         */

        if (!PossessionHandler.hasGhost(
                player,
                GhostEyeAbility.ID
        )) {

            close(player);
            return;
        }

        /*
         * ========================================================
         * 鬼眼当前没有开启
         * ========================================================
         */

        if (!OPEN_EYES.contains(player.getUUID())) {
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
         * 鬼眼已经开启，但鬼域不存在
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

    public static int getEyeLayer(
            ServerPlayer player
    ) {

        if (!isOpen(player)) {
            return 0;
        }

        GhostDomain domain =
                GhostDomainManager.get(
                        player.serverLevel()
                ).getBySourceAndType(
                        player.getUUID(),
                        DOMAIN_TYPE
                );

        if (domain == null) {
            return 0;
        }

        return domain.getLayer();
    }

    /**
     * 开启鬼眼。
     */
    public static void open(
            ServerPlayer player
    ) {

        if (!PossessionHandler.hasGhost(
                player,
                GhostEyeAbility.ID
        )) {
            return;
        }

        UUID uuid = player.getUUID();

        /*
         * 已经开启
         */
        if (!OPEN_EYES.add(uuid)) {
            return;
        }

        if (player.level()
                instanceof ServerLevel level) {

            createDomain(
                    level,
                    player
            );
        }

        QisPlan2.LOGGER.info(
                "[鬼眼] 玩家 {} 睁开了鬼眼",
                player.getGameProfile().getName()
        );
    }

    /**
     * 关闭鬼眼。
     */
    public static void close(
            ServerPlayer player
    ) {

        boolean wasOpen =
                OPEN_EYES.remove(
                        player.getUUID()
                );

        removeDomain(player);

        if (wasOpen) {

            QisPlan2.LOGGER.info(
                    "[鬼眼] 玩家 {} 闭合了鬼眼",
                    player.getGameProfile().getName()
            );
        }
    }

    /**
     * 当前鬼眼是否开启。
     */
    public static boolean isOpen(
            ServerPlayer player
    ) {

        return OPEN_EYES.contains(
                player.getUUID()
        );
    }

    /**
     * 创建第 1 层鬼眼鬼域。
     */
    private static void createDomain(
            ServerLevel level,
            ServerPlayer player
    ) {

        /*
         * 防止重复创建。
         */
        GhostDomainManager manager =
                GhostDomainManager.get(level);

        if (manager.getBySourceAndType(
                player.getUUID(),
                DOMAIN_TYPE
        ) != null) {
            return;
        }

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
                        DOMAIN_RADIUS,
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

        manager.add(domain);

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