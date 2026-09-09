package com.qidate.qisplan2.ghost.domain.umbrella;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.ghost.domain.GhostDomain;
import com.qidate.qisplan2.ghost.domain.GhostDomainBehavior;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
     */
    private void attackEntitiesInDomain(
            ServerLevel level,
            GhostDomain domain,
            ServerPlayer source,
            double strength
    ) {

        /*
         * ========================================
         * AABB 粗筛
         * ========================================
         *
         * 这里暂时使用一个足够大的方形范围。
         * 最终是否在鬼域内，由 domain.contains()
         * 决定。
         */
        double radius = 50.0D;

        if (domain.getShape()
                instanceof com.qidate.qisplan2.ghost.domain.CylinderDomainShape cylinder) {

            radius = cylinder.getRadius();
        }

        AABB box =
                new AABB(
                        domain.getX() - radius,
                        level.getMinBuildHeight(),
                        domain.getZ() - radius,
                        domain.getX() + radius,
                        level.getMaxBuildHeight(),
                        domain.getZ() + radius
                );

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        LivingEntity::isAlive
                )) {

            /*
             * 持伞者自己不会受到“领域攻击”。
             * 他会单独受到反噬。
             */
            if (target == source) {
                continue;
            }

            /*
             * ========================================
             * 真正的领域判定
             * ========================================
             */
            if (!domain.contains(
                    target.getX(),
                    target.getY(),
                    target.getZ()
            )) {
                continue;
            }

            /*
             * 灵异攻击。
             */
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