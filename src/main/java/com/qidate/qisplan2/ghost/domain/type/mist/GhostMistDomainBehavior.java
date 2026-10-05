package com.qidate.qisplan2.ghost.domain.type.mist;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.entity.ghostmist.GhostMist;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import com.qidate.qisplan2.ghost.domain.GhostDomainEntityTracker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * ========================================
 * 鬼雾鬼域行为
 * ========================================
 *
 * 鬼雾的主要规则：
 *
 * 1. 每秒对鬼域内所有符合条件的生物
 *    发动一次灵异袭击。
 *
 * 2. 灵异袭击强度 =
 *
 *        鬼域强度 / 5
 *
 * 3. 如果成功杀死目标：
 *
 *        鬼域强度 +0.1
 *        鬼域半径 +0.05
 *
 * 4. 实体范围由
 *    GhostDomainEntityTracker
 *    统一维护。
 *
 * 5. 只有当鬼雾鬼域是目标当前的
 *    最终生效鬼域时，鬼雾才能发动袭击。
 */
public final class GhostMistDomainBehavior
        implements GhostDomainBehavior {

    /**
     * 鬼雾攻击间隔：
     *
     * 20 tick = 1 秒
     */
    private static final long ATTACK_INTERVAL = 20L;

    /**
     * 鬼雾灵异袭击强度倍率。
     *
     * 攻击强度 = 鬼域强度 / 5
     */
    private static final double ATTACK_STRENGTH_DIVISOR = 5.0D;

    @Override
    public void onCreate(
            ServerLevel level,
            GhostDomain domain
    ) {
    }

    @Override
    public void tick(
            ServerLevel level,
            GhostDomain domain
    ) {
        /*
         * ========================================
         * 每秒发动一次灵异袭击
         * ========================================
         */

        if (
                level.getGameTime()
                        % ATTACK_INTERVAL
                        != 0L
        ) {
            return;
        }

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

        if (
                source == null
                        || source.isRemoved()
        ) {
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
         * 攻击鬼域范围内的生物
         * ========================================
         */

        attackEntitiesInDomain(
                level,
                domain,
                source,
                attackStrength
        );
    }

    /**
     * 攻击鬼域范围内的其他生物。
     *
     * <p>
     * 实体范围由 GhostDomainEntityTracker
     * 统一维护，这里不再自行进行 AABB 搜索
     * 和鬼域范围判定。
     * </p>
     *
     * <p>
     * 只有当前鬼雾鬼域是该实体的最终生效鬼域时，
     * 鬼雾才能对其发动灵异袭击。
     * </p>
     */
    private void attackEntitiesInDomain(
            ServerLevel level,
            GhostDomain domain,
            Entity source,
            double strength
    ) {
        GhostDomainEntityTracker tracker =
                GhostDomainEntityTracker.get(level);

        for (Entity entity :
                tracker.getEntities(domain)) {

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
             * 判断最终生效鬼域
             * ========================================
             *
             * 一个实体可能同时处于多个鬼域。
             *
             * 只有鬼雾鬼域是该实体最终生效的鬼域时，
             * 才允许鬼雾发动灵异袭击。
             */
            GhostDomain effectiveDomain =
                    tracker.getEffectiveDomain(entity);

            if (effectiveDomain == null) {
                continue;
            }

            if (!effectiveDomain.getId().equals(
                    domain.getId()
            )) {
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

            if (killed) {

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
    }

    @Override
    public void onRemove(
            ServerLevel level,
            GhostDomain domain
    ) {
    }
}