package com.qidate.qisplan2.ghost.domain;

import com.qidate.qisplan2.entity.AbstractGhostEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 管理鬼域主人的灵异视觉。
 *
 * <p>
 * 负责判断鬼域主人当前能够透视哪些实体，
 * 以及这些实体在鬼域主人眼中应该使用什么颜色。
 *
 * <p>
 * 实体是否真正处于鬼域中，
 * 统一由 {@link GhostDomainEntityTracker} 负责。
 *
 * <p>
 * 本类不负责实体追踪、网络同步以及客户端渲染。
 */
public final class GhostDomainVisionSystem {

    /**
     * 玩家描边颜色。
     */
    public static final int PLAYER_COLOR =
            0x00FF66;

    /**
     * 普通实体描边颜色。
     */
    public static final int ENTITY_COLOR =
            0xFFFFFF;

    /**
     * 厉鬼描边颜色。
     */
    public static final int GHOST_COLOR =
            0xFF3333;

    private GhostDomainVisionSystem() {
    }

    /**
     * 获取指定鬼域当前应该显示的视觉实体。
     *
     * <p>
     * 只有鬼域来源实体是玩家时，
     * 才存在对应的客户端观察者。
     *
     * <p>
     * 实体是否处于鬼域中，
     * 直接使用 {@link GhostDomainEntityTracker}
     * 已经维护好的结果。
     *
     * @param level 鬼域所在世界
     * @param domain 鬼域
     * @return 当前鬼域主人应该看到的实体
     */
    public static Map<UUID, Integer> collectVisibleEntities(
            ServerLevel level,
            GhostDomain domain
    ) {

        Map<UUID, Integer> visibleEntities =
                new LinkedHashMap<>();

        /*
         * ========================================================
         * 获取鬼域主人
         * ========================================================
         *
         * 当前只有玩家拥有客户端，
         * 因此只有 ServerPlayer 才能成为视觉观察者。
         */
        if (!(level.getEntity(
                domain.getSourceUUID()
        ) instanceof ServerPlayer)) {

            return visibleEntities;
        }

        /*
         * ========================================================
         * 获取鬼域中的实体
         * ========================================================
         *
         * 这里不重新搜索实体。
         *
         * GhostDomainEntityTracker 已经负责：
         *
         * 1. 鬼域范围判断
         * 2. 鬼域形状判断
         * 3. 肉身鬼域抵抗
         * 4. 重叠鬼域关系
         * 5. 实体进入与离开
         *
         * 因此视觉系统只消费它提供的结果。
         */
        GhostDomainEntityTracker tracker =
                GhostDomainEntityTracker.get(level);

        for (Entity entity :
                tracker.getEntities(domain)) {

            /*
             * 当前视觉只处理 LivingEntity。
             *
             * 玩家、普通生物以及厉鬼
             * 都属于 LivingEntity。
             */
            if (!(entity instanceof LivingEntity)) {
                continue;
            }

            int color =
                    getVisionColor(entity);

            visibleEntities.put(
                    entity.getUUID(),
                    color
            );
        }

        return visibleEntities;
    }

    /**
     * 获取实体在鬼域主人眼中的描边颜色。
     *
     * @param entity 实体
     * @return RGB 颜色
     */
    private static int getVisionColor(
            Entity entity
    ) {

        if (entity instanceof AbstractGhostEntity) {
            return GHOST_COLOR;
        }

        if (entity instanceof Player) {
            return PLAYER_COLOR;
        }

        return ENTITY_COLOR;
    }
}