package com.qidate.qisplan2.ghost.domain.type.mist;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ghostmist.GhostMist;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/**
 * ========================================
 * 鬼雾鬼域行为
 * ========================================
 */
public final class GhostMistDomainBehavior
        implements GhostDomainBehavior {

    /**
     * 鬼雾攻击间隔：
     *
     * 20 tick = 1 秒
     */
    private static final int ATTACK_INTERVAL = 20;

    /**
     * 鬼雾灵异袭击强度倍率。
     *
     * 攻击强度 = 鬼域强度 / 5
     */
    private static final double ATTACK_STRENGTH_DIVISOR = 5.0D;

    /**
     * 鬼雾鬼域行为的执行间隔。
     *
     * <p>
     * GhostDomainManager 会按照这个间隔
     * 调用一次 onTick()。
     * </p>
     *
     * @return 20 tick，即每秒执行一次
     */
    @Override
    public int getTickInterval() {
        return ATTACK_INTERVAL;
    }

    /**
     * 每次鬼雾鬼域行为执行时，
     * 对当前最终生效于鬼雾鬼域的实体发动袭击。
     */
    @Override
    public void onTick(
            ServerLevel level,
            GhostDomain domain,
            Set<Entity> entities
    ) {

        /*
         * ========================================
         * 获取鬼雾源头
         * ========================================
         *
         * GhostDomainManager 已经负责保证
         * 源头实体被移除后鬼域会被清理。
         *
         * 这里仍然进行一次安全检查。
         */
        Entity source =
                level.getEntity(
                        domain.getSourceUUID()
                );

        if (source == null
                || source.isRemoved()) {
            return;
        }

        /*
         * ========================================
         * 计算本次灵异袭击强度
         * ========================================
         *
         * 鬼域强度 / 5
         */
        double attackStrength =
                domain.getStrength()
                        / ATTACK_STRENGTH_DIVISOR;

        /*
         * ========================================
         * 攻击当前有效实体
         * ========================================
         */
        attackEntities(
                level,
                domain,
                source,
                attackStrength,
                entities
        );
    }

    /**
     * 攻击已经由 Tracker 确认属于
     * 当前鬼域最终生效范围的实体。
     *
     * <p>
     * 本方法不负责实体发现，
     * 只负责执行鬼雾的灵异规则。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 当前鬼雾鬼域
     * @param source 鬼雾源头
     * @param strength 本次袭击强度
     * @param entities 当前最终生效于该鬼域的实体
     */
    private void attackEntities(
            ServerLevel level,
            GhostDomain domain,
            Entity source,
            double strength,
            Set<Entity> entities
    ) {

        for (Entity entity : entities) {

            /*
             * 不攻击鬼雾源头本身。
             */
            if (entity == source) {
                continue;
            }

            /*
             * 只攻击生物。
             */
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            /*
             * 已经死亡的目标跳过。
             */
            if (!target.isAlive()) {
                continue;
            }

            /*
             * ========================================
             * 发动灵异袭击
             * ========================================
             */
            boolean killed =
                    SupernaturalDeathHandler.tryKill(
                            target,
                            ModDamageTypes.ghostMist(
                                    source
                            ),
                            strength
                    );

            /*
             * ========================================
             * 成功击杀后的鬼域成长
             * ========================================
             */
            if (!killed) {
                continue;
            }

            /*
             * ========================================
             * 世界中的 GhostMist
             * ========================================
             *
             * 世界鬼雾仍然使用原来的成长逻辑。
             */
            if (source instanceof GhostMist ghost) {

                GhostMistDomainController.grow(
                        level,
                        ghost,
                        domain
                );

                continue;
            }

            /*
             * ========================================
             * 玩家驾驭的鬼雾
             * ========================================
             *
             * 玩家鬼雾的成长必须写回
             * PossessedGhostDomainData。
             */
            if (source instanceof ServerPlayer player) {

                GhostMistDomainController.grow(
                        player,
                        domain
                );
            }
        }
    }

    /**
     * 当鬼雾鬼域被创建时调用。
     *
     * <p>
     * 当前鬼雾没有额外的创建逻辑。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 新创建的鬼雾鬼域
     */
    @Override
    public void onCreate(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    /**
     * 当鬼雾鬼域被移除时调用。
     *
     * <p>
     * 当前鬼雾没有额外的移除逻辑。
     * </p>
     *
     * @param level 当前服务器维度
     * @param domain 被移除的鬼雾鬼域
     */
    @Override
    public void onRemove(
            ServerLevel level,
            GhostDomain domain
    ) {
    }
}