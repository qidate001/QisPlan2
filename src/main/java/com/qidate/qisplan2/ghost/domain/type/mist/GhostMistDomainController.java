package com.qidate.qisplan2.ghost.domain.type.mist;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.GhostAttributeSystem;
import com.qidate.qisplan2.entity.ghostmist.GhostMist;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainUpdateMode;
import com.qidate.qisplan2.ghost.domain.SphereDomainShape;
import com.qidate.qisplan2.ghost.possession.ability.GhostAbilityContext;
import com.qidate.qisplan2.ghost.possession.ability.ghostmist.GhostMistAbility;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * ========================================
 * 鬼雾鬼域控制器
 * ========================================
 *
 * 负责：
 *
 * 1. 创建鬼雾实体的鬼域
 * 2. 从鬼雾实体恢复鬼域状态
 * 3. 保持 GhostMist 与 GhostDomain 状态同步
 * 4. 处理鬼雾实体成长
 *
 * 5. 管理玩家驾驭鬼雾时的鬼域
 * 6. 开启 / 关闭玩家的鬼雾鬼域
 *
 * 持久化数据：
 *
 * - 世界 GhostMist：
 *      由 GhostMist 实体负责
 *
 * - 玩家驾驭鬼雾：
 *      暂时沿用现有驭鬼数据体系
 *      具体数据结构后续统一重构
 *
 * GhostDomain：
 *
 *      只负责运行时状态。
 */
public final class GhostMistDomainController {

    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_mist"
            );

    private GhostMistDomainController() {
    }


    /*
     * ============================================================
     * 玩家驾驭鬼雾：生命周期
     * ============================================================
     */

    /**
     * 鬼雾每 tick 调用。
     *
     * <p>
     * 负责维护玩家驾驭鬼雾后的主动鬼域。
     * </p>
     *
     * <p>
     * 注意：
     *
     * 世界中的 GhostMist 实体鬼域
     * 不经过这里的玩家生命周期逻辑。
     * </p>
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
         * 已经失去鬼雾
         * ========================================================
         */

        if (!PossessionHandler.hasGhost(
                player,
                GhostMistAbility.ID
        )) {

            close(player);
            return;
        }

        /*
         * ========================================================
         * 获取玩家鬼雾持久数据
         * ========================================================
         */

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistAbility.ID
                );

        if (data == null) {
            return;
        }

        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null || !domainData.open()) {
            return;
        }

        /*
         * ========================================================
         * 获取当前鬼雾鬼域
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
         * 鬼雾已经开启，但鬼域不存在
         * ========================================================
         */

        if (domain == null) {

            /*
             * ====================================================
             * 这里暂时只恢复生命周期。
             *
             * 玩家鬼雾的强度、半径等持久化数据，
             * 等驭鬼数据结构重构后再正式接入。
             * ====================================================
             */

            createDomain(
                    level,
                    player
            );
        }
    }


    /**
     * 开启鬼雾鬼域。
     */
    public static void open(
            ServerPlayer player
    ) {

        /*
         * ========================================================
         * 玩家必须已经驾驭鬼雾
         * ========================================================
         */

        if (!PossessionHandler.hasGhost(
                player,
                GhostMistAbility.ID
        )) {

            return;
        }

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistAbility.ID
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
         * ========================================================
         * 已经开启
         * ========================================================
         */

        if (domainData.open()) {
            return;
        }

        /*
         * ========================================================
         * 更新持久鬼域状态
         * ========================================================
         */

        PossessedGhostDomainData newDomainData =
                domainData.withOpen(true);

        PossessedGhostData newData =
                data.withDomain(
                        newDomainData
                );

        PossessionHandler.setData(
                player,
                GhostMistAbility.ID,
                newData
        );

        /*
         * ========================================================
         * 创建玩家源头鬼域
         * ========================================================
         */

        if (player.level()
                instanceof ServerLevel level) {

            createDomain(
                    level,
                    player
            );
        }

        QisPlan2.LOGGER.info(
                "[鬼雾] 玩家 {} 开启了鬼雾鬼域",
                player.getGameProfile().getName()
        );
    }


    /**
     * 关闭鬼雾鬼域。
     *
     * <p>
     * 无论玩家是否处于开启状态，
     * 都会尝试删除对应的 GhostDomain。
     * </p>
     */
    public static void close(
            ServerPlayer player
    ) {

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistAbility.ID
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
                            GhostMistAbility.ID,
                            newData
                    );
                }
            }
        }

        /*
         * ========================================================
         * 删除运行时鬼域
         * ========================================================
         */

        removeDomain(player);

        if (wasOpen) {

            QisPlan2.LOGGER.info(
                    "[鬼雾] 玩家 {} 关闭了鬼雾鬼域",
                    player.getGameProfile().getName()
            );
        }
    }


    /**
     * 当前玩家的鬼雾鬼域是否开启。
     */
    public static boolean isOpen(
            ServerPlayer player
    ) {

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistAbility.ID
                );

        if (data == null) {
            return false;
        }

        return data.domain()
                .map(PossessedGhostDomainData::open)
                .orElse(false);
    }


    /**
     * 切换鬼雾鬼域。
     */
    public static void toggle(
            ServerPlayer player
    ) {

        if (isOpen(player)) {

            close(player);

        } else {

            open(player);
        }
    }


    /*
     * ============================================================
     * 玩家鬼雾：创建鬼域
     * ============================================================
     */

    /**
     * 创建玩家源头的鬼雾鬼域。
     *
     * <p>
     * 注意：
     *
     * 当前这里只建立运行时结构。
     *
     * 玩家驾驭鬼雾所使用的具体：
     *
     * - 强度
     * - 半径
     * - 其他成长数据
     *
     * 等驭鬼数据结构重构后再正式接入。
     * </p>
     */
    private static void createDomain(
            ServerLevel level,
            ServerPlayer player
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        /*
         * ========================================================
         * 防止重复创建
         * ========================================================
         */

        if (manager.getBySourceAndType(
                player.getUUID(),
                DOMAIN_TYPE
        ) != null) {

            return;
        }

        /*
         * ========================================================
         * 临时运行时参数
         * ========================================================
         *
         * 这里只是为了先把生命周期跑通。
         *
         * 后续重构 PossessedGhostState / 驭鬼数据后，
         * 会替换成真正的源头数据。
         */

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistAbility.ID
                );

        if (data == null) {
            return;
        }

        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null) {
            return;
        }

        double strength =
                domainData.strength();

        double radius =
                domainData.radius();

        int layer =
                domainData.layer();

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
                        new GhostMistDomainBehavior()
                );

        manager.add(domain);

        QisPlan2.LOGGER.info(
                "[鬼雾] 玩家 {} 创建鬼域，强度={}，半径={}",
                player.getGameProfile().getName(),
                strength,
                radius
        );
    }


    /**
     * 删除玩家源头的鬼雾鬼域。
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


    /*
     * ============================================================
     * 世界 GhostMist 实体：创建 / 恢复鬼域
     * ============================================================
     */

    /**
     * 创建 / 恢复世界中的 GhostMist 鬼域。
     *
     * <p>
     * 如果鬼域已经存在，则不重复创建。
     *
     * 如果不存在，则使用 GhostMist 实体中
     * 保存的鬼域状态重新创建。
     * </p>
     */
    public static void createDomain(
            ServerLevel level,
            GhostMist ghost
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        GhostDomain existingDomain =
                manager.getBySourceAndType(
                        ghost.getUUID(),
                        DOMAIN_TYPE
                );

        if (existingDomain != null) {

            /*
             * ====================================================
             * 已存在鬼域
             * ====================================================
             *
             * 正常运行过程中：
             *
             * GhostDomain 是运行时状态的权威来源。
             *
             * 因此这里不重新覆盖 Domain。
             */

            return;
        }

        /*
         * ========================================================
         * 使用实体持久化数据恢复鬼域
         * ========================================================
         */

        double strength =
                ghost.getDomainStrength();

        double radius =
                ghost.getDomainRadius();

        GhostDomain domain =
                new GhostDomain(
                        UUID.randomUUID(),
                        ghost.getUUID(),
                        DOMAIN_TYPE,
                        strength,
                        1,
                        radius,
                        level.dimension(),
                        ghost.getX(),
                        ghost.getY(),
                        ghost.getZ(),
                        new SphereDomainShape(
                                radius
                        ),
                        GhostDomainUpdateMode.DISTANCE,
                        3.0D,
                        new GhostMistDomainBehavior()
                );

        manager.add(domain);

        /*
         * ========================================================
         * 保证厉鬼自身灵异强度
         * 与鬼域强度一致
         * ========================================================
         */

        GhostAttributeSystem.setSupernaturalStrength(
                ghost,
                strength
        );

        QisPlan2.LOGGER.info(
                "[鬼雾] 鬼雾源头 {} 创建/恢复鬼域，强度={}，半径={}",
                ghost.getUUID(),
                strength,
                radius
        );
    }


    /*
     * ============================================================
     * 世界 GhostMist 实体：成长
     * ============================================================
     */

    /**
     * 每成功杀死一个目标：
     *
     * Strength +0.1
     * Radius +0.05
     *
     * 同时同步：
     *
     * GhostMist
     * GhostDomain
     * 客户端
     */
    public static void grow(
            ServerLevel level,
            GhostMist ghost,
            GhostDomain domain
    ) {

        /*
         * ========================================================
         * 计算新的状态
         * ========================================================
         */

        double newStrength =
                domain.getStrength() + 0.1D;

        double newRadius =
                domain.getRadius() + 0.05D;

        /*
         * ========================================================
         * 1. 更新 GhostDomain
         * ========================================================
         */

        domain.setStrength(
                newStrength
        );

        domain.setRadius(
                newRadius
        );

        /*
         * ========================================================
         * 2. 同步回 GhostMist
         * ========================================================
         */

        ghost.setDomainStrength(
                newStrength
        );

        ghost.setDomainRadius(
                newRadius
        );

        /*
         * ========================================================
         * 3. 厉鬼灵异强度同步
         * ========================================================
         */

        GhostAttributeSystem.setSupernaturalStrength(
                ghost,
                newStrength
        );

        /*
         * ========================================================
         * 4. 通知客户端 Domain 状态发生变化
         * ========================================================
         */

        GhostDomainManager.get(level)
                .syncUpdate(
                        domain
                );
    }
}