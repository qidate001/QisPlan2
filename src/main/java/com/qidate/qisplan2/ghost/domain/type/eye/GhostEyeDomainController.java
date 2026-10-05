package com.qidate.qisplan2.ghost.domain.type.eye;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import com.qidate.qisplan2.ghost.possession.ability.ghosteye.GhostEyeAbility;
import com.qidate.qisplan2.ghost.domain.*;
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

    private GhostEyeDomainController() {
    }

    /**
     * 鬼眼每 tick 调用。
     *
     * <p>
     * 鬼眼是否开启由 PossessedGhostDomainData
     * 中的 open 字段决定。
     * </p>
     *
     * <p>
     * 如果鬼眼处于开启状态，但当前维度的
     * 运行时 GhostDomain 不存在，则自动重建。
     * </p>
     */
    public static void tick(
            GhostAbilityContext context
    ) {
        ServerPlayer player =
                context.player();

        /*
         * ========================================
         * 玩家已经失去鬼眼
         * ========================================
         */

        if (!PossessionHandler.hasGhost(
                player,
                GhostEyeAbility.ID
        )) {
            close(player);
            return;
        }

        /*
         * ========================================
         * 确保鬼眼鬼域存在
         * ========================================
         */

        ensureDomain(player);
    }

    /**
     * 确保玩家当前维度存在鬼眼鬼域。
     *
     * <p>
     * 持久鬼域状态：
     *
     * PossessedGhostDomainData
     *        ↓
     *      open
     *        ↓
     *  Runtime GhostDomain
     * </p>
     *
     * <p>
     * 如果鬼眼没有开启，则不会创建鬼域。
     * 如果鬼眼已经开启但运行时鬼域不存在，
     * 则自动重建。
     * </p>
     */
    public static void ensureDomain(
            ServerPlayer player
    ) {
        if (!(player.level()
                instanceof ServerLevel level)) {
            return;
        }

        /*
         * 玩家必须仍然驾驭鬼眼。
         */
        if (!PossessionHandler.hasGhost(
                player,
                GhostEyeAbility.ID
        )) {
            return;
        }

        /*
         * 获取鬼眼持久数据。
         */
        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostEyeAbility.ID
                );

        if (data == null) {
            return;
        }

        /*
         * 获取鬼域持久数据。
         */
        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null) {
            return;
        }

        /*
         * 鬼眼没有睁开，不创建运行时鬼域。
         */
        if (!domainData.open()) {
            return;
        }

        /*
         * 当前维度已经存在鬼域。
         */
        GhostDomainManager manager =
                GhostDomainManager.get(level);

        GhostDomain domain =
                manager.getBySourceAndType(
                        player.getUUID(),
                        DOMAIN_TYPE
                );

        if (domain != null) {
            return;
        }

        /*
         * 持久状态要求鬼眼开启，
         * 但运行时鬼域不存在。
         *
         * 重建。
         */
        createDomain(
                level,
                player
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
     *
     * <p>
     * 开启状态写入 PossessedGhostDomainData，
     * 然后创建运行时 GhostDomain。
     * </p>
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

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostEyeAbility.ID
                );

        if (data == null) {
            return;
        }

        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null) {
            return;
        }

        /*
         * 已经开启。
         */
        if (domainData.open()) {
            return;
        }

        /*
         * 写入持久状态。
         */
        PossessedGhostDomainData newDomainData =
                domainData.withOpen(true);

        PossessedGhostData newData =
                data.withDomain(
                        newDomainData
                );

        PossessionHandler.setData(
                player,
                GhostEyeAbility.ID,
                newData
        );

        /*
         * 创建运行时鬼域。
         */
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
     *
     * <p>
     * 关闭状态写入持久数据，
     * 同时删除运行时 GhostDomain。
     * </p>
     */
    public static void close(
            ServerPlayer player
    ) {
        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostEyeAbility.ID
                );

        boolean wasOpen = false;

        if (data != null) {

            PossessedGhostDomainData domainData =
                    data.domain().orElse(null);

            if (domainData != null) {

                wasOpen =
                        domainData.open();

                if (wasOpen) {

                    PossessedGhostData newData =
                            data.withDomain(
                                    domainData.withOpen(false)
                            );

                    PossessionHandler.setData(
                            player,
                            GhostEyeAbility.ID,
                            newData
                    );
                }
            }
        }

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
        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostEyeAbility.ID
                );

        if (data == null) {
            return false;
        }

        return data.domain()
                .map(PossessedGhostDomainData::open)
                .orElse(false);
    }

    /**
     * 创建鬼眼鬼域。
     *
     * <p>
     * 鬼域的强度、半径、层数全部来自
     * PossessedGhostDomainData。
     * </p>
     */
    private static void createDomain(
            ServerLevel level,
            ServerPlayer player
    ) {
        /*
         * ========================================
         * 防止重复创建
         * ========================================
         */

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        if (manager.getBySourceAndType(
                player.getUUID(),
                DOMAIN_TYPE
        ) != null) {
            return;
        }

        /*
         * ========================================
         * 获取持久数据
         * ========================================
         */

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostEyeAbility.ID
                );

        if (data == null) {
            return;
        }

        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null) {
            return;
        }

        /*
         * ========================================
         * 从持久数据读取鬼域属性
         * ========================================
         */

        double strength =
                domainData.strength();

        double radius =
                domainData.radius();

        int layer =
                domainData.layer();

        /*
         * ========================================
         * 创建运行时 GhostDomain
         * ========================================
         */

        GhostDomain domain =
                new GhostDomain(
                        UUID.randomUUID(),
                        player.getUUID(),
                        DOMAIN_TYPE,
                        strength,
                        layer,
                        radius,
                        level.dimension(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        new SphereDomainShape(
                                radius
                        ),
                        GhostDomainUpdateMode.DISTANCE,
                        3.0D,
                        new GhostEyeDomainBehavior()
                );

        manager.add(domain);

        QisPlan2.LOGGER.info(
                "[鬼眼] 玩家 {} 开启了第{}层鬼域，强度={}，半径={}",
                player.getGameProfile().getName(),
                layer,
                strength,
                radius
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