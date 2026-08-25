package com.qidate.qisplan2.event;

import com.qidate.qisplan2.death.ModDamageTypes;
import com.qidate.qisplan2.death.SupernaturalDeathHandler;
import com.qidate.qisplan2.item.GhostUmbrellaItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class GhostUmbrellaAttackHandler {

    /**
     * 初始领域半径。
     *
     * 后续可以直接修改每个 GhostRainSource 的 radius。
     */
    private static final double INITIAL_RADIUS = 50.0D;

    /**
     * 初始灵异攻击强度。
     */
    private static final double INITIAL_ATTACK_STRENGTH = 10.0D;

    /**
     * 每秒一次。
     */
    private static final int ATTACK_INTERVAL = 20;

    /**
     * 反噬分母
     */
    private static final double SELF_ATTACK_DIVISOR = 4.0D;

    private GhostUmbrellaAttackHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(
            ServerTickEvent.Post event
    ) {
        var server =
                event.getServer();

        /*
         * ========================================
         * 每 tick 处理持伞副作用
         * ========================================
         */
        for (ServerLevel level :
                server.getAllLevels()) {

            for (Player player :
                    level.players()) {

                if (hasOpenUmbrella(player)) {

                    updateUmbrellaHolderState(
                            player
                    );
                }
            }
        }

        /*
         * ========================================
         * 每秒一次灵异攻击
         * ========================================
         */
        if (server.getTickCount() % 20 != 0) {
            return;
        }

        for (ServerLevel level :
                server.getAllLevels()) {

            attackFromOpenUmbrellas(level);
        }
    }

    private static void attackFromOpenUmbrellas(
            ServerLevel level
    ) {

        /*
         * 目前鬼雨源就是玩家手里的打开的鬼雨伞。
         */
        for (Player source :
                level.players()) {

            if (!hasOpenUmbrella(source)) {
                continue;
            }

            double radius =
                    getDomainRadius(source);

            double strength =
                    getAttackStrength(source);

            /*
             * ========================================
             * 鬼雨领域攻击其他生物
             * ========================================
             */
            attackEntitiesInDomain(
                    level,
                    source,
                    radius,
                    strength
            );

            /*
             * ========================================
             * 鬼雨反噬持伞者自己
             * ========================================
             */
            attackUmbrellaHolder(
                    source,
                    strength / SELF_ATTACK_DIVISOR
            );
        }
    }

    private static void updateUmbrellaHolderState(
            Player player
    ) {

        /*
         * 失明。
         *
         * 40 tick 会不断刷新，
         * 所以伞一旦关闭立即停止。
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
         * 极强缓慢作为客户端/移动系统层面的保险。
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
         * ========================================
         * 彻底禁止移动
         * ========================================
         */
        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        player.hasImpulse = true;
    }

    private static void attackUmbrellaHolder(
            Player player,
            double strength
    ) {

        if (!player.isAlive()) {
            return;
        }

        SupernaturalDeathHandler.tryKill(
                player,
                ModDamageTypes.ghostUmbrella(
                        player
                ),
                strength
        );
    }

    private static boolean hasOpenUmbrella(
            Player player
    ) {
        return isOpenUmbrella(
                player.getMainHandItem()
        ) || isOpenUmbrella(
                player.getOffhandItem()
        );
    }

    private static boolean isOpenUmbrella(
            ItemStack stack
    ) {
        return stack.getItem()
                instanceof GhostUmbrellaItem
                && GhostUmbrellaItem.isOpen(stack);
    }

    /**
     * 当前领域半径。
     *
     * 现在是固定 50。
     * 后续扩大领域时只修改这里。
     */
    private static double getDomainRadius(
            Player source
    ) {
        return INITIAL_RADIUS;
    }

    /**
     * 当前鬼雨攻击强度。
     *
     * 现在是 10。
     * 后续增强鬼雨强度时只修改这里。
     */
    private static double getAttackStrength(
            Player source
    ) {
        return INITIAL_ATTACK_STRENGTH;
    }

    private static void attackEntitiesInDomain(
            ServerLevel level,
            Player source,
            double radius,
            double strength
    ) {

        /*
         * 用 AABB 先做粗筛。
         *
         * EntityGetter 提供 getEntitiesOfClass(Class, AABB)
         * 可以直接取得指定范围内的 LivingEntity。
         */
        AABB box =
                source.getBoundingBox()
                        .inflate(radius);

        for (LivingEntity target :
                level.getEntitiesOfClass(
                        LivingEntity.class,
                        box,
                        LivingEntity::isAlive
                )) {

            /*
             * 撑开鬼雨伞的人（自己）不会受到鬼雨攻击。
             */
            if (target == source) {
                continue;
            }

            /*
             * 精确圆形距离。
             *
             * AABB 只是粗筛，
             * 实际领域仍然是圆形。
             */
            double dx =
                    target.getX()
                            - source.getX();

            double dz =
                    target.getZ()
                            - source.getZ();

            if (dx * dx + dz * dz
                    > radius * radius) {
                continue;
            }

            /*
             * ========================================
             * 灵异攻击
             * ========================================
             */
            SupernaturalDeathHandler.tryKill(
                    target,
                    ModDamageTypes.ghostUmbrella(
                            source
                    ),
                    strength
            );
        }
    }
}