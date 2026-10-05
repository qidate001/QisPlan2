package com.qidate.qisplan2.ghost.domain.type.mist;

import com.qidate.qisplan2.QisPlan2;
import com.qidate.qisplan2.entity.GhostAttributeSystem;
import com.qidate.qisplan2.entity.ghostmist.GhostMist;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainManager;
import com.qidate.qisplan2.ghost.domain.GhostDomainUpdateMode;
import com.qidate.qisplan2.ghost.domain.SphereDomainShape;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

/**
 * ========================================
 * 鬼雾鬼域控制器
 * ========================================
 *
 * 负责：
 *
 * 1. 创建鬼雾鬼域
 * 2. 从鬼雾实体恢复鬼域状态
 * 3. 保持 GhostMist 与 GhostDomain 状态同步
 * 4. 处理鬼雾成长
 *
 * 持久化数据由 GhostMist 实体负责。
 *
 * GhostDomain 则负责运行时状态。
 */
public final class GhostMistDomainController {

    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_mist"
            );

    private static final double DEFAULT_DOMAIN_RADIUS =
            80.0D;

    private GhostMistDomainController() {
    }

    /**
     * ========================================
     * 创建 / 恢复鬼域
     * ========================================
     *
     * 如果鬼域已经存在，则不重复创建。
     *
     * 如果不存在，则使用 GhostMist 实体中
     * 保存的鬼域状态重新创建。
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
             * ========================================
             * 已存在鬼域
             * ========================================
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
         * ========================================
         * 使用实体持久化数据恢复鬼域
         * ========================================
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
         * ========================================
         * 保证厉鬼自身灵异强度
         * 与鬼域强度一致
         * ========================================
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

    /**
     * ========================================
     * 鬼雾成长
     * ========================================
     *
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
         * ========================================
         * 计算新的状态
         * ========================================
         */

        double newStrength =
                domain.getStrength() + 0.1D;

        double newRadius =
                domain.getRadius() + 0.05D;

        /*
         * ========================================
         * ① 更新 GhostDomain
         * ========================================
         *
         * Domain 是运行时状态的权威来源。
         */

        domain.setStrength(
                newStrength
        );

        domain.setRadius(
                newRadius
        );

        /*
         * ========================================
         * ② 同步回 GhostMist
         * ========================================
         *
         * 实体保存这些数据。
         */

        ghost.setDomainStrength(
                newStrength
        );

        ghost.setDomainRadius(
                newRadius
        );

        /*
         * ========================================
         * ③ 厉鬼灵异强度同步
         * ========================================
         *
         * 厉鬼自身强度 = 鬼域强度
         */

        GhostAttributeSystem.setSupernaturalStrength(
                ghost,
                newStrength
        );

        /*
         * ========================================
         * ④ 通知客户端 Domain 状态发生变化
         * ========================================
         *
         * Domain 的强度和半径都发生了变化。
         */

        GhostDomainManager.get(level)
                .syncUpdate(
                        domain
                );

        QisPlan2.LOGGER.info(
                "[鬼雾] 鬼雾 {} 成长：强度={}，半径={}",
                ghost.getUUID(),
                newStrength,
                newRadius
        );
    }
}