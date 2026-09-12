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
import net.minecraft.world.phys.AABB;

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
            ServerPlayer player,
            double strength
    ) {

        if (!player.isAlive()) {
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

        double radius = 50.0D;

        AABB box = new AABB(
                domain.getX() - radius,
                domain.getY() - radius,
                domain.getZ() - radius,
                domain.getX() + radius,
                domain.getY() + radius,
                domain.getZ() + radius
        );

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        entity -> entity != source
                                && entity.isAlive()
                )) {

            if (!domain.contains(
                    target.getX(),
                    target.getY(),
                    target.getZ()
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