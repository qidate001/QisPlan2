package com.qidate.qisplan2.ghost.domain.type.mist;

import com.qidate.qisplan2.QisPlan2;
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
 * 2. 删除鬼雾鬼域
 * 3. 维护鬼域位置
 * 4. 确保鬼雾实体与鬼域一一对应
 *
 * 当前阶段不处理任何鬼域 Effect。
 */
public final class GhostMistDomainController {

    /**
     * 鬼雾鬼域类型。
     */
    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_mist"
            );

    /**
     * 鬼雾鬼域半径。
     *
     * 当前先给一个测试值。
     * 后续你可以直接调整。
     */
    private static final double DOMAIN_RADIUS = 80.0D;

    private GhostMistDomainController() {
    }

    /**
     * ========================================
     * 鬼雾实体每 tick 调用
     * ========================================
     *
     * 确保鬼雾实体始终拥有自己的鬼域。
     */
    public static void tick(
            GhostMist ghost
    ) {

        /*
         * ========================================
         * 只在服务端处理
         * ========================================
         */
        if (!(ghost.level()
                instanceof ServerLevel level)) {

            return;
        }

        /*
         * ========================================
         * 鬼已经死亡
         * ========================================
         *
         * 死亡实体不应该继续维持鬼域。
         */
        if (!ghost.isAlive()) {

            removeDomain(
                    level,
                    ghost.getUUID()
            );

            return;
        }

        /*
         * ========================================
         * 获取鬼雾鬼域管理器
         * ========================================
         */
        GhostDomainManager manager =
                GhostDomainManager.get(level);

        /*
         * ========================================
         * 查找现有鬼域
         * ========================================
         */
        GhostDomain domain =
                manager.getBySourceAndType(
                        ghost.getUUID(),
                        DOMAIN_TYPE
                );

        /*
         * ========================================
         * 鬼域不存在
         * ========================================
         */
        if (domain == null) {

            createDomain(
                    level,
                    ghost
            );
        }
    }

    /**
     * ========================================
     * 创建鬼雾鬼域
     * ========================================
     */
    private static void createDomain(
            ServerLevel level,
            GhostMist ghost
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        /*
         * 防止重复创建。
         */
        if (manager.getBySourceAndType(
                ghost.getUUID(),
                DOMAIN_TYPE
        ) != null) {

            return;
        }

        /*
         * ========================================
         * 创建鬼域
         * ========================================
         */
        GhostDomain domain =
                new GhostDomain(
                        UUID.randomUUID(),
                        ghost.getUUID(),
                        DOMAIN_TYPE,

                        /*
                         * 当前先使用实体自身的
                         * 灵异强度。
                         */
                        ghost.getSupernaturalStrength(),

                        /*
                         * 第 1 层。
                         */
                        1,

                        DOMAIN_RADIUS,

                        level.dimension(),

                        ghost.getX(),
                        ghost.getY(),
                        ghost.getZ(),

                        /*
                         * 球形鬼域。
                         */
                        new SphereDomainShape(
                                DOMAIN_RADIUS
                        ),

                        /*
                         * 根据距离更新。
                         */
                        GhostDomainUpdateMode.DISTANCE,

                        3.0D,

                        /*
                         * 当前没有任何 Effect。
                         */
                        new GhostMistDomainBehavior()
                );

        manager.add(domain);

        QisPlan2.LOGGER.info(
                "[鬼雾] 鬼雾源头 {} 创建鬼域，半径={}",
                ghost.getUUID(),
                DOMAIN_RADIUS
        );
    }

    /**
     * ========================================
     * 删除鬼雾鬼域
     * ========================================
     */
    public static void removeDomain(
            ServerLevel level,
            UUID sourceUUID
    ) {

        GhostDomainManager.get(level)
                .removeBySourceAndType(
                        sourceUUID,
                        DOMAIN_TYPE
                );
    }
}