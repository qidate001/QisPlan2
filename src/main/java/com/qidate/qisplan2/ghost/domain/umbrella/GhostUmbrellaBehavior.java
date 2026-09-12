package com.qidate.qisplan2.ghost.domain.umbrella;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import com.qidate.qisplan2.ghost.domain.GhostDomainEntityTracker;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class GhostUmbrellaBehavior
        implements GhostDomainBehavior {

    /**
     * 每秒攻击一次。
     */
    private static final int ATTACK_INTERVAL = 20;

    /**
     * 初始灵异攻击强度。
     */
    private static final double INITIAL_ATTACK_STRENGTH = 10.0D;

    /**
     * 反噬分母。
     */
    private static final double SELF_ATTACK_DIVISOR = 4.0D;

    @Override
    public void tick(
            ServerLevel level,
            GhostDomain domain
    ) {

        ServerPlayer source =
                level.getServer()
                        .getPlayerList()
                        .getPlayer(domain.getSourceUUID());

        if (source == null) {
            return;
        }

        /*
         * 玩家已经不在这个维度了。
         *
         * 正常情况下跨维度事件会提前清理 GhostDomain，
         * 这里再做一道保险。
         */
        if (source.serverLevel() != level) {
            return;
        }

        /*
         * 鬼伞已经关闭。
         *
         * 正常情况下 GhostUmbrellaDomain.tick()
         * 会负责删除领域。
         */
        if (!hasOpenUmbrella(source)) {
            return;
        }

        /*
         * ========================================
         * 每 tick：持伞者副作用
         * ========================================
         */
        updateUmbrellaHolderState(source);

        /*
         * ========================================
         * 每秒一次：灵异攻击
         * ========================================
         */
        if (level.getGameTime() % ATTACK_INTERVAL != 0) {
            return;
        }

        double strength =
                getAttackStrength(source);

        /*
         * 攻击领域内其他生物。
         */
        attackEntitiesInDomain(
                level,
                domain,
                source,
                strength
        );

        /*
         * 反噬持伞者。
         */
        attackUmbrellaHolder(
                level,
                domain,
                source,
                strength / SELF_ATTACK_DIVISOR
        );
    }

    /**
     * 持伞者的鬼伞副作用。
     */
    private void updateUmbrellaHolderState(
            ServerPlayer player
    ) {

        /*
         * 失明。
         */
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.BLINDNESS,
                        40,
                        0,
                        false,
                        false,
                        true
                )
        );

        /*
         * 极强缓慢。
         */
        player.addEffect(
                new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN,
                        40,
                        255,
                        false,
                        false,
                        false
                )
        );

        /*
         * 彻底禁止移动。
         */
        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        player.hasImpulse = true;
    }

    /**
     * 反噬持伞者。
     */
    private void attackUmbrellaHolder(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer player,
            double strength
    ) {

        if (!player.isAlive()) {
            return;
        }

        GhostDomainEntityTracker tracker =
                GhostDomainEntityTracker.get(level);

        GhostDomain effectiveDomain =
                tracker.getEffectiveDomain(player);

        /*
         * 只有鬼雨伞鬼域是伞主人的最终生效鬼域时，
         * 鬼雨伞才会攻击伞主人。
         */
        if (effectiveDomain == null) {
            return;
        }

        if (!effectiveDomain.getId().equals(
                domain.getId()
        )) {
            return;
        }

        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostUmbrella(player),
                strength
        );
    }

    /**
     * 攻击鬼域范围内的其他生物。
     *
     * <p>实体范围由 GhostDomainEntityTracker 统一维护，
     * 这里不再自行进行 AABB 搜索和鬼域范围判定。</p>
     */
    private void attackEntitiesInDomain(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer source,
            double strength
    ) {

        GhostDomainEntityTracker tracker =
                GhostDomainEntityTracker.get(level);

        for (Entity entity :
                tracker.getEntities(domain)) {

            if (entity == source) {
                continue;
            }

            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            if (!target.isAlive()) {
                continue;
            }

            /*
             * 只有当前鬼雨伞鬼域是该实体的最终生效鬼域时，
             * 鬼雨伞才能对其发动灵异袭击。
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

            SupernaturalDeathHandler.tryKill(
                    target,
                    ModDamageTypes.ghostUmbrella(source),
                    strength
            );
        }
    }

    private static boolean hasOpenUmbrella(
            ServerPlayer player
    ) {

        return isOpenUmbrella(
                player.getMainHandItem()
        ) || isOpenUmbrella(
                player.getOffhandItem()
        );
    }

    private static boolean isOpenUmbrella(
            net.minecraft.world.item.ItemStack stack
    ) {

        return stack.getItem()
                instanceof GhostUmbrellaItem
                && GhostUmbrellaItem.isOpen(stack);
    }

    /**
     * 当前鬼雨攻击强度。
     *
     * 后续可以根据鬼伞状态、驭鬼者状态等动态计算。
     */
    private static double getAttackStrength(
            ServerPlayer source
    ) {
        return INITIAL_ATTACK_STRENGTH;
    }
}