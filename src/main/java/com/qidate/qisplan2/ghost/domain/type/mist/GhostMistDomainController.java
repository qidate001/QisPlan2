package com.qidate.qisplan2.ghost.domain.type.mist;
import com.qidate.qisplan2.ghost.module.host.entity.GhostMistEntityBinding;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.ModularGhostEntity;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainUpdateMode;
import com.qidate.qisplan2.ghost.domain.SphereDomainShape;
import com.qidate.qisplan2.ghost.module.GhostModuleContext;
import com.qidate.qisplan2.ghost.module.host.player.PlayerGhostModuleContext;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistModule;
import com.qidate.qisplan2.ghost.module.implementation.mist.GhostMistState;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostData;
import com.qidate.qisplan2.ghost.possession.data.PossessedGhostDomainData;
import com.qidate.qisplan2.ghost.possession.manager.PossessionHandler;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * ========================================
 * 鬼雾鬼域控制器
 * ========================================
 *
 * 负责：
 *
 * 1. 创建鬼雾实体的鬼域
 * 2. 从鬼雾模块 customData 恢复鬼域状态
 * 3. 保持模块持久化数据与 GhostDomain 状态同步
 * 4. 处理鬼雾实体成长
 *
 * 5. 管理玩家驾驭鬼雾时的鬼域
 * 6. 开启 / 关闭玩家的鬼雾鬼域
 *
 * 持久化数据：
 *
 * - 世界 GhostMist：
 *      由鬼雾模块的 customData 负责
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

    /**
     * 每成功杀死一个目标，
     * 鬼域强度增加 0.1。
     */
    private static final double STRENGTH_GROWTH = 0.1D;

    /**
     * 每成功杀死一个目标，
     * 鬼域半径增加 0.05。
     */
    private static final double RADIUS_GROWTH = 0.05D;


    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_mist"
            );

    /**
     * 禁止外部实例化此工具类；功能统一通过静态入口调用。
     */
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
     * 持久鬼域状态由 PossessedGhostDomainData 决定。
     * 如果鬼域处于开启状态，则确保当前维度存在
     * 对应的运行时 GhostDomain。
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
            PlayerGhostModuleContext context
    ) {

        ServerPlayer player =
                context.player();

        /*
         * ========================================================
         * 已经失去鬼雾
         * ========================================================
         */

        if (!PossessionHandler.hasGhost(
                player,
                GhostMistModule.ID
        )) {

            close(player);
            return;
        }

        /*
         * ========================================================
         * 确保持久鬼域对应的运行时鬼域存在
         * ========================================================
         */

        ensureDomain(player);
    }

    /**
     * 确保玩家当前维度存在鬼雾鬼域。
     *
     * <p>
     * 这是：
     *
     * 持久鬼域状态
     *      ↓
     * 运行时 GhostDomain
     *
     * 的桥梁。
     * </p>
     *
     * <p>
     * 如果鬼域已经存在，则什么都不做。
     * 如果持久数据不存在、鬼域未开启，
     * 或玩家没有驾驭鬼雾，则什么都不做。
     * </p>
     */
    public static void ensureDomain(
            ServerPlayer player
    ) {

        if (!(player.level()
                instanceof ServerLevel level)) {

            return;
        }

        if (!PossessionHandler.hasGhost(
                player,
                GhostMistModule.ID
        )) {

            return;
        }

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistModule.ID
                );

        if (data == null) {
            return;
        }

        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null || !domainData.open()) {
            return;
        }

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

        createDomain(
                level,
                player
        );
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
                GhostMistModule.ID
        )) {

            return;
        }

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistModule.ID
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
                GhostMistModule.ID,
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
                        GhostMistModule.ID
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
                            GhostMistModule.ID,
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
                        GhostMistModule.ID
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
                        GhostMistModule.ID
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
     * 如果不存在，则使用鬼雾模块 customData 中
     * 保存的鬼域状态重新创建。
     * </p>
     */
    public static void createDomain(ServerLevel level, ModularGhostEntity ghost) {
        if (!GhostMistEntityBinding.isActive(ghost)) return;
        var manager = GhostDomainManager.get(level);
        var data = GhostMistState.read(ghost);
        double strength = data.getDouble("domainStrength");
        double radius = data.getDouble("domainRadius");
        int layer = data.getInt("domainLayer");
        var domain = manager.getBySourceAndType(ghost.getUUID(), DOMAIN_TYPE);
        if (domain == null) {
            domain = new GhostDomain(UUID.randomUUID(), ghost.getUUID(), DOMAIN_TYPE, strength, layer,
                    radius, level.dimension(), ghost.getX(), ghost.getY(), ghost.getZ(),
                    new SphereDomainShape(radius), GhostDomainUpdateMode.DISTANCE, 3.0D,
                    new GhostMistDomainBehavior());
            manager.add(domain);
        } else if (domain.getStrength() != strength || domain.getRadius() != radius || domain.getLayer() != layer) {
            domain.setStrength(strength);
            domain.setRadius(radius);
            domain.setLayer(layer);
            manager.syncUpdate(domain);
        }
    }

    /** 击杀成长先写回模块，再同步运行时鬼域；模块卸载和重新加载不会丢失成长值。 */
    public static void grow(ServerLevel level, ModularGhostEntity ghost, GhostDomain domain) {
        if (!GhostMistEntityBinding.isActive(ghost)) return;
        double strength = domain.getStrength() + STRENGTH_GROWTH;
        double radius = domain.getRadius() + RADIUS_GROWTH;
        GhostMistState.save(ghost, strength, radius, domain.getLayer());
        // 本体强度与鬼域强度独立保存，保留每次击杀本体也增长 0.1 的效果。
        ghost.setIntensity(GhostMistModule.ID,
                new GhostModuleContext(ghost, GhostMistModule.ID).intensity() + STRENGTH_GROWTH);
        domain.setStrength(strength);
        domain.setRadius(radius);
        GhostDomainManager.get(level).syncUpdate(domain);
    }

    /**
     * 玩家驾驭鬼雾后的鬼域成长。
     *
     * <p>
     * 与世界中的 GhostMist 实体不同，
     * 玩家鬼雾的成长数据必须写回
     * PossessedGhostDomainData，
     * 从而在玩家退出游戏、切换维度、
     * 重新进入游戏后仍然保留。
     * </p>
     *
     * <p>
     * 每成功击杀一个目标：
     *
     * 强度 +0.1
     * 半径 +0.05
     *
     * 持久数据与运行时 GhostDomain
     * 同时更新。
     * </p>
     */
    public static void grow(
            ServerPlayer player,
            GhostDomain domain
    ) {
        /*
         * ========================================
         * 获取玩家当前驾驭的鬼雾数据
         * ========================================
         */

        PossessedGhostData data =
                PossessionHandler.getData(
                        player,
                        GhostMistModule.ID
                );

        if (data == null) {
            return;
        }

        /*
         * ========================================
         * 获取鬼域持久数据
         * ========================================
         */

        PossessedGhostDomainData domainData =
                data.domain().orElse(null);

        if (domainData == null) {
            return;
        }

        /*
         * ========================================
         * 计算新的鬼域属性
         * ========================================
         */

        double newStrength =
                domainData.strength()
                        + STRENGTH_GROWTH;

        double newRadius =
                domainData.radius()
                        + RADIUS_GROWTH;

        /*
         * ========================================
         * 写回持久数据
         * ========================================
         */

        PossessedGhostDomainData newDomainData =
                domainData
                        .withStrength(newStrength)
                        .withRadius(newRadius);

        PossessedGhostData newData =
                data.withDomain(
                        newDomainData
                );

        PossessionHandler.setData(
                player,
                GhostMistModule.ID,
                newData
        );

        /*
         * ========================================
         * 更新运行时 GhostDomain
         * ========================================
         */

        domain.setStrength(
                newStrength
        );

        domain.setRadius(
                newRadius
        );

        /*
         * ========================================
         * 同步客户端
         * ========================================
         */

        if (player.level()
                instanceof ServerLevel level) {

            GhostDomainManager.get(level)
                    .syncUpdate(
                            domain
                    );
        }

        QisPlan2.LOGGER.info(
                "[鬼雾] 玩家 {} 的鬼域成长：强度={}，半径={}",
                player.getGameProfile().getName(),
                newStrength,
                newRadius
        );
    }
}