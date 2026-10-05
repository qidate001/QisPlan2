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
 * ========================================================
 * 鬼雾鬼域控制器
 * ========================================================
 *
 * 负责：
 *
 * 1. 创建鬼雾鬼域
 * 2. 定义鬼雾鬼域的基础参数
 */
public final class GhostMistDomainController {

    /**
     * ========================================================
     * 鬼雾鬼域类型
     * ========================================================
     */
    public static final ResourceLocation DOMAIN_TYPE =
            ResourceLocation.fromNamespaceAndPath(
                    QisPlan2.MODID,
                    "ghost_mist"
            );

    /**
     * ========================================================
     * 鬼雾鬼域半径
     * ========================================================
     */
    private static final double DOMAIN_RADIUS = 80.0D;

    private GhostMistDomainController() {
    }

    /**
     * ========================================================
     * 创建鬼雾鬼域
     * ========================================================
     *
     * 为指定的鬼雾实体创建一个对应的球形鬼域。
     *
     * GhostDomainManager 会负责后续：
     *
     * - 鬼域位置更新
     * - 鬼域生命周期
     * - 源实体消失后的自动清理
     * - 鬼域网络同步
     */
    public static void createDomain(
            ServerLevel level,
            GhostMist ghost
    ) {

        GhostDomainManager manager =
                GhostDomainManager.get(level);

        /*
         * ========================================================
         * 防止重复创建
         * ========================================================
         */
        if (manager.getBySourceAndType(
                ghost.getUUID(),
                DOMAIN_TYPE
        ) != null) {

            return;
        }

        /*
         * ========================================================
         * 创建鬼域
         * ========================================================
         */
        GhostDomain domain =
                new GhostDomain(
                        UUID.randomUUID(),

                        /*
                         * 鬼域源头。
                         */
                        ghost.getUUID(),

                        DOMAIN_TYPE,

                        /*
                         * 鬼域强度直接使用鬼雾自身的
                         * 灵异强度。
                         */
                        ghost.getSupernaturalStrength(),

                        /*
                         * 当前使用第 1 层。
                         */
                        1,

                        DOMAIN_RADIUS,

                        /*
                         * 鬼域所在维度。
                         */
                        level.dimension(),

                        /*
                         * 初始位置。
                         *
                         * 后续由 GhostDomainManager
                         * 根据 DISTANCE 模式自动更新。
                         */
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
                         * 根据源头实体位置更新。
                         */
                        GhostDomainUpdateMode.DISTANCE,

                        /*
                         * 源头移动超过 3 格时更新。
                         */
                        3.0D,

                        /*
                         * 鬼雾鬼域规则。
                         *
                         * 当前暂时为空。
                         */
                        new GhostMistDomainBehavior()
                );

        /*
         * ========================================================
         * 注册到统一鬼域管理器
         * ========================================================
         */
        manager.add(domain);

        QisPlan2.LOGGER.info(
                "[鬼雾] 鬼雾源头 {} 创建鬼域，半径={}",
                ghost.getUUID(),
                DOMAIN_RADIUS
        );
    }
}